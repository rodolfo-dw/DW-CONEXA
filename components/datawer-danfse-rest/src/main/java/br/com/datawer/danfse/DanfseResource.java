package br.com.datawer.danfse;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Geração do DANFSe v2.0 (NT 008/2026) a partir do XML da NFS-e Nacional.
 *
 * Recebe o XML no corpo e devolve o PDF em base64 - quem chamou decide se
 * exibe ou publica no GED.
 */
@Path("/danfse")
public class DanfseResource {

	/**
	 * @param xml      XML completo da NFS-e (raiz &lt;NFSe&gt;)
	 * @param situacao opcional: CANCELADA ou SUBSTITUIDA para imprimir a marca
	 *                 d'água. O XML da NFS-e não carrega o cancelamento (que é
	 *                 um evento separado), por isso a informação vem por
	 *                 parâmetro.
	 * @param debug    inclui no JSON a auditoria de origem e formatação de cada
	 *                 campo impresso, sem alterar o PDF
	 */
	@POST
	@Path("/base64")
	@Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.TEXT_PLAIN})
	@Produces(MediaType.APPLICATION_JSON)
	public Response gerarBase64(String xml, @QueryParam("situacao") String situacao,
			@QueryParam("debug") boolean debug) {
		try {
			if (xml == null || xml.trim().isEmpty()) {
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(error("Corpo da requisição vazio. Envie o XML da NFS-e.")).build();
			}

			NfseXml nfse = NfseXml.parse(xml);
			DanfseGenerationResult generation = new DanfseGenerator().generateWithAudit(nfse, situacao);
			byte[] pdf = generation.getPdf();

			Map<String, Object> result = new LinkedHashMap<>();
			result.put("success", true);
			result.put("chaveAcesso", nfse.chaveAcesso());
			result.put("numeroNfse", nfse.txt("infNFSe", "nNFSe"));
			result.put("emitente", nfse.txt("infNFSe", "emit", "xNome"));
			result.put("situacao", situacao == null ? "NORMAL" : situacao.toUpperCase());
			result.put("fileName", "DANFSe-" + nfse.txt("infNFSe", "nNFSe") + ".pdf");
			result.put("contentType", "application/pdf");
			result.put("pdfBase64", Base64.getEncoder().encodeToString(pdf));
			if (debug) {
				List<Map<String, Object>> audit = new ArrayList<>();
				for (DanfseAuditEntry entry : generation.getAudit()) {
					audit.add(entry.toMap());
				}
				result.put("audit", audit);
			}

			System.out.println(String.format("DANFSe gerado: nNFSe=%s chave=%s (%d bytes)",
					nfse.txt("infNFSe", "nNFSe"), nfse.chaveAcesso(), pdf.length));
			return Response.ok(result).build();

		} catch (IllegalArgumentException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(error(e.getMessage())).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(error("Falha ao gerar o DANFSe: " + e.getMessage())).build();
		}
	}

	private static Map<String, Object> error(String message) {
		Map<String, Object> err = new LinkedHashMap<>();
		err.put("success", false);
		err.put("message", message);
		return err;
	}
}
