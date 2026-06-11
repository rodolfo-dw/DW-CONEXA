package br.com.userflow.fluig.contract;

import java.util.List;

import javax.ejb.Remote;

import com.fluig.sdk.api.common.SDKException;

import com.totvs.technology.foundation.common.exception.FDNCreateException;
import com.totvs.technology.foundation.common.exception.FDNRemoveException;
import com.totvs.technology.foundation.common.exception.FDNUpdateException;

import br.com.userflow.fluig.entity.ApplicationEntity;

@Remote
public interface Application {

	public static final String JNDI_NAME = "service/uf-gc-application";
	public static final String JNDI_REMOTE_NAME = "java:global/fluig/store/" + JNDI_NAME;
	
	long create(ApplicationEntity entity) throws FDNCreateException;
	
	ApplicationEntity get(long id);
	
	void update(ApplicationEntity entity) throws FDNUpdateException;
	
	void delete(long id) throws FDNRemoveException;
	
	List<ApplicationEntity> find(String text, int limit, int offset) throws SDKException;
	
}
