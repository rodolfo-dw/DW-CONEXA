package br.com.datawer.danfse;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 * De-para código IBGE -> nome do município (tabela 2.4.5: "utilizar a
 * descrição destes códigos").
 * O snapshot IBGE embutido no JAR é carregado uma vez e torna a geração
 * determinística, sem depender de rede. A API pública fica como contingência
 * apenas se o recurso local não estiver disponível.
 *
 * Nunca propaga erro: sem o de-para, campos individuais de nome de município
 * seguem a regra geral e são impressos com traço.
 */
final class IbgeMunicipios {

	private static final String URL_IBGE = "https://servicodados.ibge.gov.br/api/v1/localidades/municipios";
	private static final String RECURSO_LOCAL = "/danfse/municipios-ibge.txt";

	private static volatile Map<String, String> cache = new HashMap<>();

	private IbgeMunicipios() {
	}

	/**
	 * Inicializa o de-para uma única vez, priorizando o snapshot embutido.
	 */
	static synchronized void atualizar() {
		if (!cache.isEmpty()) {
			return;
		}
		try {
			cache = carregarLocal();
		} catch (Exception eLocal) {
			System.out.println("IbgeMunicipios: falha ao ler " + RECURSO_LOCAL + " - " + eLocal.getMessage());
			try {
				cache = carregarDaApi();
			} catch (Exception e) {
				System.out.println("IbgeMunicipios: falha ao consultar a API do IBGE - " + e.getMessage());
			}
		}
	}

	/** Nome do município ou "" quando o de-para não estiver disponível. */
	static String nome(String codigoIbge) {
		String nome = cache.get(codigoIbge);
		return nome == null ? "" : nome;
	}

	private static Map<String, String> carregarDaApi() throws Exception {
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) new URL(URL_IBGE).openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(30000);

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

				JsonArray municipios = Json.createReader(new StringReader(sb.toString())).readArray();
				Map<String, String> map = new HashMap<>();
				for (JsonValue v : municipios) {
					JsonObject m = (JsonObject) v;
					map.put(m.getJsonNumber("id").toString(), m.getString("nome"));
				}
				if (map.isEmpty()) {
					throw new Exception("API retornou lista vazia de municipios");
				}
				return map;
			}
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private static Map<String, String> carregarLocal() throws Exception {
		try (InputStream is = IbgeMunicipios.class.getResourceAsStream(RECURSO_LOCAL)) {
			if (is == null) {
				throw new Exception("recurso nao encontrado no JAR");
			}
			Map<String, String> map = new HashMap<>();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				String line;
				while ((line = br.readLine()) != null) {
					int sep = line.indexOf(';');
					if (sep > 0) {
						map.put(line.substring(0, sep), line.substring(sep + 1));
					}
				}
			}
			if (map.isEmpty()) {
				throw new Exception("recurso vazio");
			}
			return map;
		}
	}
}
