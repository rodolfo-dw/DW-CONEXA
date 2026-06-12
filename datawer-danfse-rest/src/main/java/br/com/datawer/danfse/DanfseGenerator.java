package br.com.datawer.danfse;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.blend.BlendMode;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * Gerador do DANFSe v2.0 conforme NT 008/2026 (SE/CGNFS-e).
 *
 * Desenho posicional em PDF (A4 retrato), medidas em centímetros conforme a
 * tabela do item 2.4.5. Blocos suprimíveis (Destinatário, Intermediário etc.)
 * seguem as notas 2/3/4 - o espaço liberado desloca os blocos seguintes para
 * cima e amplia "Informações Complementares".
 *
 * Fontes: a NT pede Arial (títulos/labels) e Microsoft Sans Serif (conteúdo).
 * Os TTF são embutidos como subset a partir de /danfse/fonts; sem os resources,
 * cai em Helvetica (válido só para desenvolvimento).
 *
 * Traços: somente o que o item 2.2.3 exige - borda da página 1pt e linhas
 * divisórias dos blocos 0,5pt. Sem grade interna entre campos (o desenho do
 * Anexo I mostra tracejados internos, mas o texto não os exige e a DANFSe
 * oficial do portal não imprime grade interna).
 *
 * A borda da página é a própria moldura do corpo (no Anexo I há uma única
 * linha vertical de cada lado, percorrendo a página inteira) - não existe
 * segunda moldura recuada. O canhoto (opcional, Nota 11) é impresso como
 * caixa destacada entre o fim do corpo e a borda inferior.
 */
public class DanfseGenerator {

	private static final float CM = 28.34646f;          // pontos por centímetro
	private static final float PAGE_W = 21.0f;          // A4 em cm
	private static final float PAGE_H = 29.7f;
	// No Anexo I a borda da página (1pt) é a própria moldura lateral dos blocos:
	// não há segunda linha recuada. O corpo encosta na borda; o conteúdo das
	// células fica 0,10 para dentro, caindo nas posições da tabela 2.4.5
	// (0,30 / 5,41 / 10,51 / 15,62).
	private static final float LEFT = 0.20f;            // borda esquerda do corpo = borda da página
	private static final float RIGHT = 20.80f;          // borda direita do corpo = borda da página
	private static final float BODY_W = RIGHT - LEFT;   // 20,60
	private static final float[] COLS = {0.20f, 5.31f, 10.41f, 15.52f};
	private static final float BOTTOM = 27.90f;         // fim do corpo (Inf. Complementares)
	private static final float CANHOTO_TOP = 28.10f;    // tabela 2.4.5: canhoto em 28,10
	private static final float CANHOTO_H = 0.67f;

	private PDFont fontContent;   // Microsoft Sans Serif - conteúdo dos campos
	private PDFont fontBold;      // Arial Bold - títulos e labels
	private PDFont fontArial;     // Arial normal - marca d'água

	public static final String SIT_CANCELADA = "CANCELADA";
	public static final String SIT_SUBSTITUIDA = "SUBSTITUIDA";

	private PDDocument doc;
	private PDPageContentStream cs;
	private NfseXml n;
	private List<DanfseAuditEntry> audit;
	private String auditBlock;

	private static final class FieldValue {
		private final String value;
		private final String source;
		private final String rule;
		private final boolean tableConverted;
		private final boolean usedDash;

		private FieldValue(String value, String source, String rule, boolean tableConverted) {
			this.value = value;
			this.source = source;
			this.rule = rule;
			this.tableConverted = tableConverted;
			this.usedDash = containsMissingMarker(value);
		}
	}

	/**
	 * @param nfse     XML da NFS-e já parseado
	 * @param situacao null para nota normal, ou {@link #SIT_CANCELADA} /
	 *                 {@link #SIT_SUBSTITUIDA} para imprimir a marca d'água
	 *                 (o cancelamento não consta no XML da NFS-e; vem por evento)
	 */
	public byte[] generate(NfseXml nfse, String situacao) throws Exception {
		return generateWithAudit(nfse, situacao).getPdf();
	}

	/** Gera o PDF e devolve a trilha de origem/formatação dos campos impressos. */
	public DanfseGenerationResult generateWithAudit(NfseXml nfse, String situacao) throws Exception {
		this.n = nfse;
		this.audit = new ArrayList<>();
		IbgeMunicipios.atualizar();
		try (PDDocument document = new PDDocument()) {
			this.doc = document;
			this.fontContent = loadFont("/danfse/fonts/micross.ttf", PDType1Font.HELVETICA);
			this.fontBold = loadFont("/danfse/fonts/arialbd.ttf", PDType1Font.HELVETICA_BOLD);
			this.fontArial = loadFont("/danfse/fonts/arial.ttf", PDType1Font.HELVETICA);
			PDPage page = new PDPage(PDRectangle.A4);
			doc.addPage(page);
			try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
				this.cs = stream;
				draw(situacao);
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			doc.save(out);
			return new DanfseGenerationResult(out.toByteArray(), audit);
		}
	}

	private void draw(String situacao) throws Exception {
		cs.setLineWidth(0.5f);

		drawCabecalho();
		drawDadosNfse();

		float y = 4.34f;
		y = drawPrestador(y);
		y = drawTomador(y);
		y = drawDestinatario(y);
		y = drawIntermediario(y);
		y = drawServico(y);
		y = drawIssqn(y);
		y = drawTribFederal(y);
		y = drawIbsCbs(y);
		y = drawValorTotal(y);
		drawInfoComplementares(y);
		drawCanhoto();

		// borda da página: 1pt (item 2.2.3) - coincide com a moldura do corpo;
		// traçada por último para não ser coberta pelos sombreamentos que
		// encostam nela
		cs.setLineWidth(1f);
		rect(LEFT, 0.20f, BODY_W, PAGE_H - 0.40f);
		cs.stroke();
		cs.setLineWidth(0.5f);

		if (SIT_CANCELADA.equalsIgnoreCase(situacao)) {
			watermark("CANCELADA");
		} else if (SIT_SUBSTITUIDA.equalsIgnoreCase(situacao)) {
			watermark("SUBSTITUÍDA");
		}
	}

	// ------------------------------------------------------------------ blocos

	private void drawCabecalho() throws Exception {
		auditBlock = "CABEÇALHO";
		// topo do cabeçalho = borda da página (Anexo I); a divisória inferior
		// (1,46) é traçada pelo bloco seguinte - cada fronteira tem UMA linha
		float y = 0.20f, h = 1.46f - y;
		shade(LEFT, y, BODY_W, h);

		// logomarca NFS-e (canto esquerdo)
		PDImageXObject logo = loadLogo();
		if (logo != null) {
			float lw = 4.00f;
			float lh = lw * logo.getHeight() / logo.getWidth();
			image(logo, 0.49f, 0.44f + (0.85f - lh) / 2, lw, lh);
		} else {
			text("NFS-e", 0.49f, 0.95f, fontBold, 14);
		}

		// centro
		boolean homolog = "2".equals(n.txt("infNFSe", "DPS", "infDPS", "tpAmb"));
		textCenter("DANFSe v2.0", 5.41f, 10.19f, homolog ? 0.62f : 0.72f, fontBold, 9);
		textCenter("Documento Auxiliar da NFS-e", 5.41f, 10.19f, homolog ? 0.98f : 1.10f, fontBold, 9);
		if (homolog) {
			cs.setNonStrokingColor(255, 0, 0);
			textCenter("NFS-e SEM VALIDADE JURÍDICA", 5.41f, 10.19f, 1.34f, fontBold, 9);
			cs.setNonStrokingColor(0, 0, 0);
		}

		// canto direito
		String cTribNac = n.txt("infNFSe", "DPS", "infDPS", "serv", "cServ", "cTribNac");
		if (!cTribNac.startsWith("99")) {
			String mun = n.txt("infNFSe", "xLocEmi");
			String uf = n.txt("infNFSe", "emit", "enderNac", "UF");
			FieldValue municipio = composed(munUf(mun, uf),
					"NFSe/infNFSe/xLocEmi + NFSe/infNFSe/emit/enderNac/UF",
					"Concatenação Município / UF", false);
			String municipioImpresso = trunc("Município: " + display(municipio), 5.0f, fontContent, 8);
			text(municipioImpresso, 15.70f, 0.72f, fontContent, 8);
			auditValue("Município", display(municipio), municipio, false);
		}

		FieldValue ambGer = tableValue(n.txt("infNFSe", "ambGer"), DESC_AMB_GER,
				"NFSe/infNFSe/ambGer", "Descrição do ambiente gerador");
		text("Ambiente Gerador: " + display(ambGer), 15.70f, 1.07f, fontContent, 6);
		auditValue("Ambiente Gerador", display(ambGer), ambGer, false);

		FieldValue tpAmb = tableValue(n.txt("infNFSe", "DPS", "infDPS", "tpAmb"), DESC_TP_AMB,
				"NFSe/infNFSe/DPS/infDPS/tpAmb", "Descrição do tipo de ambiente");
		text("Tipo de Ambiente: " + display(tpAmb), 15.70f, 1.32f, fontContent, 6);
		auditValue("Tipo de Ambiente", display(tpAmb), tpAmb, false);
	}

