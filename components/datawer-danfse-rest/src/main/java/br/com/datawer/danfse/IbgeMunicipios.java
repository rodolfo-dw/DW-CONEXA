package br.com.datawer.danfse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 * De-para código IBGE -> nome do município (tabela 2.4.5: "utilizar a
 * descrição destes códigos").
 *
 * A fonte é o dataset sincronizado ds_dw_api_ibge_municipios: o JSON da API
 * do IBGE vem quebrado em linhas de 2.000 caracteres (menor limite de coluna
 * TEXT entre os bancos do Fluig - Oracle 4.000 bytes), ordenadas pela coluna
 * CODIGO (0001, 0002...) com a quantidade na linha TOTAL. Aqui as partes são
 * concatenadas e o JSON remontado. A consulta usa a API pública de datasets
 * do Fluig (mesmo endpoint do DatasetRestService da base de conhecimento),
 * com autenticação Basic.
 *
 * Configuração via system property (ou variável de ambiente):
 *   datawer.fluig.url  (DATAWER_FLUIG_URL)  - default http://localhost:8080
 *   datawer.fluig.user (DATAWER_FLUIG_USER) - usuário de integração
 *   datawer.fluig.pass (DATAWER_FLUIG_PASS)
 *
 * O mapa fica em cache por 24h e, em caso de falha, mantém o último carregado
 * (ou segue vazio). Nunca propaga erro: sem o de-para, o DANFSe imprime o
 * próprio código IBGE, como antes.
 */
final class IbgeMunicipios {

	private static final String DATASET = "ds_dw_api_ibge_municipios";
	private static final long TTL_MS = 24L * 60 * 60 * 1000;

	private static volatile Map<String, String> cache = new HashMap<>();
	private static volatile long loadedAt = 0;

	private IbgeMunicipios() {
	}

	/** Nome do município ou "" quando o de-para não estiver disponível. */
	static String nome(String codigoIbge) {
		if (cache.isEmpty() || System.currentTimeMillis() - loadedAt > TTL_MS) {
			synchronized (IbgeMunicipios.class) {
				if (cache.isEmpty() || System.currentTimeMillis() - loadedAt > TTL_MS) {
					try {
						cache = load();
					} catch (Exception e) {
						System.out.println("IbgeMunicipios: falha ao consultar " + DATASET + " - " + e.getMessage());
					}
					// marca a tentativa mesmo em falha, para não martelar o
					// Fluig a cada nota gerada
					loadedAt = System.currentTimeMillis();
				}
			}
		}
		String nome = cache.get(codigoIbge);
		return nome == null ? "" : nome;
	}

	private static Map<String, String> load() throws Exception {
		String response = getDataset();

		JsonArray values = Json.createReader(new StringReader(response)).readObject()
				.getJsonObject("content").getJsonArray("values");
		if (values == null || values.isEmpty()) {
			throw new Exception("dataset sem linhas - sincronizacao ainda nao executada?");
		}

		// linhas: 0001..NNNN com pedaços de 2.000 chars do JSON + TOTAL com a quantidade
		Map<String, String> partes = new HashMap<>();
		int total = -1;
		for (JsonValue v : values) {
			JsonObject row = (JsonObject) v;
			if (row.containsKey("ERROR")) {
				throw new Exception(row.getString("MESSAGE", "dataset retornou ERROR"));
			}
			String codigo = row.getString("CODIGO", "");
			if ("TOTAL".equals(codigo)) {
				total = Integer.parseInt(row.getString("JSON", "0").trim());
			} else {
				partes.put(codigo, row.getString("JSON", ""));
			}
		}
		if (total <= 0) {
			throw new Exception("linha TOTAL nao encontrada - sincronizacao ainda nao executada?");
		}

		StringBuilder json = new StringBuilder(total * 2000);
		for (int i = 1; i <= total; i++) {
			String parte = partes.get(String.format("%04d", i));
			if (parte == null) {
				throw new Exception("parte " + i + " de " + total + " ausente no dataset");
			}
			json.append(parte);
		}

		JsonArray municipios = Json.createReader(new StringReader(json.toString())).readArray();
		Map<String, String> map = new HashMap<>();
		for (JsonValue v : municipios) {
			JsonObject m = (JsonObject) v;
			map.put(m.getJsonNumber("id").toString(), m.getString("nome"));
		}
		if (map.isEmpty()) {
			throw new Exception("JSON do dataset sem municipios");
		}
		return map;
	}

	/** POST em /api/public/ecm/dataset/datasets pedindo o dataset completo. */
	private static String getDataset() throws Exception {
		String url = cfg("datawer.fluig.url", "DATAWER_FLUIG_URL", "http://localhost:8080");
		String user = cfg("datawer.fluig.user", "DATAWER_FLUIG_USER", "");
		String pass = cfg("datawer.fluig.pass", "DATAWER_FLUIG_PASS", "");

		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) new URL(url + "/api/public/ecm/dataset/datasets").openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Accept", "application/json");
			if (!user.isEmpty()) {
				conn.setRequestProperty("Authorization", "Basic " + Base64.getEncoder()
						.encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8)));
			}
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(30000);
			conn.setDoOutput(true);

			String body = "{\"name\":\"" + DATASET + "\",\"fields\":null,\"constraints\":null,\"order\":null}";
			try (OutputStream os = conn.getOutputStream()) {
				os.write(body.getBytes(StandardCharsets.UTF_8));
			}

			int code = conn.getResponseCode();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(
					code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
					StandardCharsets.UTF_8))) {
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
				if (code < 200 || code >= 300) {
					throw new Exception("HTTP " + code + ": " + sb);
				}
				return sb.toString();
			}
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private static String cfg(String prop, String env, String def) {
		String v = System.getProperty(prop);
		if (v == null || v.trim().isEmpty()) {
			v = System.getenv(env);
		}
		return (v == null || v.trim().isEmpty()) ? def : v.trim();
	}
}
