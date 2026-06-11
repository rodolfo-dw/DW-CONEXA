package br.com.userflow.fluig;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.fluig.customappkey.Keyring;
import com.fluig.sdk.api.component.activation.ActivationEvent;
import com.fluig.sdk.api.component.activation.ActivationListener;

@Remote
@Stateless(mappedName = "activator/uf-gestorchamadosapp", name = "activator/uf-gestorchamadosapp")
public class ActivateService implements ActivationListener {

	private static final String APP_KEY = "6585-5698-9865-1253";

	@Override
	public String getArtifactFileName() throws Exception {
		return "gestor-chamados-service.jar";
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void install(ActivationEvent event) throws Exception {
	}

	@Override
	public void disable(ActivationEvent evt) throws Exception {
	}

	@Override
	public void enable(ActivationEvent evt) throws Exception {
		Keyring.provision(APP_KEY);
	}

}
