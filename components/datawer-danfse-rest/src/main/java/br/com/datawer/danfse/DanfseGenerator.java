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
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
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

	/**
	 * @param nfse     XML da NFS-e já parseado
	 * @param situacao null para nota normal, ou {@link #SIT_CANCELADA} /
	 *                 {@link #SIT_SUBSTITUIDA} para imprimir a marca d'água
	 *                 (o cancelamento não consta no XML da NFS-e; vem por evento)
	 */
	public byte[] generate(NfseXml nfse, String situacao) throws Exception {
		this.n = nfse;
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
			return out.toByteArray();
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
		String mun = n.txt("infNFSe", "xLocEmi");
		String uf = n.txt("infNFSe", "emit", "enderNac", "UF");
		text(trunc("Município: " + mun + (uf.isEmpty() ? "" : " / " + uf), 5.0f, fontContent, 8),
				15.70f, 0.72f, fontContent, 8);
		text("Ambiente Gerador: " + DESC_AMB_GER.getOrDefault(n.txt("infNFSe", "ambGer"), n.txt("infNFSe", "ambGer")),
				15.70f, 1.07f, fontContent, 6);
		text("Tipo de Ambiente: " + DESC_TP_AMB.getOrDefault(n.txt("infNFSe", "DPS", "infDPS", "tpAmb"), "-"),
				15.70f, 1.32f, fontContent, 6);
	}

	private void drawDadosNfse() throws Exception {
		// divisória superior (única) com o cabeçalho; a inferior (4,34) é do
		// prestador. Os campos seguem as posições da tabela 2.4.5
		line(LEFT, 1.46f, RIGHT, 1.46f);
		float y = 1.48f;

		// chave de acesso
		fieldCaps("CHAVE DE ACESSO DA NFS-e", n.chaveAcesso(), LEFT, y, 15.30f, 0.79f);

		// linha 2
		fieldCaps("NÚMERO DA NFS-e", n.txt("infNFSe", "nNFSe"), COLS[0], 2.27f, 5.11f, 0.69f);
		fieldCaps("COMPETÊNCIA DA NFS-e", fmtDate(n.txt("infNFSe", "DPS", "infDPS", "dCompet")), COLS[1], 2.27f, 5.10f, 0.69f);
		fieldCaps("DATA E HORA DA EMISSÃO DA NFS-e", fmtDateTime(n.txt("infNFSe", "dhProc")), COLS[2], 2.27f, 5.11f, 0.69f);
		// linha 3
		fieldCaps("NÚMERO DA DPS", n.txt("infNFSe", "DPS", "infDPS", "nDPS"), COLS[0], 2.96f, 5.11f, 0.69f);
		fieldCaps("SÉRIE DA DPS", n.txt("infNFSe", "DPS", "infDPS", "serie"), COLS[1], 2.96f, 5.10f, 0.69f);
		fieldCaps("DATA E HORA DA EMISSÃO DA DPS", fmtDateTime(n.txt("infNFSe", "DPS", "infDPS", "dhEmi")), COLS[2], 2.96f, 5.11f, 0.69f);
		// linha 4 - "Emitente da NFS-e" com sombreamento (item 2.2.3)
		shade(COLS[0], 3.65f, 5.11f, 0.65f);
		fieldCaps("EMITENTE DA NFS-e", DESC_TP_EMIT.getOrDefault(n.txt("infNFSe", "DPS", "infDPS", "tpEmit"), "-"),
				COLS[0], 3.65f, 5.11f, 0.65f);
		fieldCaps("SITUAÇÃO DA NFS-e", DESC_CSTAT.getOrDefault(n.txt("infNFSe", "cStat"), n.txt("infNFSe", "cStat")),
				COLS[1], 3.65f, 5.10f, 0.65f);
		fieldCaps("FINALIDADE", DESC_FIN.getOrDefault(n.txt("infNFSe", "DPS", "infDPS", "IBSCBS", "finNFSe"), "-"),
				COLS[2], 3.65f, 5.11f, 0.65f);

		// QR Code (item 2.4.3): 1,52 x 1,52 cm em X 17,48 / Y 1,67
		PDImageXObject qr = qrCode("https://www.nfse.gov.br/ConsultaPublica/?tpc=1&chave=" + n.chaveAcesso());
		image(qr, 17.48f, 1.67f, 1.52f, 1.52f);

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
		float h = 2.57f;
		String[] p = {"infNFSe", "DPS", "infDPS", "prest"};

		blockTitle("PRESTADOR / FORNECEDOR", COLS[0], y, 5.11f, 0.63f);
		line(LEFT, y, RIGHT, y);
		field("CNPJ / CPF / NIF", fmtDoc(n.txt(cat(p, "CNPJ")), n.txt(cat(p, "CPF")), n.txt(cat(p, "NIF"))),
				COLS[1], y, 5.10f, 0.63f);
		field("Indicador Municipal (Inscrição)", n.txt(cat(p, "IM")), COLS[2], y, 5.11f, 0.63f);
		field("Telefone", n.txt(cat(p, "fone")), COLS[3], y, 5.08f, 0.63f);
		float y2 = y + 0.63f;
		// prestador: leiaute só traz CNPJ; demais dados cadastrais vêm em infNFSe/emit
		field("Nome / Nome Empresarial", n.txt("infNFSe", "emit", "xNome"), COLS[0], y2, 10.21f, 0.64f);
		field("Município / Sigla UF", munUf(n.txt("infNFSe", "xLocEmi"), n.txt("infNFSe", "emit", "enderNac", "UF")),
				COLS[2], y2, 5.11f, 0.64f);
		field("Código IBGE / CEP", ibgeCep(n.txt("infNFSe", "emit", "enderNac", "cMun"), n.txt("infNFSe", "emit", "enderNac", "CEP")),
				COLS[3], y2, 5.08f, 0.64f);
		float y3 = y2 + 0.64f;
		field("Endereço", endereco(n, "infNFSe", "emit", "enderNac"), COLS[0], y3, 10.21f, 0.66f);
		field("Email", n.txt("infNFSe", "emit", "email"), COLS[2], y3, 10.19f, 0.66f);
		float y4 = y3 + 0.66f;
		field("Simples Nacional na Data de Competência",
				DESC_OP_SIMP.getOrDefault(n.txt(cat(p, "regTrib", "opSimpNac")), "-"), COLS[0], y4, 5.11f, 0.64f);
		// tabela 2.4.5 indica Esq 10,51, mas o Anexo I posiciona na 2ª coluna
		// (5,41), ao lado do Simples Nacional - o item 2.2.4 manda o Anexo I
		// prevalecer na disposição dos campos
		field("Regime de Apuração Tributária pelo SN",
				DESC_REG_AP_SN.getOrDefault(n.txt(cat(p, "regTrib", "regApTribSN")), "-"), COLS[1], y4, 15.29f, 0.64f);
		return y + h;
	}

	private float drawTomador(float y) throws Exception {
		String[] t = {"infNFSe", "DPS", "infDPS", "toma"};
		if (!n.has(t)) {
			return suppressedBlock(y, "TOMADOR/ADQUIRENTE DA OPERAÇÃO NÃO IDENTIFICADO NA NFS-e");
		}
		float h = 1.94f;
		blockTitle("TOMADOR / ADQUIRENTE", COLS[0], y, 5.11f, 0.63f);
		line(LEFT, y, RIGHT, y);
		field("CNPJ / CPF / NIF", fmtDoc(n.txt(cat(t, "CNPJ")), n.txt(cat(t, "CPF")), n.txt(cat(t, "NIF"))),
				COLS[1], y, 5.10f, 0.63f);
		field("Indicador Municipal (Inscrição)", n.txt(cat(t, "IM")), COLS[2], y, 5.11f, 0.63f);
		field("Telefone", n.txt(cat(t, "fone")), COLS[3], y, 5.08f, 0.63f);
		float y2 = y + 0.63f;
		field("Nome / Nome Empresarial", n.txt(cat(t, "xNome")), COLS[0], y2, 10.21f, 0.64f);
		String cMun = n.txt(cat(t, "end", "endNac", "cMun"));
		field("Município / Sigla UF", munUf(municipioNome(cMun), ufFromIbge(cMun)), COLS[2], y2, 5.11f, 0.64f);
		field("Código IBGE / CEP", ibgeCep(cMun, n.txt(cat(t, "end", "endNac", "CEP"))), COLS[3], y2, 5.08f, 0.64f);
		float y3 = y2 + 0.64f;
		field("Endereço", endereco(n, cat(t, "end")), COLS[0], y3, 10.21f, 0.67f);
		field("Email", n.txt(cat(t, "email")), COLS[2], y3, 10.19f, 0.67f);
		return y + h;
	}

	private float drawDestinatario(float y) throws Exception {
		String[] d = {"infNFSe", "DPS", "infDPS", "IBSCBS", "dest"};
		if (!n.has(d)) {
			String msg = n.has("infNFSe", "DPS", "infDPS", "toma")
					? "O DESTINATÁRIO É O PRÓPRIO TOMADOR/ADQUIRENTE DA OPERAÇÃO"
					: "DESTINATÁRIO DA OPERAÇÃO NÃO IDENTIFICADO NA NFS-e";
			return suppressedBlock(y, msg);
		}
		float h = 1.94f;
		blockTitle("DESTINATÁRIO DA OPERAÇÃO", COLS[0], y, 5.11f, 0.63f);
		line(LEFT, y, RIGHT, y);
		field("CNPJ / CPF / NIF", fmtDoc(n.txt(cat(d, "CNPJ")), n.txt(cat(d, "CPF")), n.txt(cat(d, "NIF"))),
				COLS[1], y, 5.10f, 0.63f);
		field("Telefone", n.txt(cat(d, "fone")), COLS[3], y, 5.08f, 0.63f);
		float y2 = y + 0.63f;
		field("Nome / Nome Empresarial", n.txt(cat(d, "xNome")), COLS[0], y2, 10.21f, 0.64f);
		String cMun = n.txt(cat(d, "end", "endNac", "cMun"));
		field("Município / Sigla UF", munUf(municipioNome(cMun), ufFromIbge(cMun)), COLS[2], y2, 5.11f, 0.64f);
		field("Código IBGE / CEP", ibgeCep(cMun, n.txt(cat(d, "end", "endNac", "CEP"))), COLS[3], y2, 5.08f, 0.64f);
		float y3 = y2 + 0.64f;
		field("Endereço", endereco(n, cat(d, "end")), COLS[0], y3, 10.21f, 0.67f);
		field("Email", n.txt(cat(d, "email")), COLS[2], y3, 10.19f, 0.67f);
		return y + h;
	}

	private float drawIntermediario(float y) throws Exception {
		String[] i = {"infNFSe", "DPS", "infDPS", "interm"};
		if (!n.has(i)) {
			return suppressedBlock(y, "INTERMEDIÁRIO DA OPERAÇÃO NÃO IDENTIFICADO NA NFS-e");
		}
		float h = 1.94f;
		blockTitle("INTERMEDIÁRIO DA OPERAÇÃO", COLS[0], y, 5.11f, 0.63f);
		line(LEFT, y, RIGHT, y);
		field("CNPJ / CPF / NIF", fmtDoc(n.txt(cat(i, "CNPJ")), n.txt(cat(i, "CPF")), n.txt(cat(i, "NIF"))),
				COLS[1], y, 5.10f, 0.63f);
		field("Indicador Municipal (Inscrição)", n.txt(cat(i, "IM")), COLS[2], y, 5.11f, 0.63f);
		field("Telefone", n.txt(cat(i, "fone")), COLS[3], y, 5.08f, 0.63f);
		float y2 = y + 0.63f;
		field("Nome / Nome Empresarial", n.txt(cat(i, "xNome")), COLS[0], y2, 10.21f, 0.64f);
		String cMun = n.txt(cat(i, "end", "endNac", "cMun"));
		field("Município / Sigla UF", munUf(municipioNome(cMun), ufFromIbge(cMun)), COLS[2], y2, 5.11f, 0.64f);
		field("Código IBGE / CEP", ibgeCep(cMun, n.txt(cat(i, "end", "endNac", "CEP"))), COLS[3], y2, 5.08f, 0.64f);
		float y3 = y2 + 0.64f;
		field("Endereço", endereco(n, cat(i, "end")), COLS[0], y3, 10.21f, 0.67f);
		field("Email", n.txt(cat(i, "email")), COLS[2], y3, 10.19f, 0.67f);
		return y + h;
	}

	private float drawServico(float y) throws Exception {
		String[] s = {"infNFSe", "DPS", "infDPS", "serv"};
		String descServ = n.txt(cat(s, "cServ", "xDescServ"));
		List<String> descLines = wrap(descServ, BODY_W - 0.20f, fontContent, 7, 1300);
		float descH = Math.max(0.63f, 0.30f + descLines.size() * 0.30f);
		float h = 0.63f + 0.38f + descH;

		blockTitle("SERVIÇO PRESTADO", COLS[0], y, 5.11f, 0.63f);
		line(LEFT, y, RIGHT, y);
		field("Código de Tributação Nacional / Municipal",
				fmtCTribNac(n.txt(cat(s, "cServ", "cTribNac"))) + slash(n.txt(cat(s, "cServ", "cTribMun"))),
				COLS[1], y, 5.10f, 0.63f);
		field("Código da NBS", fmtNbs(n.txt(cat(s, "cServ", "cNBS"))), COLS[2], y, 5.11f, 0.63f);
		field("Local da Prestação / Sigla UF / País",
				n.txt("infNFSe", "xLocPrestacao") + " / " + paisPrestacao(), COLS[3], y, 5.08f, 0.63f);

		// descrição do código de tributação (sem label - tabela 2.4.5)
		float y2 = y + 0.63f;
		String xTribMun = n.txt("infNFSe", "xTribMun");
		String descTrib = !xTribMun.isEmpty() ? xTribMun : n.txt("infNFSe", "xTribNac");
		text(trunc(descTrib, BODY_W - 0.20f, fontContent, 7), LEFT + 0.10f, y2 + 0.28f, fontContent, 7);

		float y3 = y2 + 0.38f;
		label("Descrição do Serviço", LEFT + 0.10f, y3 + 0.25f);
		float ty = y3 + 0.55f;
		for (String l : descLines) {
			text(l, LEFT + 0.10f, ty, fontContent, 7);
			ty += 0.30f;
		}
		return y + h;
	}

	private float drawIssqn(float y) throws Exception {
		String[] tm = {"infNFSe", "DPS", "infDPS", "valores", "trib", "tribMun"};
		if (!n.has(tm)) {
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
		String vDR = firstNonEmpty(n.txt("infNFSe", "DPS", "infDPS", "valores", "vDedRed", "vDR"),
				n.txt("infNFSe", "valores", "vCalcDR"));
		String vDescIncond = n.txt("infNFSe", "DPS", "infDPS", "valores", "vDescCondIncond", "vDescIncond");
		boolean row3 = !(tpBM.isEmpty() && vCalcBM.isEmpty() && vDR.isEmpty() && vDescIncond.isEmpty());

		// Anexo I: o título do bloco é uma célula na 1ª linha, ao lado dos campos
		float h = 0.63f + (row2 ? 0.65f : 0) + (row3 ? 0.65f : 0) + 0.64f;
		blockTitle("TRIBUTAÇÃO MUNICIPAL (ISSQN)", COLS[0], y, 5.11f, 0.63f);
		line(LEFT, y, RIGHT, y);
		field("Tipo de Tributação do ISSQN", DESC_TRIB_ISSQN.getOrDefault(n.txt(cat(tm, "tribISSQN")), "-"),
				COLS[1], y, 5.10f, 0.63f);
		field("Município / Sigla UF / País da Incidência do ISSQN",
				n.txt("infNFSe", "xLocIncid") + " / " + ufFromIbge(n.txt("infNFSe", "cLocIncid")) + " / BR",
				COLS[2], y, 10.19f, 0.63f);
		float r = y + 0.63f;
		if (row2) {
			field("Regime Especial de Tributação do ISSQN", DESC_REG_ESP.getOrDefault(regEsp, regEsp),
					COLS[0], r, 5.11f, 0.65f);
			field("Tipo de Imunidade do ISSQN", tpImun.isEmpty() ? "-" : tpImun, COLS[1], r, 5.10f, 0.65f);
			field("Suspensão da Exigibilidade do ISSQN", tpSusp.isEmpty() ? "-" : tpSusp, COLS[2], r, 5.11f, 0.65f);
			field("Número Processo Suspensão", nProc, COLS[3], r, 5.08f, 0.65f);
			r += 0.65f;
		}
		if (row3) {
			field("Benefício Municipal", tpBM, COLS[0], r, 5.11f, 0.65f);
			field("Cálculo do BM", money(vCalcBM), COLS[1], r, 5.10f, 0.65f);
			field("Total Deduções/Reduções", money(vDR), COLS[2], r, 5.11f, 0.65f);
			field("Desconto Incondicionado", money(vDescIncond), COLS[3], r, 5.08f, 0.65f);
			r += 0.65f;
		}
		field("BC ISSQN", money(n.txt("infNFSe", "valores", "vBC")), COLS[0], r, 5.11f, 0.64f);
		field("Alíquota Aplicada", pct(n.txt("infNFSe", "valores", "pAliqAplic")), COLS[1], r, 5.10f, 0.64f);
		field("Retenção do ISSQN", DESC_RET_ISSQN.getOrDefault(n.txt(cat(tm, "tpRetISSQN")), "-"),
				COLS[2], r, 5.11f, 0.64f);
		field("ISSQN Apurado", money(n.txt("infNFSe", "valores", "vISSQN")), COLS[3], r, 5.08f, 0.64f);
		return y + h;
	}

	private float drawTribFederal(float y) throws Exception {
		String[] tf = {"infNFSe", "DPS", "infDPS", "valores", "trib", "tribFed"};
		// nota 6: linha PIS/COFINS impressa para competência até o fim de 2026
		String compet = n.txt("infNFSe", "DPS", "infDPS", "dCompet");
		boolean pisRow = n.has(cat(tf, "piscofins")) && (compet.isEmpty() || compet.compareTo("2027") < 0);

		float h = 0.63f + (pisRow ? 0.65f : 0);
		blockTitle("TRIBUTAÇÃO FEDERAL (EXCETO CBS)", COLS[0], y, 5.11f, 0.63f);
		line(LEFT, y, RIGHT, y);
		field("IRRF", money(n.txt(cat(tf, "vRetIRRF"))), COLS[1], y, 5.10f, 0.63f);
		field("Contribuição Previdenciária - Retida", money(n.txt(cat(tf, "vRetCP"))), COLS[2], y, 5.11f, 0.63f);
		field("Contribuições Sociais - Retidas", money(n.txt(cat(tf, "vRetCSLL"))), COLS[3], y, 5.08f, 0.63f);
		if (pisRow) {
			float r = y + 0.63f;
			field("PIS - Débito Apuração Própria", money(n.txt(cat(tf, "piscofins", "vPis"))), COLS[0], r, 5.11f, 0.65f);
			field("COFINS - Débito Apuração Própria", money(n.txt(cat(tf, "piscofins", "vCofins"))), COLS[1], r, 5.10f, 0.65f);
			field("Descrição Contrib. Sociais - Retidas",
					DESC_RET_PISCOFINS.getOrDefault(n.txt(cat(tf, "piscofins", "tpRetPisCofins")),
							n.txt(cat(tf, "piscofins", "tpRetPisCofins"))),
					COLS[2], r, 10.19f, 0.65f);
		}
		return y + h;
	}

	private float drawIbsCbs(float y) throws Exception {
		String[] ib = {"infNFSe", "IBSCBS"};
		String[] dpsIb = {"infNFSe", "DPS", "infDPS", "IBSCBS"};
		float h = 0.63f + 0.64f + 0.65f + 0.66f;
		blockTitle("TRIBUTAÇÃO IBS / CBS", COLS[0], y, 5.11f, 0.63f);
		line(LEFT, y, RIGHT, y);
		field("CST / cClassTrib",
				n.txt(cat(dpsIb, "valores", "trib", "gIBSCBS", "CST")) + slash(n.txt(cat(dpsIb, "valores", "trib", "gIBSCBS", "cClassTrib"))),
				COLS[1], y, 5.10f, 0.63f);
		field("Indicador de Operação / Código IBGE Incidência / Município Incidência / Sigla UF",
				n.txt(cat(dpsIb, "cIndOp")) + " / " + n.txt(cat(ib, "cLocalidadeIncid")) + " / "
						+ n.txt(cat(ib, "xLocalidadeIncid")) + " / " + ufFromIbge(n.txt(cat(ib, "cLocalidadeIncid"))),
				COLS[2], y, 10.19f, 0.63f);

		float r = y + 0.63f;
		field("Exclusões e Reduções da Base de Cálculo", money(somaExclusoes()), COLS[0], r, 5.11f, 0.64f);
		field("Base de Cálculo Após Exclusões e Reduções", money(n.txt(cat(ib, "valores", "vBC"))), COLS[1], r, 5.10f, 0.64f);
		field("Red. Alíquota IBS / Red. Alíquota CBS",
				pct(n.txt(cat(ib, "valores", "uf", "pRedAliqUF"))) + " / " + pct(n.txt(cat(ib, "valores", "mun", "pRedAliqMun")))
						+ " / " + pct(n.txt(cat(ib, "valores", "fed", "pRedAliqCBS"))),
				COLS[2], r, 5.11f, 0.64f);
		field("Alíquota - IBS UF / IBS Mun",
				pct(n.txt(cat(ib, "valores", "uf", "pIBSUF"))) + " / " + pct(n.txt(cat(ib, "valores", "mun", "pIBSMun"))),
				COLS[3], r, 5.08f, 0.64f);

		r += 0.64f;
		field("Alíq. Efetiva Municipal - IBS", pct(n.txt(cat(ib, "valores", "mun", "pAliqEfetMun"))), COLS[0], r, 5.11f, 0.65f);
		field("Valor Apurado Municipal - IBS", money(n.txt(cat(ib, "totCIBS", "gIBS", "gIBSMunTot", "vIBSMun"))), COLS[1], r, 5.10f, 0.65f);
		field("Alíq. Efetiva Estadual - IBS", pct(n.txt(cat(ib, "valores", "uf", "pAliqEfetUF"))), COLS[2], r, 5.11f, 0.65f);
		field("Valor Apurado Estadual - IBS", money(n.txt(cat(ib, "totCIBS", "gIBS", "gIBSUFTot", "vIBSUF"))), COLS[3], r, 5.08f, 0.65f);

		r += 0.65f;
		field("Valor Total Apurado - IBS", money(n.txt(cat(ib, "totCIBS", "gIBS", "vIBSTot"))), COLS[0], r, 5.11f, 0.66f);
		field("Alíquota - CBS", pct(n.txt(cat(ib, "valores", "fed", "pCBS"))), COLS[1], r, 5.10f, 0.66f);
		field("Alíquota Efetiva - CBS", pct(n.txt(cat(ib, "valores", "fed", "pAliqEfetCBS"))), COLS[2], r, 5.11f, 0.66f);
		field("Valor Total Apurado - CBS", money(n.txt(cat(ib, "totCIBS", "gCBS", "vCBS"))), COLS[3], r, 5.08f, 0.66f);
		return y + h;
	}

	private float drawValorTotal(float y) throws Exception {
		float h = 0.67f + 0.69f;
		blockTitle("VALOR TOTAL DA NFS-e", COLS[0], y, 5.11f, 0.67f);
		line(LEFT, y, RIGHT, y);
		field("Valor da Operação / Serviço", money(n.txt("infNFSe", "DPS", "infDPS", "valores", "vServPrest", "vServ")),
				COLS[1], y, 5.10f, 0.67f);
		field("Desconto Incondicionado", money(n.txt("infNFSe", "DPS", "infDPS", "valores", "vDescCondIncond", "vDescIncond")),
				COLS[2], y, 5.11f, 0.67f);
		field("Desconto Condicionado", money(n.txt("infNFSe", "DPS", "infDPS", "valores", "vDescCondIncond", "vDescCond")),
				COLS[3], y, 5.08f, 0.67f);
		float r = y + 0.67f;
		field("Total das Retenções (ISSQN / Federais)", money(n.txt("infNFSe", "valores", "vTotalRet")), COLS[0], r, 5.11f, 0.69f);
		field("Valor Líquido da NFS-e", money(n.txt("infNFSe", "valores", "vLiq")), COLS[1], r, 5.10f, 0.69f);
		field("Total do IBS/CBS", money(somaIbsCbs()), COLS[2], r, 5.11f, 0.69f);
		// "Valor Líquido da NFS-e + IBS/CBS" com sombreamento (item 2.2.3) -
		// a célula vai até a borda direita do corpo
		shade(COLS[3], r, RIGHT - COLS[3], 0.69f);
		field("Valor Líquido da NFS-e + IBS/CBS", money(n.txt("infNFSe", "IBSCBS", "totCIBS", "vTotNF")),
				COLS[3], r, RIGHT - COLS[3], 0.69f);
		return y + h;
	}

	private void drawInfoComplementares(float y) throws Exception {
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
		text(trunc(totais, BODY_W - 0.20f, fontContent, 7), LEFT + 0.10f, ty, fontContent, 7);
	}

	/**
	 * Canhoto (item 2.1.13, Nota 11 - opcional). No Anexo I é uma caixa
	 * destacada do corpo, recuada 0,10 da borda da página (0,30-20,70),
	 * com três células e labels em caixa alta 7pt negrito.
	 */
	private void drawCanhoto() throws Exception {
		float y = CANHOTO_TOP, h = CANHOTO_H;
		rect(0.30f, y, 20.40f, h);
		cs.stroke();
		line(5.41f, y, 5.41f, y + h);
		line(10.51f, y, 10.51f, y + h);
		text("DATA CIENTIFICAÇÃO:", 0.40f, y + 0.28f, fontBold, 7);
		text("IDENTIFICAÇÃO E ASSINATURA", 5.51f, y + 0.28f, fontBold, 7);
		text("Nº NFS-e / CHAVE NFS-e", 10.61f, y + 0.28f, fontBold, 7);
		// id da NFS-e sem o prefixo "NFS" (tabela 2.4.5). Ex.: nnn / nnn
		text(trunc(n.txt("infNFSe", "nNFSe") + " / " + n.chaveAcesso(), 10.0f, fontContent, 7),
				10.61f, y + h - 0.12f, fontContent, 7);
	}

	private void watermark(String txt) throws Exception {
		cs.saveGraphicsState();
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
	private void field(String lbl, String value, float x, float y, float w, float h) throws Exception {
		label(lbl, x + 0.10f, y + 0.25f);
		String v = (value == null || value.trim().isEmpty()) ? "-" : value.trim();
		text(trunc(v, w - 0.20f, fontContent, 7), x + 0.10f, y + h - 0.12f, fontContent, 7);
	}

	/** Campo com label 7pt caixa alta (item 2.4.2 - identificação e Anexo I).
	 *  O label deve vir já em caixa alta, preservando a grafia "NFS-e". */
	private void fieldCaps(String lbl, String value, float x, float y, float w, float h) throws Exception {
		text(trunc(lbl, w - 0.2f, fontBold, 7), x + 0.10f, y + 0.28f, fontBold, 7);
		String v = (value == null || value.trim().isEmpty()) ? "-" : value.trim();
		text(trunc(v, w - 0.20f, fontContent, 7), x + 0.10f, y + h - 0.14f, fontContent, 7);
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

	/** Remove caracteres fora do WinAnsi suportado pelas fontes padrão. */
	private static String sanitize(String s) {
		StringBuilder sb = new StringBuilder(s.length());
		for (char c : s.toCharArray()) {
			if (c == '\n' || c == '\r' || c == '\t') {
				sb.append(' ');
			} else if (c >= 32 && c <= 255) {
				sb.append(c);
			} else {
				sb.append('?');
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

	private String paisPrestacao() {
		String p = n.txt("infNFSe", "DPS", "infDPS", "serv", "locPrest", "cPaisPrestacao");
		return p.isEmpty() ? "BR" : p;
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
		try {
			BigDecimal ibs = new BigDecimal(firstNonEmpty(n.txt("infNFSe", "IBSCBS", "totCIBS", "gIBS", "vIBSTot"), "0"));
			BigDecimal cbs = new BigDecimal(firstNonEmpty(n.txt("infNFSe", "IBSCBS", "totCIBS", "gCBS", "vCBS"), "0"));
			return ibs.add(cbs).toPlainString();
		} catch (NumberFormatException e) {
			return "";
		}
	}

	private String totaisAproximados() {
		String fed = firstNonEmpty(n.txt("infNFSe", "DPS", "infDPS", "valores", "trib", "totTrib", "vTotTrib", "vTotTribFed"),
				n.txt("infNFSe", "DPS", "infDPS", "valores", "trib", "totTrib", "pTotTrib", "pTotTribFed"));
		String est = firstNonEmpty(n.txt("infNFSe", "DPS", "infDPS", "valores", "trib", "totTrib", "vTotTrib", "vTotTribEst"),
				n.txt("infNFSe", "DPS", "infDPS", "valores", "trib", "totTrib", "pTotTrib", "pTotTribEst"));
		String mun = firstNonEmpty(n.txt("infNFSe", "DPS", "infDPS", "valores", "trib", "totTrib", "vTotTrib", "vTotTribMun"),
				n.txt("infNFSe", "DPS", "infDPS", "valores", "trib", "totTrib", "pTotTrib", "pTotTribMun"));
		return "Totais Aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: " + money(fed)
				+ "; Estaduais: " + money(est) + "; Municipais: " + money(mun);
	}

	private static void appendInfo(StringBuilder sb, String prefix, String value) {
		if (value != null && !value.trim().isEmpty()) {
			if (sb.length() > 0) {
				sb.append(" | ");
			}
			sb.append(prefix).append(" ").append(value.trim());
		}
	}

	private static String slash(String v) {
		return (v == null || v.isEmpty()) ? "" : " / " + v;
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
	 * Nome do município a partir do código IBGE. Fase 1: devolve o próprio
	 * código (a NT pede o nome da tabela IBGE - fase 2 carregará a tabela
	 * completa como resource).
	 */
	private String municipioNome(String cMun) {
		if (cMun == null || cMun.isEmpty()) {
			return "";
		}
		// quando o município do tomador coincide com o local de incidência
		// IBS/CBS, o XML já traz o nome
		if (cMun.equals(n.txt("infNFSe", "IBSCBS", "cLocalidadeIncid"))) {
			String nome = n.txt("infNFSe", "IBSCBS", "xLocalidadeIncid");
			if (!nome.isEmpty()) {
				return nome;
			}
		}
		if (cMun.equals(n.txt("infNFSe", "cLocIncid"))) {
			String nome = n.txt("infNFSe", "xLocIncid");
			if (!nome.isEmpty()) {
				return nome;
			}
		}
		return cMun;
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

		DESC_TRIB_ISSQN.put("1", "Operação Tributável");
		DESC_TRIB_ISSQN.put("2", "Imunidade");
		DESC_TRIB_ISSQN.put("3", "Exportação de Serviço");
		DESC_TRIB_ISSQN.put("4", "Não Incidência");

		DESC_RET_ISSQN.put("1", "Não Retido");
		DESC_RET_ISSQN.put("2", "Retido pelo Tomador");
		DESC_RET_ISSQN.put("3", "Retido pelo Intermediário");

		DESC_RET_PISCOFINS.put("1", "Retido");
		DESC_RET_PISCOFINS.put("2", "Não Retido");
		DESC_RET_PISCOFINS.put("3", "PIS/COFINS/CSLL Retidos");
		DESC_RET_PISCOFINS.put("4", "PIS/COFINS/CSLL Não Retidos");
	}
}
