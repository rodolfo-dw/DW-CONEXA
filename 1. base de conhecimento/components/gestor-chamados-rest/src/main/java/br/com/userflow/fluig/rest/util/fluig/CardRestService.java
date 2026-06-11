package br.com.userflow.fluig.rest.util.fluig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fluig.sdk.exception.ApplicationKeyNotFoundException;

public class CardRestService extends RestService {
	public CardRestService() throws Exception, ApplicationKeyNotFoundException {
		super();
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
	public CardRestService(Long tenantId) throws Exception, ApplicationKeyNotFoundException {
		super(tenantId);
	}

	public String createCard(int parentId, List<CardDataParams> cardData) throws Exception {
		String endpoint = fluigURL + "/ecm-forms/api/v2/cardindex/" + parentId + "/cards";

		Map<String, Object> payloadMap = new HashMap<>();
		payloadMap.put("values", cardData);

		return doPost(endpoint, payloadMap);
	}

	public String editCard(int parentId, int documentId, List<CardDataParams> cardData) throws Exception {
		String endpoint = fluigURL + "/ecm-forms/api/v2/cardindex/" + parentId + "/cards/" + documentId;

		Map<String, Object> payloadMap = new HashMap<>();
		payloadMap.put("values", cardData);

		return doPut(endpoint, payloadMap);
	}

	public Map<String, Object> getCard(int parentId, int documentId) throws Exception {
		String endpoint = fluigURL + "/ecm-forms/api/v2/cardindex/" + parentId + "/cards/" + documentId;
		String jsonResponse = doGet(endpoint);

		return mapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {
		});
	}

	public static class CardDataParams {
		public String fieldId;
		public String value;

		public CardDataParams() {
		}

		public CardDataParams(String field, String value) {
			this.fieldId = field;
			this.value = value;
		}

		// Getters necessários para o Jackson converter para JSON
		public String getFieldId() {
			return fieldId;
		}

		public String getValue() {
			return value;
		}

	}
}
