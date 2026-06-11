package br.com.userflow.fluig.rest.util.fluig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fluig.sdk.exception.ApplicationKeyNotFoundException;

import br.com.userflow.fluig.rest.util.TenantContext;
import br.com.userflow.fluig.rest.util.fluig.DatasetRestService.ConstraintParams;
import br.com.userflow.fluig.rest.util.fluig.DatasetRestService.SearchDatasetParams;

public class FolderService extends RestService {

	public FolderService() throws Exception, ApplicationKeyNotFoundException {
		super();
	}

	@Deprecated
	public FolderService(Long tenantId) throws Exception, ApplicationKeyNotFoundException {
		super(tenantId);
	}

	public Integer getOrCreateFolder(Integer parentId, String folderName) throws Exception {
		Integer existingId = findFolderByDataset(parentId, folderName);

		if (existingId != null) {
			return existingId;
		}

		return createFolderApi(parentId, folderName);
	}

	private Integer findFolderByDataset(Integer parentId, String folderName) throws Exception {
		Long tenantId = TenantContext.getTenantId();

		DatasetRestService datasetService = new DatasetRestService();

		datasetService.setCheckAuthorization(false);

		SearchDatasetParams params = new SearchDatasetParams();
		params.name = "document";
		params.fields = new ArrayList<>();
		params.fields.add("documentPK.documentId");

		params.constraints = new ArrayList<>();
		params.constraints.add(new ConstraintParams("parentDocumentId", String.valueOf(parentId)));
		params.constraints.add(new ConstraintParams("documentDescription", folderName));
		params.constraints.add(new ConstraintParams("deleted", "false"));
		params.constraints.add(new ConstraintParams("activeVersion", "true"));
		params.constraints.add(new ConstraintParams("documentType", "1"));

		params.constraints.add(new ConstraintParams("documentPK.companyId", String.valueOf(tenantId)));

		String jsonResult = datasetService.searchDataset(params, false);

		List<Map<String, Object>> rows = datasetService.formatValuesDataset(jsonResult);

		if (rows != null && !rows.isEmpty()) {
			Object idObj = rows.get(0).get("documentPK.documentId");
			if (idObj != null) {
				String idStr = String.valueOf(idObj);
				return Integer.parseInt(idStr.replace(".0", ""));
			}
		}

		return null;
	}

	private Integer createFolderApi(Integer parentId, String folderName) throws Exception {
		String endpoint = fluigURL + "/content-management/api/v2/folders/" + parentId;

		Map<String, Object> payload = new HashMap<>();

		payload.put("alias", folderName);

		String responseJson = doPost(endpoint, payload);

		Map<String, Object> responseMap = mapper.readValue(responseJson, new TypeReference<Map<String, Object>>() {
		});

		if (responseMap.containsKey("content")) {
			@SuppressWarnings("unchecked")
			Map<String, Object> content = (Map<String, Object>) responseMap.get("content");

			if (content.containsKey("id")) {
				return Integer.parseInt(content.get("id").toString());
			}
			if (content.containsKey("documentId")) {
				return Integer.parseInt(content.get("documentId").toString());
			}
		}

		if (responseMap.containsKey("id")) {
			return Integer.parseInt(responseMap.get("id").toString());
		}

		if (responseMap.containsKey("documentId")) {
			return Integer.parseInt(responseMap.get("documentId").toString());
		}

		throw new Exception("Pasta criada, mas o ID não foi retornado corretamente. Resposta: " + responseJson);
	}
}