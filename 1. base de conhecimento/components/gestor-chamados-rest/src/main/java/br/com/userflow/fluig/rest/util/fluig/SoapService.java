package br.com.userflow.fluig.rest.util.fluig;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;

import com.fluig.customappkey.Keyring;
import com.fluig.sdk.api.customappkey.KeyVO;
import com.fluig.sdk.exception.ApplicationKeyNotFoundException;

import br.com.userflow.fluig.rest.util.RestConstant;
import br.com.userflow.fluig.rest.util.SoapConstant;
import br.com.userflow.fluig.rest.util.TenantContext;

public class SoapService {

	private static final Logger logger = Logger.getLogger(SoapService.class.getName());

	protected final String fluigURL;
	protected final OAuthConsumer consumer;
	protected final String user;
	protected final String username;
	protected final String password;
	protected final Long tenantId;

	protected SoapService() throws Exception, ApplicationKeyNotFoundException {

		tenantId = TenantContext.getTenantId();
		if (tenantId == null) {
			throw new IllegalStateException("Tenant is not defined in context. Verify the url path sent.");
		}

		KeyVO key = Keyring.getKeys(tenantId, RestConstant.APP_KEY);

		this.user = key.getUser();
		this.username = SoapConstant.APP_USER_USERNAME;
		this.password = SoapConstant.APP_USER_PASSWORD;

		this.consumer = config(key);

		this.fluigURL = key.getDomainUrl();

		logInfo("SoapService inicializado para tenant: " + tenantId + ", usuário: " + user);
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
	protected SoapService(Long tenantId) throws Exception, ApplicationKeyNotFoundException {

		KeyVO key = Keyring.getKeys(tenantId, RestConstant.APP_KEY);

		this.user = key.getUser();
		this.username = SoapConstant.APP_USER_USERNAME;
		this.password = SoapConstant.APP_USER_PASSWORD;
		this.tenantId = key.getTenantId();

		this.consumer = config(key);

		this.fluigURL = key.getDomainUrl();

		logInfo("SoapService inicializado para tenant: " + tenantId + ", usuário: " + user);
	}

	protected <T> T initializeSoapClient(Class<T> serviceClass, String serviceName, String portName, String wsdlPath)
			throws Exception {

		try {
			String wsdlUrl = fluigURL + wsdlPath;
			logDebug("Inicializando cliente SOAP para: " + wsdlUrl);

			URL url = new URL(wsdlUrl);
			QName qname = new QName("http://ws.workflow.ecm.technology.totvs.com/", serviceName);
			Service service = Service.create(url, qname);

			QName portQName = new QName("http://ws.workflow.ecm.technology.totvs.com/", portName);
			T port = service.getPort(portQName, serviceClass);

			logInfo("Cliente SOAP inicializado com sucesso: " + serviceName);

			return port;

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Erro ao inicializar cliente SOAP: " + e.getMessage(), e);
			throw new Exception("Erro ao inicializar cliente SOAP para " + serviceName + ": " + e.getMessage(), e);
		}
	}

	private OAuthConsumer config(KeyVO key) {
		OAuthConsumer consumer = new DefaultOAuthConsumer(key.getConsumerKey(), key.getConsumerSecret());
		consumer.setTokenWithSecret(key.getToken(), key.getTokenSecret());
		return consumer;
	}

	protected String getFluigURL() {
		return fluigURL;
	}

	protected String getUser() {
		return user;
	}

	protected Long getTenantId() {
		return tenantId;
	}

	protected void logInfo(String message) {
		System.out.println(message);
		logger.log(Level.INFO, message);
	}

	protected void logError(String message, Exception exception) {
		System.out.println(message);
		logger.log(Level.SEVERE, message, exception);
	}

	protected void logDebug(String message) {
		System.out.println(message);
		logger.log(Level.FINE, message);
	}
}