	private void drawDadosNfse() throws Exception {
		auditBlock = "DADOS DA NFS-e";
		// divisória superior (única) com o cabeçalho; a inferior (4,34) é do
		// prestador. Os campos seguem as posições da tabela 2.4.5
		line(LEFT, 1.46f, RIGHT, 1.46f);
		float y = 1.48f;

		// chave de acesso
		fieldCaps("CHAVE DE ACESSO DA NFS-e", formatted(n.chaveAcesso(), "NFSe/infNFSe/@Id",
				"Remoção do prefixo NFS"), LEFT, y, 15.30f, 0.79f);

		// linha 2
		fieldCaps("NÚMERO DA NFS-e", raw(n.txt("infNFSe", "nNFSe"), "NFSe/infNFSe/nNFSe"), COLS[0], 2.27f, 5.11f, 0.69f);
		fieldCaps("COMPETÊNCIA DA NFS-e", formatted(fmtDate(n.txt("infNFSe", "DPS", "infDPS", "dCompet")),
				"NFSe/infNFSe/DPS/infDPS/dCompet", "Data DD/MM/AAAA"), COLS[1], 2.27f, 5.10f, 0.69f);
		fieldCaps("DATA E HORA DA EMISSÃO DA NFS-e", formatted(fmtDateTime(n.txt("infNFSe", "dhProc")),
				"NFSe/infNFSe/dhProc", "Data/hora DD/MM/AAAA HH:mm:ss sem conversão de fuso"), COLS[2], 2.27f, 5.11f, 0.69f);
		// linha 3
		fieldCaps("NÚMERO DA DPS", raw(n.txt("infNFSe", "DPS", "infDPS", "nDPS"), "NFSe/infNFSe/DPS/infDPS/nDPS"), COLS[0], 2.96f, 5.11f, 0.69f);
		fieldCaps("SÉRIE DA DPS", raw(n.txt("infNFSe", "DPS", "infDPS", "serie"), "NFSe/infNFSe/DPS/infDPS/serie"), COLS[1], 2.96f, 5.10f, 0.69f);
		fieldCaps("DATA E HORA DA EMISSÃO DA DPS", formatted(fmtDateTime(n.txt("infNFSe", "DPS", "infDPS", "dhEmi")),
				"NFSe/infNFSe/DPS/infDPS/dhEmi", "Data/hora DD/MM/AAAA HH:mm:ss sem conversão de fuso"), COLS[2], 2.96f, 5.11f, 0.69f);
		// linha 4 - "Emitente da NFS-e" com sombreamento (item 2.2.3)
		shade(COLS[0], 3.65f, 5.11f, 0.65f);
		fieldCaps("EMITENTE DA NFS-e", tableValue(n.txt("infNFSe", "DPS", "infDPS", "tpEmit"), DESC_TP_EMIT,
				"NFSe/infNFSe/DPS/infDPS/tpEmit", "Descrição do emitente da DPS"),
				COLS[0], 3.65f, 5.11f, 0.65f);
		fieldCaps("SITUAÇÃO DA NFS-e", tableValue(n.txt("infNFSe", "cStat"), DESC_CSTAT,
				"NFSe/infNFSe/cStat", "Descrição da situação da NFS-e"),
				COLS[1], 3.65f, 5.10f, 0.65f);
		fieldCaps("FINALIDADE", tableValue(n.txt("infNFSe", "DPS", "infDPS", "IBSCBS", "finNFSe"), DESC_FIN,
				"NFSe/infNFSe/DPS/infDPS/IBSCBS/finNFSe", "Descrição da finalidade da NFS-e"),
				COLS[2], 3.65f, 5.11f, 0.65f);

		// QR Code (item 2.4.3): 1,52 x 1,52 cm em X 17,48 / Y 1,67
		String qrUrl = "https://www.nfse.gov.br/ConsultaPublica/?tpc=1&chave=" + n.chaveAcesso();
		PDImageXObject qr = qrCode(qrUrl);
		image(qr, 17.48f, 1.67f, 1.52f, 1.52f);
		auditValue("QR Code", qrUrl, formatted(qrUrl, "NFSe/infNFSe/@Id",
				"URL oficial + chave de acesso sem o prefixo NFS"), false);

		// complemento do QR Code: 3 linhas, 6pt, no quadro 0,68 x 4,72 em (15,80/3,36)
		String[] qrTxt = {
				"A autenticidade desta NFS-e pode ser verificada",
				"pela leitura deste código QR ou pela consulta da",
				"chave de acesso no portal nacional da NFS-e"};
		float ty = 3.54f;
		for (String t : qrTxt) {
			text(t, 15.80f, ty, fontContent, 6);
			ty += 0.22f;
		}
	}

	private float drawPrestador(float y) throws Exception {
		auditBlock = "PRESTADOR / FORNECEDOR";
		float h = 2.57f;
		String[] p = {"infNFSe", "DPS", "infDPS", "prest"};

		blockTitle("PRESTADOR / FORNECEDOR", COLS[0], y, 5.11f, 0.63f);
		line(LEFT, y, RIGHT, y);
		field("CNPJ / CPF / NIF", formatted(fmtDoc(n.txt(cat(p, "CNPJ")), n.txt(cat(p, "CPF")), n.txt(cat(p, "NIF"))),
				"NFSe/infNFSe/DPS/infDPS/prest/CNPJ|CPF|NIF", "Seleção do identificador e máscara CNPJ/CPF"),
				COLS[1], y, 5.10f, 0.63f);
		field("Indicador Municipal (Inscrição)", raw(n.txt(cat(p, "IM")), "NFSe/infNFSe/DPS/infDPS/prest/IM"), COLS[2], y, 5.11f, 0.63f);
		field("Telefone", formatted(fmtFone(n.txt(cat(p, "fone"))), "NFSe/infNFSe/DPS/infDPS/prest/fone", "Máscara de telefone"), COLS[3], y, 5.08f, 0.63f);
		float y2 = y + 0.63f;
		// prestador: leiaute só traz CNPJ; demais dados cadastrais vêm em infNFSe/emit
		field("Nome / Nome Empresarial", raw(n.txt("infNFSe", "emit", "xNome"), "NFSe/infNFSe/emit/xNome"), COLS[0], y2, 10.21f, 0.64f);
		// município/UF do emitente: nome em xLocEmi e UF em emit/enderNac; quando
		// faltarem, resolve pelo código IBGE (cMun) conforme tabela 2.4.5
		String cMunEmit = firstNonEmpty(n.txt("infNFSe", "emit", "enderNac", "cMun"),
				n.txt(cat(p, "end", "endNac", "cMun")));
		String munEmit = firstNonEmpty(n.txt("infNFSe", "xLocEmi"), municipioNome(cMunEmit));
		String ufEmit = firstNonEmpty(n.txt("infNFSe", "emit", "enderNac", "UF"), ufFromIbge(cMunEmit));
		field("Município / Sigla UF", composed(munUf(munEmit, ufEmit),
				"NFSe/infNFSe/xLocEmi + NFSe/infNFSe/emit/enderNac/cMun|UF",
				"Conversão do código IBGE e concatenação Município / UF", !cMunEmit.isEmpty()),
				COLS[2], y2, 5.11f, 0.64f);
		field("Código IBGE / CEP", formatted(ibgeCep(cMunEmit,
				firstNonEmpty(n.txt("infNFSe", "emit", "enderNac", "CEP"), n.txt(cat(p, "end", "endNac", "CEP")))),
				"NFSe/infNFSe/emit/enderNac/cMun|CEP", "Concatenação código IBGE / CEP e máscara do CEP"),
				COLS[3], y2, 5.08f, 0.64f);
		float y3 = y2 + 0.64f;
		field("Endereço", formatted(endereco(n, "infNFSe", "emit", "enderNac"), "NFSe/infNFSe/emit/enderNac/xLgr|nro|xCpl|xBairro", "Concatenação dos componentes do endereço"), COLS[0], y3, 10.21f, 0.66f);
		field("Email", raw(n.txt("infNFSe", "emit", "email"), "NFSe/infNFSe/emit/email"), COLS[2], y3, 10.19f, 0.66f);
		float y4 = y3 + 0.66f;
		field("Simples Nacional na Data de Competência",
				tableValue(n.txt(cat(p, "regTrib", "opSimpNac")), DESC_OP_SIMP,
						"NFSe/infNFSe/DPS/infDPS/prest/regTrib/opSimpNac", "Descrição da opção pelo Simples Nacional"), COLS[0], y4, 5.11f, 0.64f);
		// tabela 2.4.5 indica Esq 10,51, mas o Anexo I posiciona na 2ª coluna
		// (5,41), ao lado do Simples Nacional - o item 2.2.4 manda o Anexo I
		// prevalecer na disposição dos campos
		field("Regime de Apuração Tributária pelo SN",
				tableValue(n.txt(cat(p, "regTrib", "regApTribSN")), DESC_REG_AP_SN,
						"NFSe/infNFSe/DPS/infDPS/prest/regTrib/regApTribSN", "Descrição do regime de apuração pelo Simples Nacional"), COLS[1], y4, 15.29f, 0.64f);
		return y + h;
	}

	private float drawTomador(float y) throws Exception {
		auditBlock = "TOMADOR / ADQUIRENTE";
		String[] t = {"infNFSe", "DPS", "infDPS", "toma"};
		if (!n.has(t)) {
			return suppressedBlock(y, "TOMADOR/ADQUIRENTE DA OPERAÇÃO NÃO IDENTIFICADO NA NFS-e");
		}
		float h = 1.94f;
		blockTitle("TOMADOR / ADQUIRENTE", COLS[0], y, 5.11f, 0.63f);
		line(LEFT, y, RIGHT, y);
		field("CNPJ / CPF / NIF", formatted(fmtDoc(n.txt(cat(t, "CNPJ")), n.txt(cat(t, "CPF")), n.txt(cat(t, "NIF"))),
				"NFSe/infNFSe/DPS/infDPS/toma/CNPJ|CPF|NIF", "Seleção do identificador e máscara CNPJ/CPF"),
				COLS[1], y, 5.10f, 0.63f);
		field("Indicador Municipal (Inscrição)", raw(n.txt(cat(t, "IM")), "NFSe/infNFSe/DPS/infDPS/toma/IM"), COLS[2], y, 5.11f, 0.63f);
		field("Telefone", formatted(fmtFone(n.txt(cat(t, "fone"))), "NFSe/infNFSe/DPS/infDPS/toma/fone", "Máscara de telefone"), COLS[3], y, 5.08f, 0.63f);
		float y2 = y + 0.63f;
		field("Nome / Nome Empresarial", raw(n.txt(cat(t, "xNome")), "NFSe/infNFSe/DPS/infDPS/toma/xNome"), COLS[0], y2, 10.21f, 0.64f);
		String cMun = n.txt(cat(t, "end", "endNac", "cMun"));
		field("Município / Sigla UF", composed(munUf(municipioNome(cMun), ufFromIbge(cMun)),
				"NFSe/infNFSe/DPS/infDPS/toma/end/endNac/cMun", "Conversão IBGE e concatenação Município / UF", !cMun.isEmpty()), COLS[2], y2, 5.11f, 0.64f);
		field("Código IBGE / CEP", formatted(ibgeCep(cMun, n.txt(cat(t, "end", "endNac", "CEP"))),
				"NFSe/infNFSe/DPS/infDPS/toma/end/endNac/cMun|CEP", "Concatenação código IBGE / CEP e máscara do CEP"), COLS[3], y2, 5.08f, 0.64f);
		float y3 = y2 + 0.64f;
		field("Endereço", formatted(endereco(n, cat(t, "end")), "NFSe/infNFSe/DPS/infDPS/toma/end/xLgr|nro|xCpl|xBairro", "Concatenação dos componentes do endereço"), COLS[0], y3, 10.21f, 0.67f);
		field("Email", raw(n.txt(cat(t, "email")), "NFSe/infNFSe/DPS/infDPS/toma/email"), COLS[2], y3, 10.19f, 0.67f);
		return y + h;
	}

