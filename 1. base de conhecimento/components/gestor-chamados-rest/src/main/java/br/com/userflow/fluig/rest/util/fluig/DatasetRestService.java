package br.com.userflow.fluig.rest.util.fluig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fluig.sdk.exception.ApplicationKeyNotFoundException;

import br.com.userflow.fluig.rest.security.AuthorizationManager;

public class DatasetRestService extends RestService {

	private boolean checkAuthorization = true;

	public DatasetRestService() throws Exception, ApplicationKeyNotFoundException {
		super();
	}

	@Deprecated
	public DatasetRestService(Long tenantId) throws Exception, ApplicationKeyNotFoundException {
		super(tenantId);
	}

	public void setCheckAuthorization(boolean checkAuthorization) {
		this.checkAuthorization = checkAuthorization;
	}

	public String searchDataset(SearchDatasetParams searchDatasetParams, boolean authorize) throws Exception {

		if (this.checkAuthorization) {
			try {
				AuthorizationManager authorizationManager = new AuthorizationManager();
				authorizationManager.canAccess(searchDatasetParams.name, "dataset", authorize);
			} catch (Exception e) {
				throw new Exception("Erro de Autorização: " + e.getMessage());
			}
		}

		String endpoint = fluigURL + "/api/public/ecm/dataset/datasets";

		Map<String, Object> payload = new HashMap<>();
		payload.put("name", searchDatasetParams.name);
		payload.put("fields", searchDatasetParams.fields);
		payload.put("constraints", searchDatasetParams.constraints);
		payload.put("order", searchDatasetParams.order);

		return doPost(endpoint, payload);
	}

	public List<Map<String, Object>> formatValuesDataset(String jsonResponse) throws Exception {

		Map<String, Object> rootNode = mapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {
		});

		if (rootNode.containsKey("content")) {
			Map<String, Object> content = (Map<String, Object>) rootNode.get("content");
			if (content.containsKey("values")) {
				return (List<Map<String, Object>>) content.get("values");
			}
		}

		return new ArrayList<>();
	}

	public static class SearchDatasetParams {
		public String name;
		public List<String> fields;
		public List<String> order;
		public List<ConstraintParams> constraints;

		public SearchDatasetParams() {

		}

		public String getName() {
			return name;
		}

		public List<String> getFields() {
			return fields;
		}

		public List<String> getOrder() {
			return order;
		}

		public List<ConstraintParams> getConstraints() {
			return constraints;
		}

	}

	public static class ConstraintParams {
		public String _field;
		public String _initialValue;
		public String _finalValue;
		public int _type; // MUST, SHOULD, MUST_NOT
		public boolean _likeSearch;

		public ConstraintParams() {
			this._type = 1;
			this._likeSearch = false;
		}

		public ConstraintParams(String field, String value) {
			this._field = field;
			this._initialValue = value;
			this._finalValue = value;
			this._type = 1;
			this._likeSearch = false;
		}

		public String get_field() {
			return _field;
		}

		public String get_initialValue() {
			return _initialValue;
		}

		public String get_finalValue() {
			return _finalValue;
		}

		public int get_type() {
			return _type;
		}

		public boolean is_likeSearch() {
			return _likeSearch;
		}
	}
}