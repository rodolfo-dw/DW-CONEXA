package br.com.userflow.fluig.rest.security;

import java.util.UUID;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import br.com.userflow.fluig.rest.util.TenantContext;
import br.com.userflow.fluig.rest.util.fluig.DatasetRestService;

public class SessionManager {

	private final String form = "ds_uf_form_cadastro_usuario_externo";
	private final int folder;
	private final DatasetRestService datasetFluigService;

	public SessionManager() throws Exception {
		Long tenantId = TenantContext.getTenantId();
		datasetFluigService = new DatasetRestService();

		List<DatasetRestService.ConstraintParams> constraints = new ArrayList<>();
		constraints.add(new DatasetRestService.ConstraintParams("documentPK.companyId", Long.toString(tenantId)));
		constraints.add(new DatasetRestService.ConstraintParams("documentType", "4"));
		constraints.add(new DatasetRestService.ConstraintParams("activeVersion", "true"));
		constraints.add(new DatasetRestService.ConstraintParams("datasetName", form));

		DatasetRestService.SearchDatasetParams searchParams = new DatasetRestService.SearchDatasetParams();
		searchParams.name = "document";
		searchParams.fields = Arrays.asList("documentPK.documentId");
		searchParams.constraints = constraints;

		String jsonResponse = datasetFluigService.searchDataset(searchParams, true);
		List<Map<String, Object>> results = datasetFluigService.formatValuesDataset(jsonResponse);

		if (results.isEmpty()) {
			throw new Exception("Formulário de usuários não encontrado.");
		}

		Map<String, Object> document = results.get(0);
		folder = Integer.parseInt(String.valueOf(document.get("documentPK.documentId")));
	}

	public void invalidateSession(String token) throws Exception {
		TokenManager tokenManager = new TokenManager();
		tokenManager.invalidateToken(token);
	}

	public SessionData authenticate(String login, String password) throws Exception {
		Long tenantId = TenantContext.getTenantId();

		List<DatasetRestService.ConstraintParams> constraints = new ArrayList<>();

		constraints.add(new DatasetRestService.ConstraintParams("companyid", Long.toString(tenantId)));
		constraints.add(new DatasetRestService.ConstraintParams("metadata#active", "true"));
		constraints.add(new DatasetRestService.ConstraintParams("login", login));

		DatasetRestService.SearchDatasetParams searchParams = new DatasetRestService.SearchDatasetParams();
		searchParams.name = form;
		searchParams.fields = Arrays.asList("nome", "email", "senha", "login");
		searchParams.constraints = constraints;

		String jsonResponse = datasetFluigService.searchDataset(searchParams, true);
		List<Map<String, Object>> results = datasetFluigService.formatValuesDataset(jsonResponse);

		if (results.isEmpty()) {
			throw new Exception("Usuário não encontrado");
		}

		Map<String, Object> user = results.get(0);
		String userName = (String) user.get("nome");
		String userEmail = (String) user.get("email");
		String userPassword = (String) user.get("senha");

		if (!password.equals(userPassword)) {
			throw new Exception("Senha incorreta");
		}

		String token = UUID.randomUUID().toString();
		TokenManager tokenManager = new TokenManager();
		tokenManager.createSession(token, login, userEmail);

		return new SessionData(token, userName, userEmail);
	}

	public static class SessionData {
		public String token;
		public String name;
		public String email;

		public SessionData() {
		}

		public SessionData(String token, String name, String email) {
			this.token = token;
			this.name = name;
			this.email = email;
		}

		public String getToken() {
			return token;
		}

		public String getName() {
			return name;
		}

		public String getEmail() {
			return email;
		}
	}

}
