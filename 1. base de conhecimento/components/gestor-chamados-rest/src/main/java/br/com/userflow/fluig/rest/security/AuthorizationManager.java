package br.com.userflow.fluig.rest.security;

import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import br.com.userflow.fluig.rest.util.TenantContext;
import br.com.userflow.fluig.rest.util.UserContext;
import br.com.userflow.fluig.rest.util.fluig.DatasetRestService;

public class AuthorizationManager {

	private final String form = "ds_uf_form_cadastro_autorizacao_rest";
	private final int folder;
	private final DatasetRestService datasetFluigService;

	public AuthorizationManager() throws Exception {
		Long tenantId = TenantContext.getTenantId();

		// Importante: Desativar a checagem interna para evitar Loop Infinito
		// pois este serviço é usado para buscar as próprias regras de autorização.
		datasetFluigService = new DatasetRestService();
		datasetFluigService.setCheckAuthorization(false);

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
			throw new Exception("Formulário de autorização de acessos não encontrado.");
		}

		Map<String, Object> document = results.get(0);
		folder = Integer.parseInt(String.valueOf(document.get("documentPK.documentId")));
	}

	/**
	 * Verifica se o código (Dataset ou Processo) está liberado no formulário de
	 * autorização. Lança exceção se não estiver liberado. * @param resourceId O
	 * nome do dataset ou o código do processo a ser verificado. * @throws Exception
	 * Caso o recurso não esteja liberado ou ativo.
	 */
	public void canAccess(String resourceId, String resourceType, boolean authorize) throws Exception {
		if (authorize) {
			return;
		}

		Long tenantId = TenantContext.getTenantId();

		List<DatasetRestService.ConstraintParams> constraintsPath = new ArrayList<>();
		constraintsPath.add(new DatasetRestService.ConstraintParams("companyid", Long.toString(tenantId)));
		constraintsPath.add(new DatasetRestService.ConstraintParams("metadata#active", "true"));
		//
		constraintsPath.add(new DatasetRestService.ConstraintParams("resourceId", resourceId));
		constraintsPath.add(new DatasetRestService.ConstraintParams("resourceType", resourceType));

		DatasetRestService.SearchDatasetParams searchParams = new DatasetRestService.SearchDatasetParams();
		searchParams.name = form;
		searchParams.fields = Arrays.asList("ativo", "resourceId");
		searchParams.constraints = constraintsPath;

		String jsonResponse = datasetFluigService.searchDataset(searchParams, true);
		List<Map<String, Object>> resources = datasetFluigService.formatValuesDataset(jsonResponse);

		if (resources.isEmpty()) {
			throw new Exception("Recurso não liberado ou não encontrado: " + resourceId);
		}

		Map<String, Object> authorization = resources.get(0);
		String access = (String) authorization.get("ativo");

		if (!"1".equals(access)) {
			throw new Exception("Recurso desativado: " + resourceId);
		}
	}

	public void canMove(String instanceProcessId) throws Exception {

		// Pega o usuário logado automaticamente do contexto
		String userId = UserContext.getUserId();

		if (userId == null || userId.isEmpty()) {
			throw new Exception("Usuário não identificado na sessão.");
		}

		Long tenantId = TenantContext.getTenantId();

		List<DatasetRestService.ConstraintParams> constraintsPath = new ArrayList<>();
		constraintsPath.add(new DatasetRestService.ConstraintParams("companyid", Long.toString(tenantId)));
		constraintsPath.add(new DatasetRestService.ConstraintParams("metadata#active", "true"));

		// Valida se o processo é este e se o usuário dono é o usuário logado
		constraintsPath.add(new DatasetRestService.ConstraintParams("processNumber", instanceProcessId));
		constraintsPath.add(new DatasetRestService.ConstraintParams("idUserCreationExt", userId));

		DatasetRestService.SearchDatasetParams searchParams = new DatasetRestService.SearchDatasetParams();
		searchParams.name = "ds_uf_form_gestor_chamados";
		searchParams.fields = Arrays.asList("processNumber");
		searchParams.constraints = constraintsPath;

		String jsonResponse = datasetFluigService.searchDataset(searchParams, true);
		List<Map<String, Object>> resources = datasetFluigService.formatValuesDataset(jsonResponse);

		if (resources.isEmpty()) {
			throw new Exception("Solicitação " + instanceProcessId + " não pertence ao usuário " + userId
					+ " ou não foi encontrada.");
		}
	}

}