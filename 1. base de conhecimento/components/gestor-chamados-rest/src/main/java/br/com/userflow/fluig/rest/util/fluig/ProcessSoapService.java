package br.com.userflow.fluig.rest.util.fluig;

import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.userflow.fluig.rest.util.fluig.soap.generated.WorkflowEngineService;
import br.com.userflow.fluig.rest.util.fluig.soap.generated.StringArray;
import br.com.userflow.fluig.rest.util.fluig.soap.generated.StringArrayArray;
import br.com.userflow.fluig.rest.util.fluig.soap.generated.ProcessAttachmentDtoArray;
import br.com.userflow.fluig.rest.util.fluig.soap.generated.ProcessTaskAppointmentDtoArray;

import com.fluig.sdk.exception.ApplicationKeyNotFoundException;

public class ProcessSoapService extends SoapService {

	private static final Logger logger = Logger.getLogger(ProcessSoapService.class.getName());

	private static final String WSDL_PATH = "/webdesk/ECMWorkflowEngineService?wsdl";
	private static final String SERVICE_NAME = "ECMWorkflowEngineServiceService";
	private static final String PORT_NAME = "WorkflowEngineServicePort";

	public ProcessSoapService() throws Exception, ApplicationKeyNotFoundException {
		super();
		logInfo("ProcessServiceSoap inicializado com sucesso");
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
	public ProcessSoapService(Long tenantId) throws Exception, ApplicationKeyNotFoundException {
		super(tenantId);
		logInfo("ProcessServiceSoap inicializado com sucesso");
	}

	public StringArrayArray startProcess(StartProcessParams startProcessParams) throws Exception {

		try {
			// Log dos parâmetros que estão indo para o método
			logInfo("Chamando startProcess para usuário: " + user);
			logInfo("Iniciando processo: " + startProcessParams.processId);

			WorkflowEngineService port = initializeSoapClient(WorkflowEngineService.class, SERVICE_NAME, PORT_NAME,
					WSDL_PATH);

			StringArrayArray result = port.startProcess(username, password, tenantId.intValue(),
					startProcessParams.processId, startProcessParams.choosedState,
					convertToStringArray(startProcessParams.colleagueIds), startProcessParams.comments, user,
					startProcessParams.completeTask, startProcessParams.attachments,
					convertToStringArrayArray(startProcessParams.cardData), startProcessParams.appointment,
					startProcessParams.managerMode);

			logInfo("Processo iniciado com sucesso: " + startProcessParams.processId);
			return result;

		} catch (javax.xml.ws.soap.SOAPFaultException sfe) {
			// LOG DETALHADO DO ERRO SOAP
			System.out.println("### ERRO SOAP FAULT ###");
			System.out.println("Fault Code: " + sfe.getFault().getFaultCode());
			System.out.println("Fault String: " + sfe.getFault().getFaultString());
			if (sfe.getFault().getDetail() != null) {
				System.out.println("Fault Detail: " + sfe.getFault().getDetail().getTextContent());
			}
			throw sfe;
		} catch (Exception e) {
			logError("Erro ao iniciar processo " + startProcessParams.processId, e);
			throw new Exception("Erro ao iniciar processo " + startProcessParams.processId + ": " + e.getMessage(), e);
		}
	}

	public StringArray simpleStartProcess(SimpleStartProcessParams simpleStartProcessParams) throws Exception {

		try {
			logInfo("Iniciando processo simples: " + simpleStartProcessParams.processId);

			WorkflowEngineService port = initializeSoapClient(WorkflowEngineService.class, SERVICE_NAME, PORT_NAME,
					WSDL_PATH);

			StringArray result = port.simpleStartProcess(username, password, tenantId.intValue(),
					simpleStartProcessParams.processId, simpleStartProcessParams.comments,
					simpleStartProcessParams.attachments, convertToStringArrayArray(simpleStartProcessParams.cardData));

			logInfo("Processo simples iniciado com sucesso: " + simpleStartProcessParams.processId);
			return result;

		} catch (Exception e) {
			logError("Erro ao iniciar processo simples " + simpleStartProcessParams.processId, e);
			throw new Exception(
					"Erro ao iniciar processo simples " + simpleStartProcessParams.processId + ": " + e.getMessage(),
					e);
		}
	}

	public StringArrayArray saveAndSendTask(SaveAndSendTaskParams saveAndSendTaskParams) throws Exception {

		try {
			logInfo("Enviando tarefa para instância: " + saveAndSendTaskParams.processInstanceId);

			WorkflowEngineService port = initializeSoapClient(WorkflowEngineService.class, SERVICE_NAME, PORT_NAME,
					WSDL_PATH);

			StringArrayArray result = port.saveAndSendTask(username, password, tenantId.intValue(),
					saveAndSendTaskParams.processInstanceId, saveAndSendTaskParams.choosedState,
					convertToStringArray(saveAndSendTaskParams.colleagueIds), saveAndSendTaskParams.comments, user,
					saveAndSendTaskParams.completeTask, saveAndSendTaskParams.attachments,
					convertToStringArrayArray(saveAndSendTaskParams.cardData), saveAndSendTaskParams.appointment,
					saveAndSendTaskParams.managerMode, saveAndSendTaskParams.threadSequence);

			logInfo("Tarefa enviada com sucesso para instância: " + saveAndSendTaskParams.processInstanceId);
			return result;

		} catch (Exception e) {
			logError("Erro ao enviar tarefa para instância " + saveAndSendTaskParams.processInstanceId, e);
			throw new Exception("Erro ao enviar tarefa para instância " + saveAndSendTaskParams.processInstanceId + ": "
					+ e.getMessage(), e);
		}
	}

	private StringArray convertToStringArray(String[] array) {
		if (array == null) {
			return null;
		}

		StringArray soapArray = new StringArray();
		for (String item : array) {
			soapArray.getItem().add(item);
		}

		logDebug("Array convertido: " + array.length + " elementos");
		return soapArray;
	}

	private StringArrayArray convertToStringArrayArray(String[][] array) {
		if (array == null) {
			return null;
		}

		StringArrayArray soapArray = new StringArrayArray();
		for (String[] row : array) {
			StringArray soapRow = convertToStringArray(row);
			soapArray.getItem().add(soapRow);
		}

		logDebug("Array bidimensional convertido: " + array.length + " linhas");
		return soapArray;
	}

	public static class StartProcessParams {
		public String processId;
		public int choosedState;
		public String[] colleagueIds;
		public String comments;
		public boolean completeTask;
		public ProcessAttachmentDtoArray attachments;
		public String[][] cardData;
		public ProcessTaskAppointmentDtoArray appointment;
		public boolean managerMode;

		public StartProcessParams() {
			completeTask = true;
			managerMode = true;
			attachments = new ProcessAttachmentDtoArray();
			appointment = new ProcessTaskAppointmentDtoArray();
		}

		// Getters necessários para o Jackson converter para JSON
		public String getProcessId() {
			return processId;
		}

		public int getChoosedState() {
			return choosedState;
		}

		public String[] getColleagueIds() {
			return colleagueIds;
		}

		public String getComments() {
			return comments;
		}

		public boolean isCompleteTask() {
			return completeTask;
		}

		public ProcessAttachmentDtoArray getAttachments() {
			return attachments;
		}

		public String[][] getCardData() {
			return cardData;
		}

		public ProcessTaskAppointmentDtoArray getAppointment() {
			return appointment;
		}

		public boolean isManagerMode() {
			return managerMode;
		}
	}

	public static class SimpleStartProcessParams {
		public String processId;
		public String comments;
		public ProcessAttachmentDtoArray attachments;
		public String[][] cardData;

		public SimpleStartProcessParams() {
			attachments = new ProcessAttachmentDtoArray();
		}

		public String getProcessId() {
			return processId;
		}

		public String getComments() {
			return comments;
		}

		public ProcessAttachmentDtoArray getAttachments() {
			return attachments;
		}

		public String[][] getCardData() {
			return cardData;
		}
	}

	public static class SaveAndSendTaskParams {
		public int processInstanceId;
		public int choosedState;
		public String[] colleagueIds;
		public String comments;
		public boolean completeTask;
		public ProcessAttachmentDtoArray attachments;
		public String[][] cardData;
		public ProcessTaskAppointmentDtoArray appointment;
		public boolean managerMode;
		public int threadSequence;

		public SaveAndSendTaskParams() {
			managerMode = true;
			attachments = new ProcessAttachmentDtoArray();
			appointment = new ProcessTaskAppointmentDtoArray();
		}

		public int getProcessInstanceId() {
			return processInstanceId;
		}

		public int getChoosedState() {
			return choosedState;
		}

		public String[] getColleagueIds() {
			return colleagueIds;
		}

		public String getComments() {
			return comments;
		}

		public boolean isCompleteTask() {
			return completeTask;
		}

		public ProcessAttachmentDtoArray getAttachments() {
			return attachments;
		}

		public String[][] getCardData() {
			return cardData;
		}

		public ProcessTaskAppointmentDtoArray getAppointment() {
			return appointment;
		}

		public boolean isManagerMode() {
			return managerMode;
		}

		public int getThreadSequence() {
			return threadSequence;
		}
	}

}
