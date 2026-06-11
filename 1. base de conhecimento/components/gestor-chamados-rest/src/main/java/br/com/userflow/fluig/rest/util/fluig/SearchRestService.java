package br.com.userflow.fluig.rest.util.fluig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fluig.sdk.exception.ApplicationKeyNotFoundException;

public class SearchRestService extends RestService {
	public SearchRestService() throws Exception, ApplicationKeyNotFoundException {
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
	public SearchRestService(Long tenantId) throws Exception, ApplicationKeyNotFoundException {
		super(tenantId);
	}

	public Map<String, Object> searchGlobal() throws Exception {
		String endpoint = fluigURL + "/api/public/search/advanced";

		Map<String, Object> payload = new HashMap<>();
		payload.put("searchType", "GLOBAL");
		payload.put("pattern", "");
		payload.put("ordering", "RELEVANT");
		payload.put("limit", "15");
		payload.put("offset", "0");
		payload.put("contentSearch", "false");
		payload.put("documentTypes", new ArrayList<String>(Arrays.asList("FILEDOCUMENT")));
		payload.put("folderToSearch", "0");

		String jsonResponse = doPost(endpoint, payload);

		return mapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {
		});
	}
}