	private float drawDestinatario(float y) throws Exception {
		auditBlock = "DESTINATÁRIO DA OPERAÇÃO";
		String[] d = {"infNFSe", "DPS", "infDPS", "IBSCBS", "dest"};
		String indDest = n.txt("infNFSe", "DPS", "infDPS", "IBSCBS", "indDest");
		if ("0".equals(indDest) || !n.has(d)) {
			String msg = "0".equals(indDest) && n.has("infNFSe", "DPS", "infDPS", "toma")
					? "O DESTINATÁRIO É O PRÓPRIO TOMADOR/ADQUIRENTE DA OPERAÇÃO"
					: "DESTINATÁRIO DA OPERAÇÃO NÃO IDENTIFICADO NA NFS-e";
			return suppressedBlock(y, msg);
		}
		float h = 1.94f;
		blockTitle("DESTINATÁRIO DA OPERAÇÃO", COLS[0], y, 5.11f, 0.63f);
		line(LEFT, y, RIGHT, y);
		field("CNPJ / CPF / NIF", formatted(fmtDoc(n.txt(cat(d, "CNPJ")), n.txt(cat(d, "CPF")), n.txt(cat(d, "NIF"))),
				"NFSe/infNFSe/DPS/infDPS/IBSCBS/dest/CNPJ|CPF|NIF", "Seleção do identificador e máscara CNPJ/CPF"),
				COLS[1], y, 5.10f, 0.63f);
		field("Telefone", formatted(fmtFone(n.txt(cat(d, "fone"))), "NFSe/infNFSe/DPS/infDPS/IBSCBS/dest/fone", "Máscara de telefone"), COLS[3], y, 5.08f, 0.63f);
		float y2 = y + 0.63f;
		field("Nome / Nome Empresarial", raw(n.txt(cat(d, "xNome")), "NFSe/infNFSe/DPS/infDPS/IBSCBS/dest/xNome"), COLS[0], y2, 10.21f, 0.64f);
		String cMun = n.txt(cat(d, "end", "endNac", "cMun"));
		field("Município / Sigla UF", composed(munUf(municipioNome(cMun), ufFromIbge(cMun)),
				"NFSe/infNFSe/DPS/infDPS/IBSCBS/dest/end/endNac/cMun", "Conversão IBGE e concatenação Município / UF", !cMun.isEmpty()), COLS[2], y2, 5.11f, 0.64f);
		field("Código IBGE / CEP", formatted(ibgeCep(cMun, n.txt(cat(d, "end", "endNac", "CEP"))),
				"NFSe/infNFSe/DPS/infDPS/IBSCBS/dest/end/endNac/cMun|CEP", "Concatenação código IBGE / CEP e máscara do CEP"), COLS[3], y2, 5.08f, 0.64f);
		float y3 = y2 + 0.64f;
		field("Endereço", formatted(endereco(n, cat(d, "end")), "NFSe/infNFSe/DPS/infDPS/IBSCBS/dest/end/xLgr|nro|xCpl|xBairro", "Concatenação dos componentes do endereço"), COLS[0], y3, 10.21f, 0.67f);
		field("Email", raw(n.txt(cat(d, "email")), "NFSe/infNFSe/DPS/infDPS/IBSCBS/dest/email"), COLS[2], y3, 10.19f, 0.67f);
		return y + h;
	}

	private float drawIntermediario(float y) throws Exception {
		auditBlock = "INTERMEDIÁRIO DA OPERAÇÃO";
		String[] i = {"infNFSe", "DPS", "infDPS", "interm"};
		if (!n.has(i)) {
			return suppressedBlock(y, "INTERMEDIÁRIO DA OPERAÇÃO NÃO IDENTIFICADO NA NFS-e");
		}
		float h = 1.94f;
		blockTitle("INTERMEDIÁRIO DA OPERAÇÃO", COLS[0], y, 5.11f, 0.63f);
		line(LEFT, y, RIGHT, y);
		field("CNPJ / CPF / NIF", formatted(fmtDoc(n.txt(cat(i, "CNPJ")), n.txt(cat(i, "CPF")), n.txt(cat(i, "NIF"))),
				"NFSe/infNFSe/DPS/infDPS/interm/CNPJ|CPF|NIF", "Seleção do identificador e máscara CNPJ/CPF"),
				COLS[1], y, 5.10f, 0.63f);
		field("Indicador Municipal (Inscrição)", raw(n.txt(cat(i, "IM")), "NFSe/infNFSe/DPS/infDPS/interm/IM"), COLS[2], y, 5.11f, 0.63f);
		field("Telefone", formatted(fmtFone(n.txt(cat(i, "fone"))), "NFSe/infNFSe/DPS/infDPS/interm/fone", "Máscara de telefone"), COLS[3], y, 5.08f, 0.63f);
		float y2 = y + 0.63f;
		field("Nome / Nome Empresarial", raw(n.txt(cat(i, "xNome")), "NFSe/infNFSe/DPS/infDPS/interm/xNome"), COLS[0], y2, 10.21f, 0.64f);
		String cMun = n.txt(cat(i, "end", "endNac", "cMun"));
		field("Município / Sigla UF", composed(munUf(municipioNome(cMun), ufFromIbge(cMun)),
				"NFSe/infNFSe/DPS/infDPS/interm/end/endNac/cMun", "Conversão IBGE e concatenação Município / UF", !cMun.isEmpty()), COLS[2], y2, 5.11f, 0.64f);
		field("Código IBGE / CEP", formatted(ibgeCep(cMun, n.txt(cat(i, "end", "endNac", "CEP"))),
				"NFSe/infNFSe/DPS/infDPS/interm/end/endNac/cMun|CEP", "Concatenação código IBGE / CEP e máscara do CEP"), COLS[3], y2, 5.08f, 0.64f);
		float y3 = y2 + 0.64f;
		field("Endereço", formatted(endereco(n, cat(i, "end")), "NFSe/infNFSe/DPS/infDPS/interm/end/xLgr|nro|xCpl|xBairro", "Concatenação dos componentes do endereço"), COLS[0], y3, 10.21f, 0.67f);
		field("Email", raw(n.txt(cat(i, "email")), "NFSe/infNFSe/DPS/infDPS/interm/email"), COLS[2], y3, 10.19f, 0.67f);
		return y + h;
	}

	private float drawServico(float y) throws Exception {
		auditBlock = "SERVIÇO PRESTADO";
		String[] s = {"infNFSe", "DPS", "infDPS", "serv"};
		String descServ = dashIfEmpty(n.txt(cat(s, "cServ", "xDescServ")));
		List<String> descLines = wrap(descServ, BODY_W - 0.20f, fontContent, 7, 1300);
		float descH = Math.max(0.63f, 0.30f + descLines.size() * 0.30f);
		float h = 0.63f + 0.38f + descH;

		blockTitle("SERVIÇO PRESTADO", COLS[0], y, 5.11f, 0.63f);
		line(LEFT, y, RIGHT, y);
		field("Código de Tributação Nacional / Municipal",
				formatted(joinRequiredSlash(fmtCTribNac(n.txt(cat(s, "cServ", "cTribNac"))), n.txt(cat(s, "cServ", "cTribMun"))),
						"NFSe/infNFSe/DPS/infDPS/serv/cServ/cTribNac + cTribMun", "Formatação do código nacional e concatenação com traço por componente ausente"),
				COLS[1], y, 5.10f, 0.63f);
		field("Código da NBS", formatted(fmtNbs(n.txt(cat(s, "cServ", "cNBS"))),
				"NFSe/infNFSe/DPS/infDPS/serv/cServ/cNBS", "Formatação n.nnnn.nn.nn"), COLS[2], y, 5.11f, 0.63f);
		field("Local da Prestação / Sigla UF / País", composed(localPrestacao(),
				"NFSe/infNFSe/xLocPrestacao + NFSe/infNFSe/DPS/infDPS/serv/locPrest/cLocPrestacao|cPaisPrestacao",
				"Conversão IBGE/UF, país ISO e concatenação Município / UF / País", true), COLS[3], y, 5.08f, 0.63f);

		// descrição do código de tributação (sem label - tabela 2.4.5)
		float y2 = y + 0.63f;
		String xTribMun = n.txt("infNFSe", "xTribMun");
		String descTrib = dashIfEmpty(!xTribMun.isEmpty() ? xTribMun : n.txt("infNFSe", "xTribNac"));
		String descTribImpresso = trunc(descTrib, BODY_W - 0.20f, fontContent, 7);
		text(descTribImpresso, LEFT + 0.10f, y2 + 0.28f, fontContent, 7);
		auditValue("Descrição do Código de Tributação Nacional / Municipal", descTribImpresso,
				formatted(descTrib, "NFSe/infNFSe/xTribMun | NFSe/infNFSe/xTribNac",
						"xTribMun quando preenchido; caso contrário xTribNac; truncamento visual"), false);

		float y3 = y2 + 0.38f;
		label("Descrição do Serviço", LEFT + 0.10f, y3 + 0.25f);
		float ty = y3 + 0.55f;
		for (String l : descLines) {
			text(l, LEFT + 0.10f, ty, fontContent, 7);
			ty += 0.30f;
		}
		auditValue("Descrição do Serviço", String.join("\n", descLines),
				formatted(descServ, "NFSe/infNFSe/DPS/infDPS/serv/cServ/xDescServ",
						"Conteúdo XML preservado; quebra de linha e truncamento visual até 1300 caracteres"), false);
		return y + h;
	}

