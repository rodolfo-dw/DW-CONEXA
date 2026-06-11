package br.com.datawer.danfse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

class DanfseGeneratorTest {

	private static final String QR_URL = "https://www.nfse.gov.br/ConsultaPublica/?tpc=1&chave="
			+ "33045572227092748000118000000005344426065793314288";

	private static DanfseGenerationResult generation;
	private static String sourceXml;

	@BeforeAll
	static void generate() throws Exception {
		try (InputStream in = DanfseGeneratorTest.class.getResourceAsStream("/nfse-53444.xml")) {
			assertNotNull(in);
			sourceXml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			generation = generate(sourceXml);
		}
	}

	@Test
	void deveResolverCamposEValoresDoXmlConformeNt008() {
		assertField("SERVIÇO PRESTADO", "Local da Prestação / Sigla UF / País", "Rio de Janeiro / RJ / BR");
		assertField("PRESTADOR / FORNECEDOR", "Município / Sigla UF", "Rio de Janeiro / RJ");
		assertField("TOMADOR / ADQUIRENTE", "Município / Sigla UF", "Medianeira / PR");
		assertField("TRIBUTAÇÃO MUNICIPAL (ISSQN)", "Município / Sigla UF / País da Incidência do ISSQN", "Rio de Janeiro / RJ / BR");
		assertField("TRIBUTAÇÃO IBS / CBS", "Indicador de Operação / Código IBGE Incidência / Município Incidência / Sigla UF", "100301 / 4115804 / Medianeira / PR");
		assertField("SERVIÇO PRESTADO", "Código da NBS", "1.2301.22.00");

		assertField("VALOR TOTAL DA NFS-e", "Valor da Operação / Serviço", "R$ 10.321,73");
		assertField("VALOR TOTAL DA NFS-e", "Total das Retenções (ISSQN / Federais)", "R$ 634,78");
		assertField("TRIBUTAÇÃO MUNICIPAL (ISSQN)", "ISSQN Apurado", "R$ 516,09");
		assertField("VALOR TOTAL DA NFS-e", "Total do IBS/CBS", "R$ 94,29");
		assertField("VALOR TOTAL DA NFS-e", "Valor Líquido da NFS-e", "R$ 9.686,95");
		assertField("VALOR TOTAL DA NFS-e", "Valor Líquido da NFS-e + IBS/CBS", "R$ 9.686,95");

		assertField("DADOS DA NFS-e", "DATA E HORA DA EMISSÃO DA NFS-e", "08/06/2026 15:48:42");
		assertField("DADOS DA NFS-e", "DATA E HORA DA EMISSÃO DA DPS", "08/06/2026 15:47:24");
	}

	@Test
	void deveUsarTracoEFrasesEspeciaisSemInventarDados() {
		DanfseAuditEntry prestadorIm = field("PRESTADOR / FORNECEDOR", "Indicador Municipal (Inscrição)");
		assertEquals("-", prestadorIm.getValorImpresso());
		assertTrue(prestadorIm.isUsouTracoPorAusencia());

		DanfseAuditEntry tomadorFone = field("TOMADOR / ADQUIRENTE", "Telefone");
		assertEquals("-", tomadorFone.getValorImpresso());
		assertTrue(tomadorFone.isUsouTracoPorAusencia());

		DanfseAuditEntry destinatario = field("DESTINATÁRIO DA OPERAÇÃO", "DESTINATÁRIO DA OPERAÇÃO");
		assertEquals("O DESTINATÁRIO É O PRÓPRIO TOMADOR/ADQUIRENTE DA OPERAÇÃO", destinatario.getValorImpresso());
		assertTrue(destinatario.isUsouFraseEspecialDeBloco());

		DanfseAuditEntry intermediario = field("INTERMEDIÁRIO DA OPERAÇÃO", "INTERMEDIÁRIO DA OPERAÇÃO");
		assertEquals("INTERMEDIÁRIO DA OPERAÇÃO NÃO IDENTIFICADO NA NFS-e", intermediario.getValorImpresso());
		assertTrue(intermediario.isUsouFraseEspecialDeBloco());

		for (DanfseAuditEntry entry : generation.getAudit()) {
			assertFalse(entry.getValorImpresso().isEmpty(), entry.getBloco() + " / " + entry.getLabelImpresso());
			assertFalse(entry.getCaminhoXmlOrigem().isEmpty(), entry.getBloco() + " / " + entry.getLabelImpresso());
			assertFalse(entry.getRegraFormatacaoAplicada().isEmpty(), entry.getBloco() + " / " + entry.getLabelImpresso());
		}
	}

	@Test
	void deveOrganizarInformacoesComplementaresSemAcrescentarNbs() {
		DanfseAuditEntry info = field("INFORMAÇÕES COMPLEMENTARES", "Informações Complementares");
		assertTrue(info.getValorImpresso().startsWith("Inf. Cont.: Servicos prestados Maio/2026"));
		assertTrue(info.getValorImpresso().contains("Totais Aproximados dos Tributos cfe. Lei nº 12.741/2012"));
		assertFalse(info.getValorImpresso().contains("1.2301.22.00"));
	}

