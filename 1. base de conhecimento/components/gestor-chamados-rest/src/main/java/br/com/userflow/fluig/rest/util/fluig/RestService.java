package br.com.userflow.fluig.rest.util.fluig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.URL;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fluig.customappkey.Keyring;
import com.fluig.sdk.api.customappkey.KeyVO;
import com.fluig.sdk.exception.ApplicationKeyNotFoundException;

import br.com.userflow.fluig.rest.util.RestConstant;
import br.com.userflow.fluig.rest.util.TenantContext;

public class RestService {

	protected final String fluigURL;
	protected final OAuthConsumer consumer;
	protected final ObjectMapper mapper;
	protected final Long tenantId;

	protected RestService() throws Exception, ApplicationKeyNotFoundException {
		this.tenantId = TenantContext.getTenantId();
		if (this.tenantId == null) {
			throw new IllegalStateException("Tenant is not defined in context. Verify the url path sent.");
		}

		KeyVO key = Keyring.getKeys(tenantId, RestConstant.APP_KEY);

		this.fluigURL = key.getDomainUrl();
		this.consumer = config(key);
		this.mapper = new ObjectMapper();
	}

	/**
	 * Construtor alternativo que aceita um tenantId explícito. Este construtor é
	 * mantido por compatibilidade com código legado.
	 * 
	 * @param tenantId o ID do tenant
	 * @throws Exception                       se houver erro ao recuperar as chaves
	 *                                         do Keyring
	 * @throws ApplicationKeyNotFoundException se a chave da aplicação não for
	 *                                         encontrada
	 */
	@Deprecated
	protected RestService(Long tenantId) throws Exception, ApplicationKeyNotFoundException {
		KeyVO key = Keyring.getKeys(tenantId, RestConstant.APP_KEY);

		this.fluigURL = key.getDomainUrl();
		this.tenantId = key.getTenantId();
		this.consumer = config(key);
		this.mapper = new ObjectMapper();
	}

	protected String doPut(String endpointUrl, Map<String, Object> payloadMap) throws Exception {
		HttpURLConnection connection = null;

		try {
			URL url = new URL(endpointUrl);
			connection = (HttpURLConnection) url.openConnection();

			connection.setRequestMethod("PUT");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Accept", "application/json");
			connection.setDoOutput(true);

			consumer.sign(connection);

			// TODO: Refactor
			ObjectMapper mapper = new ObjectMapper();
			String jsonBody = mapper.writeValueAsString(payloadMap);
			/*
			 * System.out.println(String.format("doPut: %d - %s: data: %s", 200,
			 * endpointUrl, jsonBody));
			 */

			try (OutputStream os = connection.getOutputStream()) {
				byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
				os.write(input, 0, input.length);
			}

			return readResponse(connection);

		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	protected String doPost(String endpointUrl, Map<String, Object> payloadMap) throws Exception {
		HttpURLConnection connection = null;

		try {
			URL url = new URL(endpointUrl);
			connection = (HttpURLConnection) url.openConnection();

			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Accept", "application/json");
			connection.setDoOutput(true);

			consumer.sign(connection);

			// TODO: Refactor
			ObjectMapper mapper = new ObjectMapper();
			String jsonBody = mapper.writeValueAsString(payloadMap);
			/*
			 * System.out.println(String.format("doPost: %d - %s: data: %s", 200,
			 * endpointUrl, jsonBody));
			 */

			try (OutputStream os = connection.getOutputStream()) {
				byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
				os.write(input, 0, input.length);
			}

			return readResponse(connection);

		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	protected String doGet(String endpointUrl) throws Exception {
		HttpURLConnection connection = null;
		try {
			URL url = new URL(endpointUrl);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");

			consumer.sign(connection);

			return readResponse(connection);

		} finally {
			if (connection != null)
				connection.disconnect();
		}
	}

	private String readResponse(HttpURLConnection connection) throws Exception {
		int responseCode = connection.getResponseCode();
		if (responseCode >= 200 && responseCode < 300) {
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
				StringBuilder response = new StringBuilder();
				String line;
				while ((line = br.readLine()) != null)
					response.append(line.trim());
				/*
				 * System.out.println(String.format("readResponse: %d - %s: data: %s",
				 * responseCode, "Ok", response.toString()));
				 */
				return response.toString();
			}
		} else {
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
				StringBuilder error = new StringBuilder();
				String line;
				while ((line = br.readLine()) != null)
					error.append(line.trim());
				throw new Exception("Fluig API Error (" + responseCode + "): " + error.toString());
			}
		}
	}

	private List<Map<String, String>> transformToFormData(Map<String, Object> simpleMap) {
		List<Map<String, String>> formData = new ArrayList<>();

		for (Map.Entry<String, Object> entry : simpleMap.entrySet()) {
			Map<String, String> field = new HashMap<>();
			field.put("name", entry.getKey());
			field.put("value", entry.getValue() != null ? entry.getValue().toString() : "");
			formData.add(field);
		}
		return formData;
	}

	private OAuthConsumer config(KeyVO key) {
		OAuthConsumer consumer = new DefaultOAuthConsumer(key.getConsumerKey(), key.getConsumerSecret());
		consumer.setTokenWithSecret(key.getToken(), key.getTokenSecret());
		return consumer;
	}

}