	private float drawIssqn(float y) throws Exception {
		auditBlock = "TRIBUTAÇÃO MUNICIPAL (ISSQN)";
		String[] tm = {"infNFSe", "DPS", "infDPS", "valores", "trib", "tribMun"};
		if (!n.has(tm) || "4".equals(n.txt(cat(tm, "tribISSQN")))) {
			return suppressedBlock(y, "TRIBUTAÇÃO MUNICIPAL (ISSQN) - OPERAÇÃO NÃO SUJEITA AO ISSQN");
		}
		// linhas suprimíveis (nota 5)
		String regEsp = n.txt("infNFSe", "DPS", "infDPS", "prest", "regTrib", "regEspTrib");
		String tpImun = n.txt(cat(tm, "tpImunidade"));
		String tpSusp = n.txt(cat(tm, "exigSusp", "tpSusp"));
		String nProc = n.txt(cat(tm, "exigSusp", "nProcesso"));
		boolean row2 = !(regEsp.isEmpty() && tpImun.isEmpty() && tpSusp.isEmpty() && nProc.isEmpty());

		String tpBM = n.txt("infNFSe", "valores", "tpBM");
		String vCalcBM = firstNonEmpty(n.txt("infNFSe", "valores", "vCalcBM"),
				n.txt(cat(tm, "BM", "vRedBCBM")));
		String vDR = totalDeducoesReducoes();
		String vDescIncond = n.txt("infNFSe", "DPS", "infDPS", "valores", "vDescCondIncond", "vDescIncond");
		boolean row3 = !(tpBM.isEmpty() && vCalcBM.isEmpty() && vDR.isEmpty() && vDescIncond.isEmpty());

		// Anexo I: o título do bloco é uma célula na 1ª linha, ao lado dos campos
		float h = 0.63f + (row2 ? 0.65f : 0) + (row3 ? 0.65f : 0) + 0.64f;
		blockTitle("TRIBUTAÇÃO MUNICIPAL (ISSQN)", COLS[0], y, 5.11f, 0.63f);
		line(LEFT, y, RIGHT, y);
		field("Tipo de Tributação do ISSQN", tableValue(n.txt(cat(tm, "tribISSQN")), DESC_TRIB_ISSQN,
				"NFSe/infNFSe/DPS/infDPS/valores/trib/tribMun/tribISSQN", "Descrição do tipo de tributação do ISSQN"),
				COLS[1], y, 5.10f, 0.63f);
		String cLocIncid = n.txt("infNFSe", "cLocIncid");
		field("Município / Sigla UF / País da Incidência do ISSQN", composed(
				joinRequiredSlash(firstNonEmpty(n.txt("infNFSe", "xLocIncid"), municipioNome(cLocIncid)),
						ufFromIbge(cLocIncid),
						firstNonEmpty(n.txt(cat(tm, "cPaisResult")), cLocIncid.isEmpty() ? "" : "BR")),
				"NFSe/infNFSe/xLocIncid|cLocIncid + NFSe/infNFSe/DPS/infDPS/valores/trib/tribMun/cPaisResult",
				"Conversão IBGE/UF, país ISO e concatenação Município / UF / País", !cLocIncid.isEmpty()),
				COLS[2], y, 10.19f, 0.63f);
		float r = y + 0.63f;
		if (row2) {
			field("Regime Especial de Tributação do ISSQN", tableValue(regEsp, DESC_REG_ESP,
					"NFSe/infNFSe/DPS/infDPS/prest/regTrib/regEspTrib", "Descrição do regime especial de tributação"),
					COLS[0], r, 5.11f, 0.65f);
			field("Tipo de Imunidade do ISSQN", tableValue(tpImun, DESC_TP_IMUNIDADE,
					"NFSe/infNFSe/DPS/infDPS/valores/trib/tribMun/tpImunidade", "Descrição do tipo de imunidade"), COLS[1], r, 5.10f, 0.65f);
			field("Suspensão da Exigibilidade do ISSQN", tableValue(tpSusp, DESC_TP_SUSP,
					"NFSe/infNFSe/DPS/infDPS/valores/trib/tribMun/exigSusp/tpSusp", "Descrição da suspensão de exigibilidade"), COLS[2], r, 5.11f, 0.65f);
			field("Número Processo Suspensão", raw(nProc, "NFSe/infNFSe/DPS/infDPS/valores/trib/tribMun/exigSusp/nProcesso"), COLS[3], r, 5.08f, 0.65f);
			r += 0.65f;
		}
		if (row3) {
			field("Benefício Municipal", tableValue(tpBM, DESC_TP_BM, "NFSe/infNFSe/valores/tpBM", "Descrição do benefício municipal"), COLS[0], r, 5.11f, 0.65f);
			field("Cálculo do BM", formatted(money(vCalcBM), "NFSe/infNFSe/valores/vCalcBM | NFSe/infNFSe/DPS/infDPS/valores/trib/tribMun/BM/vRedBCBM", "Valor monetário brasileiro"), COLS[1], r, 5.10f, 0.65f);
			field("Total Deduções/Reduções", formatted(money(vDR), "NFSe/infNFSe/DPS/infDPS/valores/vDedRed/vDR | NFSe/infNFSe/valores/vCalcDR + NFSe/infNFSe/IBSCBS/valores/vCalcReeRepRes", "Prioriza vDR; na ausência, soma vCalcDR + vCalcReeRepRes"), COLS[2], r, 5.11f, 0.65f);
			field("Desconto Incondicionado", formatted(money(vDescIncond), "NFSe/infNFSe/DPS/infDPS/valores/vDescCondIncond/vDescIncond", "Valor monetário brasileiro"), COLS[3], r, 5.08f, 0.65f);
			r += 0.65f;
		}
		field("BC ISSQN", formatted(money(n.txt("infNFSe", "valores", "vBC")), "NFSe/infNFSe/valores/vBC", "Valor monetário brasileiro"), COLS[0], r, 5.11f, 0.64f);
		field("Alíquota Aplicada", formatted(pct(n.txt("infNFSe", "valores", "pAliqAplic")), "NFSe/infNFSe/valores/pAliqAplic", "Percentual brasileiro"), COLS[1], r, 5.10f, 0.64f);
		field("Retenção do ISSQN", tableValue(n.txt(cat(tm, "tpRetISSQN")), DESC_RET_ISSQN,
				"NFSe/infNFSe/DPS/infDPS/valores/trib/tribMun/tpRetISSQN", "Descrição da retenção do ISSQN"),
				COLS[2], r, 5.11f, 0.64f);
		field("ISSQN Apurado", formatted(money(n.txt("infNFSe", "valores", "vISSQN")), "NFSe/infNFSe/valores/vISSQN", "Valor monetário brasileiro; sem recálculo"), COLS[3], r, 5.08f, 0.64f);
		return y + h;
	}

	private float drawTribFederal(float y) throws Exception {
		auditBlock = "TRIBUTAÇÃO FEDERAL (EXCETO CBS)";
		String[] tf = {"infNFSe", "DPS", "infDPS", "valores", "trib", "tribFed"};
		// nota 6: linha PIS/COFINS impressa para competência até o fim de 2026
		String compet = n.txt("infNFSe", "DPS", "infDPS", "dCompet");
		boolean pisRow = compet.isEmpty() || compet.compareTo("2027") < 0;

		float h = 0.63f + (pisRow ? 0.65f : 0);
		blockTitle("TRIBUTAÇÃO FEDERAL (EXCETO CBS)", COLS[0], y, 5.11f, 0.63f);
		line(LEFT, y, RIGHT, y);
		field("IRRF", formatted(money(n.txt(cat(tf, "vRetIRRF"))), "NFSe/infNFSe/DPS/infDPS/valores/trib/tribFed/vRetIRRF", "Valor monetário brasileiro"), COLS[1], y, 5.10f, 0.63f);
		field("Contribuição Previdenciária - Retida", formatted(money(n.txt(cat(tf, "vRetCP"))), "NFSe/infNFSe/DPS/infDPS/valores/trib/tribFed/vRetCP", "Valor monetário brasileiro"), COLS[2], y, 5.11f, 0.63f);
		field("Contribuições Sociais - Retidas", formatted(money(n.txt(cat(tf, "vRetCSLL"))), "NFSe/infNFSe/DPS/infDPS/valores/trib/tribFed/vRetCSLL", "Valor monetário brasileiro"), COLS[3], y, 5.08f, 0.63f);
		if (pisRow) {
			float r = y + 0.63f;
			field("PIS - Débito Apuração Própria", formatted(money(n.txt(cat(tf, "piscofins", "vPis"))), "NFSe/infNFSe/DPS/infDPS/valores/trib/tribFed/piscofins/vPis", "Valor monetário brasileiro"), COLS[0], r, 5.11f, 0.65f);
			field("COFINS - Débito Apuração Própria", formatted(money(n.txt(cat(tf, "piscofins", "vCofins"))), "NFSe/infNFSe/DPS/infDPS/valores/trib/tribFed/piscofins/vCofins", "Valor monetário brasileiro"), COLS[1], r, 5.10f, 0.65f);
			field("Descrição Contrib. Sociais - Retidas",
					tableValue(n.txt(cat(tf, "piscofins", "tpRetPisCofins")), DESC_RET_PISCOFINS,
							"NFSe/infNFSe/DPS/infDPS/valores/trib/tribFed/piscofins/tpRetPisCofins", "Descrição da retenção PIS/COFINS/CSLL"),
					COLS[2], r, 10.19f, 0.65f);
		}
		return y + h;
	}

