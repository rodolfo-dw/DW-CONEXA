package br.com.userflow.fluig.rest.security;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import br.com.userflow.fluig.rest.util.fluig.DatasetRestService;
import br.com.userflow.fluig.rest.util.fluig.CardRestService;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import br.com.userflow.fluig.rest.util.TenantContext;

public class TokenManager {

	private final String form = "ds_uf_form_gerenciamento_token_acesso";
	private final int folder;
	private final DatasetRestService datasetFluigService;
	private final CardRestService cardFluigService;
	private static final long ONE_HOUR_MS = 3600000;

	public TokenManager() throws Exception {
		datasetFluigService = new DatasetRestService();
		cardFluigService = new CardRestService();

		Long tenantId = TenantContext.getTenantId();

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
			throw new Exception("Formulário de tokens não encontrado.");
		}

		Map<String, Object> document = results.get(0);
		folder = Integer.parseInt(String.valueOf(document.get("documentPK.documentId")));
	}

	public void createSession(String token, String login, String email) throws Exception {
		Long tenantId = TenantContext.getTenantId();

		List<DatasetRestService.ConstraintParams> constraints = new ArrayList<>();
		constraints.add(new DatasetRestService.ConstraintParams("companyid", Long.toString(tenantId)));
		constraints.add(new DatasetRestService.ConstraintParams("metadata#active", "true"));
		constraints.add(new DatasetRestService.ConstraintParams("login", login));
		constraints.add(new DatasetRestService.ConstraintParams("ativo", "1"));

		DatasetRestService.SearchDatasetParams searchParams = new DatasetRestService.SearchDatasetParams();
		searchParams.name = form;
		searchParams.constraints = constraints;

		String jsonResponse = datasetFluigService.searchDataset(searchParams, true);
		List<Map<String, Object>> results = datasetFluigService.formatValuesDataset(jsonResponse);

		if (!results.isEmpty()) {
			for (Map<String, Object> tokenSessao : results) {
				int sessionId = Integer.parseInt(String.valueOf(tokenSessao.get("documentid")));
				markAsExpired(sessionId);
			}
		}

		List<CardRestService.CardDataParams> cardData = new ArrayList<>();
		cardData.add(new CardRestService.CardDataParams("login", login));
		cardData.add(new CardRestService.CardDataParams("email", email));
		cardData.add(new CardRestService.CardDataParams("token", token));
		cardData.add(new CardRestService.CardDataParams("inicio", String.valueOf(System.currentTimeMillis())));
		cardData.add(new CardRestService.CardDataParams("ativo", "1"));
		cardFluigService.createCard(this.folder, cardData);

	}

	// ALTERADO: Agora retorna String (o login)
	public String validateAndRotateToken(String token) throws Exception {
		Long tenantId = TenantContext.getTenantId();

		List<DatasetRestService.ConstraintParams> constraints = new ArrayList<>();
		constraints.add(new DatasetRestService.ConstraintParams("companyid", Long.toString(tenantId)));
		constraints.add(new DatasetRestService.ConstraintParams("metadata#active", "true"));
		constraints.add(new DatasetRestService.ConstraintParams("token", token));
		constraints.add(new DatasetRestService.ConstraintParams("ativo", "1"));

		DatasetRestService.SearchDatasetParams searchParams = new DatasetRestService.SearchDatasetParams();
		searchParams.name = form;
		searchParams.constraints = constraints;

		String jsonResponse = datasetFluigService.searchDataset(searchParams, true);
		List<Map<String, Object>> results = datasetFluigService.formatValuesDataset(jsonResponse);

		if (results.isEmpty()) {
			throw new Exception("Token não encontrado");
		}

		Map<String, Object> tokenSessao = results.get(0);
		String inicioSessaoString = (String) tokenSessao.get("inicio");
		int sessionId = (int) tokenSessao.get("documentid");

		// Captura o login do usuário
		String login = (String) tokenSessao.get("login");

		long inicioSessao = Long.parseLong(inicioSessaoString);
		long now = System.currentTimeMillis();

		if ((now - inicioSessao) > ONE_HOUR_MS) {
			markAsExpired(sessionId);
			throw new Exception("Token expirado. Sessão encerrada.");
		} else {
			renewExpirationTime(sessionId);
		}

		return login;
	}

	public void invalidateToken(String token) throws Exception {
		Long tenantId = TenantContext.getTenantId();

		List<DatasetRestService.ConstraintParams> constraints = new ArrayList<>();
		constraints.add(new DatasetRestService.ConstraintParams("companyid", Long.toString(tenantId)));
		constraints.add(new DatasetRestService.ConstraintParams("metadata#active", "true"));
		constraints.add(new DatasetRestService.ConstraintParams("token", token));
		constraints.add(new DatasetRestService.ConstraintParams("ativo", "1"));

		DatasetRestService.SearchDatasetParams searchParams = new DatasetRestService.SearchDatasetParams();
		searchParams.name = form;
		searchParams.constraints = constraints;

		String jsonResponse = datasetFluigService.searchDataset(searchParams, true);
		List<Map<String, Object>> results = datasetFluigService.formatValuesDataset(jsonResponse);

		if (results.isEmpty()) {
			throw new Exception("Token não encontrado");
		}

		Map<String, Object> tokenSessao = results.get(0);
		int sessionId = (int) tokenSessao.get("documentid");
		markAsExpired(sessionId);

	}

	private void markAsExpired(int sessionId) throws Exception {

		List<CardRestService.CardDataParams> cardData = new ArrayList<>();
		cardData.add(new CardRestService.CardDataParams("ativo", "0"));
		cardFluigService.editCard(this.folder, sessionId, cardData);

	}

	private void renewExpirationTime(int sessionId) throws Exception {

		List<CardRestService.CardDataParams> cardData = new ArrayList<>();
		cardData.add(new CardRestService.CardDataParams("inicio", String.valueOf(System.currentTimeMillis())));
		cardFluigService.editCard(this.folder, sessionId, cardData);

	}

}