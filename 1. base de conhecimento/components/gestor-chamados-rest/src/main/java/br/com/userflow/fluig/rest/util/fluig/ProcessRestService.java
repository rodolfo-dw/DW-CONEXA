package br.com.userflow.fluig.rest.util.fluig;

import java.util.HashMap;
import java.util.Map;

import com.fluig.sdk.exception.ApplicationKeyNotFoundException;

import br.com.userflow.fluig.rest.security.AuthorizationManager;

public class ProcessRestService extends RestService {

	private boolean checkAuthorization = true;

	public ProcessRestService() throws Exception, ApplicationKeyNotFoundException {
		super();
	}

	@Deprecated
	public ProcessRestService(Long tenantId) throws Exception, ApplicationKeyNotFoundException {
		super(tenantId);
	}

	public void setCheckAuthorization(boolean checkAuthorization) {
		this.checkAuthorization = checkAuthorization;
	}

	public String startProcess(String processId, Map<String, Object> payloadMap) throws Exception {

		if (this.checkAuthorization) {
			try {
				AuthorizationManager authorizationManager = new AuthorizationManager();
				authorizationManager.canAccess(processId, "processo", false);
			} catch (Exception e) {
				throw new Exception("Erro de Autorização: " + e.getMessage());
			}
		}

		String endpoint = fluigURL + "/process-management/api/v2/processes/" + processId + "/start";
		return doPost(endpoint, payloadMap);
	}

	public String moveProcess(int instanceProcessId, Map<String, Object> payloadMap) throws Exception {

		if (this.checkAuthorization) {
			try {
				AuthorizationManager authorizationManager = new AuthorizationManager();
				// Chama a validação passando o ID da solicitação.
				// O usuário é pego automaticamente pelo Contexto.
				authorizationManager.canMove(String.valueOf(instanceProcessId));
			} catch (Exception e) {
				throw new Exception("Erro de Autorização: " + e.getMessage());
			}
		}

		String endpoint = fluigURL + "/process-management/api/v2/requests/" + instanceProcessId + "/move";
		return doPost(endpoint, payloadMap);
	}

}