	private float drawIbsCbs(float y) throws Exception {
		auditBlock = "TRIBUTAÇÃO IBS / CBS";
		String[] ib = {"infNFSe", "IBSCBS"};
		String[] dpsIb = {"infNFSe", "DPS", "infDPS", "IBSCBS"};
		float h = 0.63f + 0.64f + 0.65f + 0.66f;
		blockTitle("TRIBUTAÇÃO IBS / CBS", COLS[0], y, 5.11f, 0.63f);
		line(LEFT, y, RIGHT, y);
		field("CST / cClassTrib",
				formatted(joinRequiredSlash(n.txt(cat(dpsIb, "valores", "trib", "gIBSCBS", "CST")), n.txt(cat(dpsIb, "valores", "trib", "gIBSCBS", "cClassTrib"))),
						"NFSe/infNFSe/DPS/infDPS/IBSCBS/valores/trib/gIBSCBS/CST + cClassTrib", "Concatenação CST / cClassTrib com traço por componente ausente"),
				COLS[1], y, 5.10f, 0.63f);
		field("Indicador de Operação / Código IBGE Incidência / Município Incidência / Sigla UF",
				composed(joinRequiredSlash(n.txt(cat(dpsIb, "cIndOp")), n.txt(cat(ib, "cLocalidadeIncid")),
						firstNonEmpty(n.txt(cat(ib, "xLocalidadeIncid")), municipioNome(n.txt(cat(ib, "cLocalidadeIncid")))),
						ufFromIbge(n.txt(cat(ib, "cLocalidadeIncid")))),
						"NFSe/infNFSe/DPS/infDPS/IBSCBS/cIndOp + NFSe/infNFSe/IBSCBS/cLocalidadeIncid|xLocalidadeIncid",
						"Conversão IBGE/UF e concatenação dos quatro componentes", !n.txt(cat(ib, "cLocalidadeIncid")).isEmpty()),
				COLS[2], y, 10.19f, 0.63f);

		float r = y + 0.63f;
		field("Exclusões e Reduções da Base de Cálculo", formatted(money(somaExclusoes()),
				"vDescIncond + vCalcReeRepRes + vISSQN + vPis + vCofins", "Soma expressamente determinada pela NT 008"), COLS[0], r, 5.11f, 0.64f);
		field("Base de Cálculo Após Exclusões e Reduções", formatted(money(n.txt(cat(ib, "valores", "vBC"))), "NFSe/infNFSe/IBSCBS/valores/vBC", "Valor monetário brasileiro; sem recálculo"), COLS[1], r, 5.10f, 0.64f);
		field("Red. Alíquota IBS / Red. Alíquota CBS",
				formatted(joinRequiredSlash(pct(n.txt(cat(ib, "valores", "uf", "pRedAliqUF"))), pct(n.txt(cat(ib, "valores", "mun", "pRedAliqMun"))),
						pct(n.txt(cat(ib, "valores", "fed", "pRedAliqCBS")))), "NFSe/infNFSe/IBSCBS/valores/uf/pRedAliqUF + mun/pRedAliqMun + fed/pRedAliqCBS", "Percentuais brasileiros e concatenação com traço por ausência"),
				COLS[2], r, 5.11f, 0.64f);
		field("Alíquota - IBS UF / IBS Mun",
				formatted(joinRequiredSlash(pct(n.txt(cat(ib, "valores", "uf", "pIBSUF"))), pct(n.txt(cat(ib, "valores", "mun", "pIBSMun")))), "NFSe/infNFSe/IBSCBS/valores/uf/pIBSUF + mun/pIBSMun", "Percentuais brasileiros e concatenação com traço por ausência"),
				COLS[3], r, 5.08f, 0.64f);

		r += 0.64f;
		field("Alíq. Efetiva Municipal - IBS", formatted(pct(n.txt(cat(ib, "valores", "mun", "pAliqEfetMun"))), "NFSe/infNFSe/IBSCBS/valores/mun/pAliqEfetMun", "Percentual brasileiro"), COLS[0], r, 5.11f, 0.65f);
		field("Valor Apurado Municipal - IBS", formatted(money(n.txt(cat(ib, "totCIBS", "gIBS", "gIBSMunTot", "vIBSMun"))), "NFSe/infNFSe/IBSCBS/totCIBS/gIBS/gIBSMunTot/vIBSMun", "Valor monetário brasileiro; sem recálculo"), COLS[1], r, 5.10f, 0.65f);
		field("Alíq. Efetiva Estadual - IBS", formatted(pct(n.txt(cat(ib, "valores", "uf", "pAliqEfetUF"))), "NFSe/infNFSe/IBSCBS/valores/uf/pAliqEfetUF", "Percentual brasileiro"), COLS[2], r, 5.11f, 0.65f);
		field("Valor Apurado Estadual - IBS", formatted(money(n.txt(cat(ib, "totCIBS", "gIBS", "gIBSUFTot", "vIBSUF"))), "NFSe/infNFSe/IBSCBS/totCIBS/gIBS/gIBSUFTot/vIBSUF", "Valor monetário brasileiro; sem recálculo"), COLS[3], r, 5.08f, 0.65f);

		r += 0.65f;
		field("Valor Total Apurado - IBS", formatted(money(n.txt(cat(ib, "totCIBS", "gIBS", "vIBSTot"))), "NFSe/infNFSe/IBSCBS/totCIBS/gIBS/vIBSTot", "Valor monetário brasileiro; sem recálculo"), COLS[0], r, 5.11f, 0.66f);
		field("Alíquota - CBS", formatted(pct(n.txt(cat(ib, "valores", "fed", "pCBS"))), "NFSe/infNFSe/IBSCBS/valores/fed/pCBS", "Percentual brasileiro"), COLS[1], r, 5.10f, 0.66f);
		field("Alíquota Efetiva - CBS", formatted(pct(n.txt(cat(ib, "valores", "fed", "pAliqEfetCBS"))), "NFSe/infNFSe/IBSCBS/valores/fed/pAliqEfetCBS", "Percentual brasileiro"), COLS[2], r, 5.11f, 0.66f);
		field("Valor Total Apurado - CBS", formatted(money(n.txt(cat(ib, "totCIBS", "gCBS", "vCBS"))), "NFSe/infNFSe/IBSCBS/totCIBS/gCBS/vCBS", "Valor monetário brasileiro; sem recálculo"), COLS[3], r, 5.08f, 0.66f);
		return y + h;
	}

	private float drawValorTotal(float y) throws Exception {
		auditBlock = "VALOR TOTAL DA NFS-e";
		float h = 0.67f + 0.69f;
		blockTitle("VALOR TOTAL DA NFS-e", COLS[0], y, 5.11f, 0.67f);
		line(LEFT, y, RIGHT, y);
		field("Valor da Operação / Serviço", formatted(money(n.txt("infNFSe", "DPS", "infDPS", "valores", "vServPrest", "vServ")),
				"NFSe/infNFSe/DPS/infDPS/valores/vServPrest/vServ", "Valor monetário brasileiro; sem recálculo"),
				COLS[1], y, 5.10f, 0.67f);
		field("Desconto Incondicionado", formatted(money(n.txt("infNFSe", "DPS", "infDPS", "valores", "vDescCondIncond", "vDescIncond")),
				"NFSe/infNFSe/DPS/infDPS/valores/vDescCondIncond/vDescIncond", "Valor monetário brasileiro; sem recálculo"),
				COLS[2], y, 5.11f, 0.67f);
		field("Desconto Condicionado", formatted(money(n.txt("infNFSe", "DPS", "infDPS", "valores", "vDescCondIncond", "vDescCond")),
				"NFSe/infNFSe/DPS/infDPS/valores/vDescCondIncond/vDescCond", "Valor monetário brasileiro; sem recálculo"),
				COLS[3], y, 5.08f, 0.67f);
		float r = y + 0.67f;
		field("Total das Retenções (ISSQN / Federais)", formatted(money(n.txt("infNFSe", "valores", "vTotalRet")), "NFSe/infNFSe/valores/vTotalRet", "Valor monetário brasileiro; tag totalizadora, sem recálculo"), COLS[0], r, 5.11f, 0.69f);
		field("Valor Líquido da NFS-e", formatted(money(n.txt("infNFSe", "valores", "vLiq")), "NFSe/infNFSe/valores/vLiq", "Valor monetário brasileiro; tag totalizadora, sem recálculo"), COLS[1], r, 5.10f, 0.69f);
		field("Total do IBS/CBS", formatted(money(somaIbsCbs()), "NFSe/infNFSe/IBSCBS/totCIBS/gIBS/vIBSTot + NFSe/infNFSe/IBSCBS/totCIBS/gCBS/vCBS", "Soma expressamente determinada pela NT 008"), COLS[2], r, 5.11f, 0.69f);
		// "Valor Líquido da NFS-e + IBS/CBS" com sombreamento (item 2.2.3) -
		// a célula vai até a borda direita do corpo
		shade(COLS[3], r, RIGHT - COLS[3], 0.69f);
		field("Valor Líquido da NFS-e + IBS/CBS", formatted(money(n.txt("infNFSe", "IBSCBS", "totCIBS", "vTotNF")),
				"NFSe/infNFSe/IBSCBS/totCIBS/vTotNF", "Valor monetário brasileiro; tag totalizadora, sem recálculo"),
				COLS[3], r, RIGHT - COLS[3], 0.69f);
		return y + h;
	}

	private void drawInfoComplementares(float y) throws Exception {
		auditBlock = "INFORMAÇÕES COMPLEMENTARES";
		float h = BOTTOM - y;
		blockStrip("INFORMAÇÕES COMPLEMENTARES", y);
		line(LEFT, y, RIGHT, y);
		line(LEFT, BOTTOM, RIGHT, BOTTOM); // fim do corpo

		StringBuilder sb = new StringBuilder();
		appendInfo(sb, "Inf. Cont.:", n.txt("infNFSe", "DPS", "infDPS", "serv", "infoCompl", "xInfComp"));
		appendInfo(sb, "NFS-e Subst.:", n.txt("infNFSe", "DPS", "infDPS", "subst", "chSubstda"));
		appendInfo(sb, "Doc. Ref.:", n.txt("infNFSe", "DPS", "infDPS", "serv", "infoCompl", "docRef"));
		appendInfo(sb, "Cod. Obra:", n.txt("infNFSe", "DPS", "infDPS", "serv", "obra", "cObra"));
		appendInfo(sb, "Insc. Imob.:", n.txt("infNFSe", "DPS", "infDPS", "IBSCBS", "imovel", "inscImobFisc"));
		appendInfo(sb, "Cod. Evt.:", n.txt("infNFSe", "DPS", "infDPS", "serv", "atvEvento", "idAtvEvt"));
		appendInfo(sb, "Doc. Tec.:", n.txt("infNFSe", "DPS", "infDPS", "serv", "infoCompl", "idDocTec"));
		appendInfo(sb, "Núm. Ped.:", n.txt("infNFSe", "DPS", "infDPS", "serv", "infoCompl", "xPed"));
		appendInfo(sb, "Item Ped.:", n.txt("infNFSe", "DPS", "infDPS", "serv", "infoCompl", "gItemPed", "xItemPed"));
		appendInfo(sb, "Inf. A. T. Mun.:", n.txt("infNFSe", "xOutInf"));

		// nota 10: Totais Aproximados dos Tributos (linha fixa obrigatória)
		String totais = totaisAproximados();

		int maxLines = Math.max(1, (int) ((h - 0.67f) / 0.30f) - 1);
		List<String> lines = wrap(sb.toString(), BODY_W - 0.20f, fontContent, 7, 1997);
		if (lines.size() > maxLines) {
			lines = lines.subList(0, maxLines);
			String last = lines.get(maxLines - 1);
			lines.set(maxLines - 1, last.length() > 3 ? last.substring(0, last.length() - 3) + "..." : last);
		}
		float ty = y + 0.67f;
		for (String l : lines) {
			text(l, LEFT + 0.10f, ty, fontContent, 7);
			ty += 0.30f;
		}
		String totaisImpresso = trunc(totais, BODY_W - 0.20f, fontContent, 7);
		text(totaisImpresso, LEFT + 0.10f, ty, fontContent, 7);
		String infoImpresso = String.join("\n", lines);
		if (!infoImpresso.isEmpty()) {
			infoImpresso += "\n";
		}
		infoImpresso += totaisImpresso;
		auditValue("Informações Complementares", infoImpresso,
				formatted(infoImpresso,
						"NFSe/infNFSe/DPS/infDPS/serv/infoCompl + subst + obra + IBSCBS/imovel + atvEvento + NFSe/infNFSe/xOutInf + valores/trib/totTrib",
						"Ordem da NT 008, grupos separados por pipe, quebra/truncamento visual e linha fixa de Totais Aproximados"), false);
	}

