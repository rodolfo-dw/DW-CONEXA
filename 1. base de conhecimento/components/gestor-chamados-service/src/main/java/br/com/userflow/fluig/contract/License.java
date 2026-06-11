package br.com.userflow.fluig.contract;

import java.util.List;

import javax.ejb.Remote;

import com.fluig.sdk.api.common.SDKException;

import com.totvs.technology.foundation.common.exception.FDNCreateException;
import com.totvs.technology.foundation.common.exception.FDNRemoveException;
import com.totvs.technology.foundation.common.exception.FDNUpdateException;

import br.com.userflow.fluig.entity.LicenseEntity;

@Remote
public interface License {

	public static final String JNDI_NAME = "service/uf-gc-license";
	public static final String JNDI_REMOTE_NAME = "java:global/fluig/store/" + JNDI_NAME;
	
	long create(LicenseEntity entity) throws FDNCreateException;
	
	LicenseEntity get(long id);
	
	void update(LicenseEntity entity) throws FDNUpdateException;
	
	void delete(long id) throws FDNRemoveException;
	
	List<LicenseEntity> find(String text, int limit, int offset) throws SDKException;
}