	@Test
	void deveGerarPdfA4DeUmaPaginaComQrCodeOficial() throws Exception {
		try (PDDocument pdf = PDDocument.load(generation.getPdf())) {
			assertEquals(1, pdf.getNumberOfPages());
			PDPage page = pdf.getPage(0);
			assertEquals(595.28f, page.getMediaBox().getWidth(), 0.1f);
			assertEquals(841.89f, page.getMediaBox().getHeight(), 0.1f);

			String text = new PDFTextStripper().getText(pdf);
			assertTrue(text.contains("DANFSe v2.0"));
			assertTrue(text.contains("O DESTINATÁRIO É O PRÓPRIO TOMADOR/ADQUIRENTE DA OPERAÇÃO"));
			assertTrue(text.contains("INTERMEDIÁRIO DA OPERAÇÃO NÃO IDENTIFICADO NA NFS-e"));
			assertEquals(QR_URL, decodeQrCode(page.getResources()));
		}
	}

	@Test
	void deveAuditarUrlDoQrCodeComChaveSemPrefixoNfs() {
		DanfseAuditEntry qr = field("DADOS DA NFS-e", "QR Code");
		assertEquals(QR_URL, qr.getValorImpresso());
		assertFalse(qr.getValorImpresso().contains("chave=NFS"));
	}

	@Test
	void devePriorizarTotalizadoraECalcularSomenteNaAusencia() throws Exception {
		String withCalculatedValues = sourceXml
				.replace("<vBC>10321.73</vBC>", "<vBC>10321.73</vBC><vCalcDR>20.00</vCalcDR>")
				.replace("<vCalcReeRepRes>0.00</vCalcReeRepRes>", "<vCalcReeRepRes>1.00</vCalcReeRepRes>");
		DanfseGenerationResult calculated = generate(withCalculatedValues);
		assertEquals("R$ 21,00", field(calculated, "TRIBUTAÇÃO MUNICIPAL (ISSQN)", "Total Deduções/Reduções").getValorImpresso());

		String withTotalizer = withCalculatedValues.replace("</vServPrest>",
				"</vServPrest><vDedRed><vDR>30.00</vDR></vDedRed>");
		DanfseGenerationResult totalized = generate(withTotalizer);
		assertEquals("R$ 30,00", field(totalized, "TRIBUTAÇÃO MUNICIPAL (ISSQN)", "Total Deduções/Reduções").getValorImpresso());
	}

	@Test
	void deveFormatarTotaisAproximadosPercentuaisEFraseIssNaoSujeito() throws Exception {
		String xml = sourceXml
				.replace("<vTotTrib><vTotTribFed>634.78</vTotTribFed><vTotTribEst>0.00</vTotTribEst><vTotTribMun>516.09</vTotTribMun></vTotTrib>",
						"<pTotTrib><pTotTribFed>6.15</pTotTribFed><pTotTribEst>0.00</pTotTribEst><pTotTribMun>5.00</pTotTribMun></pTotTrib>")
				.replace("<tribISSQN>1</tribISSQN>", "<tribISSQN>4</tribISSQN>");
		DanfseGenerationResult result = generate(xml);

		DanfseAuditEntry info = field(result, "INFORMAÇÕES COMPLEMENTARES", "Informações Complementares");
		assertTrue(info.getValorImpresso().contains("Federais: 6,15%; Estaduais: 0,00%; Municipais: 5,00%"));

		DanfseAuditEntry iss = field(result, "TRIBUTAÇÃO MUNICIPAL (ISSQN)", "TRIBUTAÇÃO MUNICIPAL (ISSQN)");
		assertEquals("TRIBUTAÇÃO MUNICIPAL (ISSQN) - OPERAÇÃO NÃO SUJEITA AO ISSQN", iss.getValorImpresso());
		assertTrue(iss.isUsouFraseEspecialDeBloco());
	}

	private static void assertField(String block, String label, String expected) {
		assertEquals(expected, field(block, label).getValorImpresso());
	}

	private static DanfseAuditEntry field(String block, String label) {
		return field(generation, block, label);
	}

	private static DanfseAuditEntry field(DanfseGenerationResult result, String block, String label) {
		return result.getAudit().stream()
				.filter(entry -> block.equals(entry.getBloco()) && label.equals(entry.getLabelImpresso()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Campo não auditado: " + block + " / " + label));
	}

	private static DanfseGenerationResult generate(String xml) throws Exception {
		return new DanfseGenerator().generateWithAudit(NfseXml.parse(xml), null);
	}

	private static String decodeQrCode(PDResources resources) throws Exception {
		for (org.apache.pdfbox.cos.COSName name : resources.getXObjectNames()) {
			PDXObject object = resources.getXObject(name);
			if (object instanceof PDImageXObject) {
				BufferedImage image = ((PDImageXObject) object).getImage();
				BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
				try {
					Result result = new MultiFormatReader().decode(bitmap);
					return result.getText();
				} catch (NotFoundException ignored) {
					// A outra imagem da página é a logomarca.
				}
			}
		}
		throw new AssertionError("QR Code não encontrado no PDF");
	}
}