	/**
	 * Canhoto (item 2.1.13, Nota 11 - opcional). No Anexo I é uma caixa
	 * destacada do corpo, recuada 0,10 da borda da página (0,30-20,70),
	 * com três células e labels em caixa alta 7pt negrito.
	 */
	private void drawCanhoto() throws Exception {
		auditBlock = "CANHOTO";
		float y = CANHOTO_TOP, h = CANHOTO_H;
		rect(0.30f, y, 20.40f, h);
		cs.stroke();
		line(5.41f, y, 5.41f, y + h);
		line(10.51f, y, 10.51f, y + h);
		text("DATA CIENTIFICAÇÃO:", 0.40f, y + 0.28f, fontBold, 7);
		text("IDENTIFICAÇÃO E ASSINATURA", 5.51f, y + 0.28f, fontBold, 7);
		text("Nº NFS-e / CHAVE NFS-e", 10.61f, y + 0.28f, fontBold, 7);
		// id da NFS-e sem o prefixo "NFS" (tabela 2.4.5). Ex.: nnn / nnn
		String numeroChave = trunc(joinRequiredSlash(n.txt("infNFSe", "nNFSe"), n.chaveAcesso()), 10.0f, fontContent, 7);
		text(numeroChave,
				10.61f, y + h - 0.12f, fontContent, 7);
		auditValue("Nº NFS-e / Chave NFS-e", numeroChave,
				formatted(numeroChave, "NFSe/infNFSe/nNFSe + NFSe/infNFSe/@Id", "Concatenação e remoção do prefixo NFS da chave"), false);
	}

	private void watermark(String txt) throws Exception {
		cs.saveGraphicsState();
		// MULTIPLY: o cinza só escurece o fundo, deixando o conteúdo da nota
		// legível por cima - efeito de marca d'água atrás do documento
		PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
		gs.setBlendMode(BlendMode.MULTIPLY);
		cs.setGraphicsStateParameters(gs);
		cs.setNonStrokingColor(166, 166, 166); // K35
		cs.beginText();
		cs.setFont(fontArial, 60);
		float w = fontArial.getStringWidth(txt) / 1000f * 60;
		// diagonal, centralizada na página
		Matrix m = Matrix.getRotateInstance(Math.toRadians(45), PAGE_W / 2 * CM, PAGE_H / 2 * CM);
		m.concatenate(Matrix.getTranslateInstance(-w / 2, 0));
		cs.setTextMatrix(m);
		cs.showText(txt);
		cs.endText();
		cs.restoreGraphicsState();
	}

	// ----------------------------------------------------------- composições

	private float suppressedBlock(float y, String msg) throws Exception {
		float h = 0.32f; // altura mínima - notas 2/3/4
		line(LEFT, y, RIGHT, y);
		// exemplos do item 2.4.5.1: texto centralizado, fundo branco, peso normal
		textCenter(msg, LEFT, BODY_W, y + 0.23f, fontContent, 7);
		audit.add(new DanfseAuditEntry(auditBlock, auditBlock, msg,
				origemFraseEspecial(auditBlock), "Frase especial de bloco prevista na NT 008",
				false, false, true));
		return y + h;
	}

	/** Título de bloco ocupando a primeira célula da primeira linha. */
	private void blockTitle(String title, float x, float y, float w, float h) throws Exception {
		shade(x, y, w, h);
		// título de bloco: 7pt, negrito, caixa alta (item 2.4.1)
		List<String> ls = splitTitle(title, w);
		float ty = y + h / 2 - (ls.size() - 1) * 0.14f + 0.09f;
		for (String l : ls) {
			text(l, x + 0.10f, ty, fontBold, 7);
			ty += 0.28f;
		}
	}

	/** Faixa de título ocupando a largura inteira do bloco (Alt. 0,39 - tabela 2.4.5). */
	private void blockStrip(String title, float y) throws Exception {
		shade(LEFT, y, BODY_W, 0.39f);
		text(title, LEFT + 0.10f, y + 0.27f, fontBold, 7);
	}

	private List<String> splitTitle(String title, float w) throws IOException {
		List<String> out = new ArrayList<>();
		if (width(title, fontBold, 7) <= w - 0.2f) {
			out.add(title);
			return out;
		}
		int cut = title.indexOf(' ', title.length() / 2);
		if (cut < 0) {
			out.add(title);
		} else {
			out.add(title.substring(0, cut));
			out.add(title.substring(cut + 1));
		}
		return out;
	}

	/** Campo com label 6pt (item 2.4.2) e conteúdo 7pt. */
	private void field(String lbl, FieldValue value, float x, float y, float w, float h) throws Exception {
		label(lbl, x + 0.10f, y + 0.25f);
		String v = display(value);
		String printed = trunc(v, w - 0.20f, fontContent, 7);
		text(printed, x + 0.10f, y + h - 0.12f, fontContent, 7);
		auditValue(lbl, printed, value, false);
	}

	/** Campo com label 7pt caixa alta (item 2.4.2 - identificação e Anexo I).
	 *  O label deve vir já em caixa alta, preservando a grafia "NFS-e". */
	private void fieldCaps(String lbl, FieldValue value, float x, float y, float w, float h) throws Exception {
		text(trunc(lbl, w - 0.2f, fontBold, 7), x + 0.10f, y + 0.28f, fontBold, 7);
		String v = display(value);
		String printed = trunc(v, w - 0.20f, fontContent, 7);
		text(printed, x + 0.10f, y + h - 0.14f, fontContent, 7);
		auditValue(lbl, printed, value, false);
	}

	private FieldValue raw(String value, String source) {
		return new FieldValue(value, source, "Sem formatação além do limite visual previsto", false);
	}

	private FieldValue formatted(String value, String source, String rule) {
		return new FieldValue(value, source, rule, false);
	}

	private FieldValue composed(String value, String source, String rule, boolean tableConverted) {
		return new FieldValue(value, source, rule, tableConverted);
	}

	private FieldValue tableValue(String rawValue, Map<String, String> table, String source, String rule) {
		String value = rawValue == null ? "" : rawValue.trim();
		boolean converted = !value.isEmpty() && table.containsKey(value);
		return new FieldValue(converted ? table.get(value) : value, source, rule, converted);
	}

	private static String display(FieldValue value) {
		return value == null || value.value == null || value.value.trim().isEmpty() ? "-" : value.value.trim();
	}

	private void auditValue(String label, String printed, FieldValue value, boolean specialBlock) {
		audit.add(new DanfseAuditEntry(auditBlock, label, printed, value.source, value.rule,
				value.usedDash, value.tableConverted, specialBlock));
	}

	private static boolean containsMissingMarker(String value) {
		if (value == null || value.trim().isEmpty() || "-".equals(value.trim())) {
			return true;
		}
		String v = value.trim();
		return v.startsWith("- / ") || v.endsWith(" / -") || v.contains(" / - / ");
	}

	private static String origemFraseEspecial(String block) {
		if ("TOMADOR / ADQUIRENTE".equals(block)) {
			return "NFSe/infNFSe/DPS/infDPS/toma";
		}
		if ("DESTINATÁRIO DA OPERAÇÃO".equals(block)) {
			return "NFSe/infNFSe/DPS/infDPS/IBSCBS/indDest + dest + NFSe/infNFSe/DPS/infDPS/toma";
		}
		if ("INTERMEDIÁRIO DA OPERAÇÃO".equals(block)) {
			return "NFSe/infNFSe/DPS/infDPS/interm";
		}
		return "NFSe/infNFSe/DPS/infDPS/valores/trib/tribMun/tribISSQN";
	}

	private void label(String lbl, float x, float yBase) throws Exception {
		text(trunc(lbl, 12f, fontBold, 6), x, yBase, fontBold, 6);
	}

	// ------------------------------------------------------------ primitivas

	/** Coordenadas em cm a partir do topo/esquerda da página. */
	private void rect(float x, float yTop, float w, float h) throws IOException {
		cs.addRect(x * CM, (PAGE_H - yTop - h) * CM, w * CM, h * CM);
	}

	private void shade(float x, float yTop, float w, float h) throws IOException {
		cs.setNonStrokingColor(242, 242, 242); // cinza 5% (item 2.2.3)
		rect(x, yTop, w, h);
		cs.fill();
		cs.setNonStrokingColor(0, 0, 0);
	}

	private void line(float x1, float y1, float x2, float y2) throws IOException {
		cs.moveTo(x1 * CM, (PAGE_H - y1) * CM);
		cs.lineTo(x2 * CM, (PAGE_H - y2) * CM);
		cs.stroke();
	}

	private void text(String s, float x, float yBase, PDFont f, float size) throws IOException {
		if (s == null || s.isEmpty()) {
			return;
		}
		cs.beginText();
		cs.setFont(f, size);
		cs.newLineAtOffset(x * CM, (PAGE_H - yBase) * CM);
		cs.showText(sanitize(s));
		cs.endText();
	}

	private void textCenter(String s, float x, float w, float yBase, PDFont f, float size) throws IOException {
		float tw = width(sanitize(s), f, size);
		text(s, x + (w - tw) / 2, yBase, f, size);
	}

	private void image(PDImageXObject img, float x, float yTop, float w, float h) throws IOException {
		cs.drawImage(img, x * CM, (PAGE_H - yTop - h) * CM, w * CM, h * CM);
	}

	private float width(String s, PDFont f, float size) throws IOException {
		return f.getStringWidth(sanitize(s)) / 1000f * size / CM; // em cm
	}

	private String trunc(String s, float maxCm, PDFont f, float size) throws IOException {
		if (width(s, f, size) <= maxCm) {
			return s;
		}
		String t = s;
		while (t.length() > 1 && width(t + "...", f, size) > maxCm) {
			t = t.substring(0, t.length() - 1);
		}
		return t + "...";
	}

	private List<String> wrap(String s, float maxCm, PDFont f, float size, int maxChars) throws IOException {
		if (s.length() > maxChars) {
			s = s.substring(0, maxChars - 3) + "...";
		}
		List<String> out = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		for (String word : s.split("\\s+")) {
			String cand = cur.length() == 0 ? word : cur + " " + word;
			if (width(cand, f, size) > maxCm && cur.length() > 0) {
				out.add(cur.toString());
				cur = new StringBuilder(word);
			} else {
				cur = new StringBuilder(cand);
			}
		}
		if (cur.length() > 0) {
			out.add(cur.toString());
		}
		if (out.isEmpty()) {
			out.add("");
		}
		return out;
	}

	/** Preserva Unicode do XML e normaliza somente controles para espaço visual. */
	private static String sanitize(String s) {
		StringBuilder sb = new StringBuilder(s.length());
		for (char c : s.toCharArray()) {
			if (c == '\n' || c == '\r' || c == '\t') {
				sb.append(' ');
			} else if (!Character.isISOControl(c)) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private PDImageXObject qrCode(String content) throws Exception {
		Map<EncodeHintType, Object> hints = new HashMap<>();
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
		hints.put(EncodeHintType.MARGIN, 0);
		BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 300, 300, hints);
		BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);
		return LosslessFactory.createFromImage(doc, img);
	}

	/** Carrega TTF do classpath com subset; sem o resource, usa a fonte padrão. */
	private PDFont loadFont(String resource, PDFont fallback) {
		try (InputStream in = DanfseGenerator.class.getResourceAsStream(resource)) {
			if (in == null) {
				return fallback;
			}
			return PDType0Font.load(doc, in, true);
		} catch (Exception e) {
			return fallback;
		}
	}

	private PDImageXObject loadLogo() {
		try (InputStream in = DanfseGenerator.class.getResourceAsStream("/danfse/logo-nfse.png")) {
			if (in == null) {
				return null;
			}
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			byte[] b = new byte[8192];
			int r;
			while ((r = in.read(b)) > 0) {
				buf.write(b, 0, r);
			}
			return PDImageXObject.createFromByteArray(doc, buf.toByteArray(), "logo-nfse");
		} catch (Exception e) {
			return null;
		}
	}

	// ----------------------------------------------------------- formatações

	private static final DecimalFormat MONEY;
	private static final DecimalFormat PCT;
	static {
		DecimalFormatSymbols br = new DecimalFormatSymbols(new Locale("pt", "BR"));
		MONEY = new DecimalFormat("#,##0.00", br);
		PCT = new DecimalFormat("#,##0.00'%'", br);
	}

	private static String money(String v) {
		if (v == null || v.trim().isEmpty()) {
			return "-";
		}
		try {
			return "R$ " + MONEY.format(new BigDecimal(v.trim()));
		} catch (NumberFormatException e) {
			return v;
		}
	}

	private static String pct(String v) {
		if (v == null || v.trim().isEmpty()) {
			return "-";
		}
		try {
			return PCT.format(new BigDecimal(v.trim()));
		} catch (NumberFormatException e) {
			return v;
		}
	}

	private static String fmtDate(String iso) {
		if (iso == null || iso.length() < 10) {
			return "-";
		}
		return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
	}

	private static String fmtDateTime(String iso) {
		if (iso == null || iso.length() < 19) {
			return "-";
		}
		return fmtDate(iso) + " " + iso.substring(11, 19);
	}

	private static String fmtDoc(String cnpj, String cpf, String nif) {
		if (cnpj != null && cnpj.length() == 14) {
			return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." + cnpj.substring(5, 8)
					+ "/" + cnpj.substring(8, 12) + "-" + cnpj.substring(12);
		}
		if (cpf != null && cpf.length() == 11) {
			return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
		}
		return firstNonEmpty(cnpj, cpf, nif);
	}

	/** Telefone com máscara (nn) nnnn-nnnn ou (nn) nnnnn-nnnn quando possível. */
	private static String fmtFone(String fone) {
		if (fone == null) {
			return "";
		}
		String d = fone.trim();
		if (d.matches("\\d{10}")) {
			return "(" + d.substring(0, 2) + ") " + d.substring(2, 6) + "-" + d.substring(6);
		}
		if (d.matches("\\d{11}")) {
			return "(" + d.substring(0, 2) + ") " + d.substring(2, 7) + "-" + d.substring(7);
		}
		return d;
	}

	private static String fmtCTribNac(String c) {
		if (c != null && c.length() == 6) {
			return c.substring(0, 2) + "." + c.substring(2, 4) + "." + c.substring(4);
		}
		return c == null ? "" : c;
	}

	private static String fmtNbs(String c) {
		if (c != null && c.length() == 9) {
			return c.substring(0, 1) + "." + c.substring(1, 5) + "." + c.substring(5, 7) + "." + c.substring(7);
		}
		return c == null || c.isEmpty() ? "-" : c;
	}

	private static String ibgeCep(String cMun, String cep) {
		String c = (cep != null && cep.length() == 8)
				? cep.substring(0, 2) + "." + cep.substring(2, 5) + "-" + cep.substring(5)
				: cep;
		return firstNonEmpty(cMun, "-") + " / " + firstNonEmpty(c, "-");
	}

	private static String munUf(String mun, String uf) {
		if ((mun == null || mun.isEmpty()) && (uf == null || uf.isEmpty())) {
			return "-";
		}
		return firstNonEmpty(mun, "-") + " / " + firstNonEmpty(uf, "-");
	}

	private String endereco(NfseXml n, String... endPath) {
		StringBuilder sb = new StringBuilder();
		// xLgr/nro/xCpl/xBairro podem estar no nível do "end" (DPS) ou dentro de enderNac (emit)
		for (String tag : new String[] {"xLgr", "nro", "xCpl", "xBairro"}) {
			String v = n.txt(cat(endPath, tag));
			if (v.isEmpty()) {
				v = n.txt(cat(endPath, "endNac", tag));
			}
			if (!v.isEmpty()) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(v);
			}
		}
		return sb.toString();
	}

	/**
	 * "Local da Prestação / Sigla UF / País" (tabela 2.4.5): concatena o nome
	 * do município, a respectiva UF da Tabela do IBGE e o código ISO do país.
	 * O XML traz só o nome em xLocPrestacao; a UF é resolvida pelos 2 primeiros
	 * dígitos de locPrest/cLocPrestacao. Prestação no exterior (cLocPrestacao
	 * ausente) sai sem UF, com o país de cPaisPrestacao.
	 */
	private String localPrestacao() {
		String[] loc = {"infNFSe", "DPS", "infDPS", "serv", "locPrest"};
		String cLoc = n.txt(cat(loc, "cLocPrestacao"));
		String mun = firstNonEmpty(n.txt("infNFSe", "xLocPrestacao"), municipioNome(cLoc));
		String pais = n.txt(cat(loc, "cPaisPrestacao"));
		if (pais.isEmpty() && !cLoc.isEmpty()) {
			pais = "BR"; // código IBGE municipal implica localidade brasileira
		}
		return joinRequiredSlash(mun, ufFromIbge(cLoc), pais);
	}

	/** Concatena campos compostos e preserva o traço de cada componente ausente. */
	private static String joinRequiredSlash(String... parts) {
		StringBuilder sb = new StringBuilder();
		for (String p : parts) {
			if (sb.length() > 0) {
				sb.append(" / ");
			}
			sb.append(dashIfEmpty(p));
		}
		return sb.toString();
	}

	private static String dashIfEmpty(String value) {
		return value == null || value.trim().isEmpty() ? "-" : value.trim();
	}

	private String totalDeducoesReducoes() {
		String totalXml = n.txt("infNFSe", "DPS", "infDPS", "valores", "vDedRed", "vDR");
		if (!totalXml.isEmpty()) {
			return totalXml;
		}
		String vCalcDR = n.txt("infNFSe", "valores", "vCalcDR");
		String vCalcReeRepRes = n.txt("infNFSe", "IBSCBS", "valores", "vCalcReeRepRes");
		if (vCalcDR.isEmpty() && vCalcReeRepRes.isEmpty()) {
			return "";
		}
		try {
			return new BigDecimal(firstNonEmpty(vCalcDR, "0"))
					.add(new BigDecimal(firstNonEmpty(vCalcReeRepRes, "0"))).toPlainString();
		} catch (NumberFormatException e) {
			return "";
		}
	}

	private String somaExclusoes() {
		BigDecimal total = BigDecimal.ZERO;
		boolean any = false;
		String[][] paths = {
				{"infNFSe", "DPS", "infDPS", "valores", "vDescCondIncond", "vDescIncond"},
				{"infNFSe", "IBSCBS", "valores", "vCalcReeRepRes"},
				{"infNFSe", "valores", "vISSQN"},
				{"infNFSe", "DPS", "infDPS", "valores", "trib", "tribFed", "piscofins", "vPis"},
				{"infNFSe", "DPS", "infDPS", "valores", "trib", "tribFed", "piscofins", "vCofins"}};
		for (String[] p : paths) {
			String v = n.txt(p);
			if (!v.isEmpty()) {
				try {
					total = total.add(new BigDecimal(v));
					any = true;
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return any ? total.toPlainString() : "";
	}

	private String somaIbsCbs() {
		String ibs = n.txt("infNFSe", "IBSCBS", "totCIBS", "gIBS", "vIBSTot");
		String cbs = n.txt("infNFSe", "IBSCBS", "totCIBS", "gCBS", "vCBS");
		if (ibs.isEmpty() && cbs.isEmpty()) {
			return ""; // nota 12: sem informação no XML, imprime "-"
		}
		try {
			return new BigDecimal(firstNonEmpty(ibs, "0")).add(new BigDecimal(firstNonEmpty(cbs, "0"))).toPlainString();
		} catch (NumberFormatException e) {
			return "";
		}
	}

	private String totaisAproximados() {
		String fed = totalAproximado("vTotTribFed", "pTotTribFed");
		String est = totalAproximado("vTotTribEst", "pTotTribEst");
		String mun = totalAproximado("vTotTribMun", "pTotTribMun");
		return "Totais Aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: " + fed
				+ "; Estaduais: " + est + "; Municipais: " + mun;
	}

	private String totalAproximado(String monetaryTag, String percentTag) {
		String moneyValue = n.txt("infNFSe", "DPS", "infDPS", "valores", "trib", "totTrib", "vTotTrib", monetaryTag);
		if (!moneyValue.isEmpty()) {
			return money(moneyValue);
		}
		String percentValue = n.txt("infNFSe", "DPS", "infDPS", "valores", "trib", "totTrib", "pTotTrib", percentTag);
		return percentValue.isEmpty() ? "-" : pct(percentValue);
	}

	private static void appendInfo(StringBuilder sb, String prefix, String value) {
		if (value != null && !value.trim().isEmpty()) {
			if (sb.length() > 0) {
				sb.append(" | ");
			}
			sb.append(prefix).append(" ").append(value.trim());
		}
	}

	private static String firstNonEmpty(String... vals) {
		for (String v : vals) {
			if (v != null && !v.trim().isEmpty()) {
				return v.trim();
			}
		}
		return "";
	}

	private static String[] cat(String[] base, String... extra) {
		String[] out = new String[base.length + extra.length];
		System.arraycopy(base, 0, out, 0, base.length);
		System.arraycopy(extra, 0, out, base.length, extra.length);
		return out;
	}

	/** UF a partir dos 2 primeiros dígitos do código IBGE do município. */
	private static String ufFromIbge(String cMun) {
		if (cMun == null || cMun.length() < 2) {
			return "";
		}
		return UF_IBGE.getOrDefault(cMun.substring(0, 2), "");
	}

	/**
	 * Nome do município a partir do código IBGE (tabela 2.4.5 manda usar a
	 * descrição da tabela do IBGE). Consulta o de-para de {@link IbgeMunicipios}
	 * (API do IBGE com fallback no snapshot embutido); sem ele, tenta os
	 * nomes que o próprio XML traz e, em último caso, imprime o código.
	 */
	private String municipioNome(String cMun) {
		if (cMun == null || cMun.isEmpty()) {
			return "";
		}
		String nome = IbgeMunicipios.nome(cMun);
		if (!nome.isEmpty()) {
			return nome;
		}
		// quando o município do tomador coincide com o local de incidência
		// IBS/CBS, o XML já traz o nome
		if (cMun.equals(n.txt("infNFSe", "IBSCBS", "cLocalidadeIncid"))) {
			nome = n.txt("infNFSe", "IBSCBS", "xLocalidadeIncid");
			if (!nome.isEmpty()) {
				return nome;
			}
		}
		if (cMun.equals(n.txt("infNFSe", "cLocIncid"))) {
			nome = n.txt("infNFSe", "xLocIncid");
			if (!nome.isEmpty()) {
				return nome;
			}
		}
		return "";
	}

	// ------------------------------------------------------- tabelas de domínio

	private static final Map<String, String> UF_IBGE = new HashMap<>();
	private static final Map<String, String> DESC_TP_EMIT = new HashMap<>();
	private static final Map<String, String> DESC_CSTAT = new HashMap<>();
	private static final Map<String, String> DESC_FIN = new HashMap<>();
	private static final Map<String, String> DESC_AMB_GER = new HashMap<>();
	private static final Map<String, String> DESC_TP_AMB = new HashMap<>();
	private static final Map<String, String> DESC_OP_SIMP = new HashMap<>();
	private static final Map<String, String> DESC_REG_AP_SN = new HashMap<>();
	private static final Map<String, String> DESC_REG_ESP = new HashMap<>();
	private static final Map<String, String> DESC_TRIB_ISSQN = new HashMap<>();
	private static final Map<String, String> DESC_TP_IMUNIDADE = new HashMap<>();
	private static final Map<String, String> DESC_TP_SUSP = new HashMap<>();
	private static final Map<String, String> DESC_TP_BM = new HashMap<>();
	private static final Map<String, String> DESC_RET_ISSQN = new HashMap<>();
	private static final Map<String, String> DESC_RET_PISCOFINS = new HashMap<>();

	static {
		String[][] ufs = {{"11", "RO"}, {"12", "AC"}, {"13", "AM"}, {"14", "RR"}, {"15", "PA"}, {"16", "AP"},
				{"17", "TO"}, {"21", "MA"}, {"22", "PI"}, {"23", "CE"}, {"24", "RN"}, {"25", "PB"}, {"26", "PE"},
				{"27", "AL"}, {"28", "SE"}, {"29", "BA"}, {"31", "MG"}, {"32", "ES"}, {"33", "RJ"}, {"35", "SP"},
				{"41", "PR"}, {"42", "SC"}, {"43", "RS"}, {"50", "MS"}, {"51", "MT"}, {"52", "GO"}, {"53", "DF"}};
		for (String[] u : ufs) {
			UF_IBGE.put(u[0], u[1]);
		}

		DESC_TP_EMIT.put("1", "Prestador");
		DESC_TP_EMIT.put("2", "Tomador");
		DESC_TP_EMIT.put("3", "Intermediário");

		DESC_CSTAT.put("100", "NFS-e Gerada");
		DESC_CSTAT.put("101", "NFS-e Cancelada");
		DESC_CSTAT.put("102", "NFS-e Substituída");

		DESC_FIN.put("0", "NFS-e regular");
		DESC_FIN.put("1", "NFS-e substituta");
		DESC_FIN.put("2", "NFS-e de decisão judicial");
		DESC_FIN.put("3", "NFS-e avulsa");

		DESC_AMB_GER.put("1", "Prefeitura");
		DESC_AMB_GER.put("2", "Sistema Nacional NFS-e");

		DESC_TP_AMB.put("1", "Produção");
		DESC_TP_AMB.put("2", "Homologação");

		DESC_OP_SIMP.put("1", "Não Optante");
		DESC_OP_SIMP.put("2", "Optante - Microempreendedor Individual (MEI)");
		DESC_OP_SIMP.put("3", "Optante - Microempresa ou Empresa de Pequeno Porte (ME/EPP)");

		DESC_REG_AP_SN.put("1", "Regime de apuração dos tributos federais e municipal pelo SN");
		DESC_REG_AP_SN.put("2", "Regime de apuração dos tributos federais pelo SN e ISSQN fora do SN");
		DESC_REG_AP_SN.put("3", "Regime de apuração dos tributos federais e municipal fora do SN");

		DESC_REG_ESP.put("0", "Nenhum");
		DESC_REG_ESP.put("1", "Ato Cooperado");
		DESC_REG_ESP.put("2", "Estimativa");
		DESC_REG_ESP.put("3", "Microempresa Municipal");
		DESC_REG_ESP.put("4", "Notário ou Registrador");
		DESC_REG_ESP.put("5", "Profissional Autônomo");
		DESC_REG_ESP.put("6", "Sociedade de Profissionais");
		DESC_REG_ESP.put("9", "Outros");

		DESC_TRIB_ISSQN.put("1", "Operação Tributável");
		DESC_TRIB_ISSQN.put("2", "Imunidade");
		DESC_TRIB_ISSQN.put("3", "Exportação de Serviço");
		DESC_TRIB_ISSQN.put("4", "Não Incidência");

		DESC_TP_IMUNIDADE.put("0", "Imunidade (tipo não informado na nota de origem)");
		DESC_TP_IMUNIDADE.put("1", "Patrimônio, renda ou serviços, uns dos outros (CF88, Art 150, VI, a)");
		DESC_TP_IMUNIDADE.put("2", "Templos de qualquer culto (CF88, Art 150, VI, b)");
		DESC_TP_IMUNIDADE.put("3", "Patrimônio, renda ou serviços dos partidos políticos, inclusive suas fundações, das entidades sindicais dos trabalhadores, das instituições de educação e de assistência social, sem fins lucrativos, atendidos os requisitos da lei (CF88, Art 150, VI, c)");
		DESC_TP_IMUNIDADE.put("4", "Livros, jornais, periódicos e o papel destinado a sua impressão (CF88, Art 150, VI, d)");
		DESC_TP_IMUNIDADE.put("5", "Fonogramas e videofonogramas musicais produzidos no Brasil contendo obras musicais ou literomusicais de autores brasileiros e/ou obras em geral interpretadas por artistas brasileiros bem como os suportes materiais ou arquivos digitais que os contenham, salvo na etapa de replicação industrial de mídias ópticas de leitura a laser (CF88, Art 150, VI, e)");

		DESC_TP_SUSP.put("1", "Exigibilidade Suspensa por Decisão Judicial");
		DESC_TP_SUSP.put("2", "Exigibilidade Suspensa por Processo Administrativo");

		DESC_TP_BM.put("1", "Isenção");
		DESC_TP_BM.put("2", "Redução da BC em percentual");
		DESC_TP_BM.put("3", "Redução da BC em valor monetário");
		DESC_TP_BM.put("4", "Alíquota Diferenciada");

		DESC_RET_ISSQN.put("1", "Não Retido");
		DESC_RET_ISSQN.put("2", "Retido pelo Tomador");
		DESC_RET_ISSQN.put("3", "Retido pelo Intermediário");

		DESC_RET_PISCOFINS.put("0", "PIS/COFINS/CSLL Não Retidos");
		DESC_RET_PISCOFINS.put("1", "PIS/COFINS Retidos");
		DESC_RET_PISCOFINS.put("2", "PIS/COFINS Não Retidos");
		DESC_RET_PISCOFINS.put("3", "PIS/COFINS/CSLL Retidos");
		DESC_RET_PISCOFINS.put("4", "PIS/COFINS Retidos, CSLL Não Retido");
		DESC_RET_PISCOFINS.put("5", "PIS Retido, COFINS/CSLL Não Retido");
		DESC_RET_PISCOFINS.put("6", "COFINS Retido, PIS/CSLL Não Retido");
		DESC_RET_PISCOFINS.put("7", "PIS Não Retido, COFINS/CSLL Retidos");
		DESC_RET_PISCOFINS.put("8", "PIS/COFINS Não Retidos, CSLL Retido");
		DESC_RET_PISCOFINS.put("9", "COFINS Não Retido, PIS/CSLL Retidos");
	}
}
