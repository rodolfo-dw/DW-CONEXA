package br.com.userflow.fluig.service;

import java.util.List;
import java.util.Optional;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.fluig.sdk.api.common.SDKException;
import com.fluig.sdk.service.SecurityService;
import com.fluig.sdk.service.UserService;

import com.totvs.technology.foundation.common.exception.FDNCreateException;
import com.totvs.technology.foundation.common.exception.FDNRemoveException;
import com.totvs.technology.foundation.common.exception.FDNUpdateException;

import br.com.userflow.fluig.contract.License;
import br.com.userflow.fluig.dao.LicenseDAO;
import br.com.userflow.fluig.entity.LicenseEntity;

@Remote(License.class)
@Stateless(name = License.JNDI_NAME, mappedName = License.JNDI_NAME)
public class LicenseService implements License {
	
	
	@EJB
	private LicenseDAO dao;

	@EJB(lookup = SecurityService.JNDI_REMOTE_NAME)
	private SecurityService securityService;
	
	@EJB(lookup = UserService.JNDI_REMOTE_NAME)
	private UserService userService;
	
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public LicenseEntity get(long id) {
		Optional<LicenseEntity> license = Optional.ofNullable(dao.find(id));
		return (license.isPresent() ? license.get() : null);
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public long create(LicenseEntity license) throws FDNCreateException {
		try {
			/**
			 * check permission if needed 
			 */
			license.setTenantId(securityService.getCurrentTenantId());
			Optional<LicenseEntity> newLicense = Optional.ofNullable(dao.create(license));
			return (newLicense.isPresent() ? newLicense.get().getId() : null);
		} catch (FDNCreateException | SDKException e) {
            throw new FDNCreateException(e.getMessage(), e);
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void update(LicenseEntity license) throws FDNUpdateException {
		/**
		 * check permission if needed
		 */
		Optional<LicenseEntity> licenseBeforeUpdate = Optional.ofNullable(dao.find(license.getId()));
		if (!licenseBeforeUpdate.isPresent())
			throw new FDNUpdateException("No Category found for ID: " + license.getId());
		license.setTenantId(licenseBeforeUpdate.get().getTenantId());
		dao.edit(license);
	}

	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void delete(long id) throws FDNRemoveException {
		/**
		 * check permission if needed
		 */
		Optional<LicenseEntity> license = Optional.ofNullable(dao.find(id));
		if (!license.isPresent())
			throw new FDNRemoveException("No Category found for ID: " + id);
		dao.remove(license.get());
	}

	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public List<LicenseEntity> find(String text, int limit, int offset) throws SDKException {
		return dao.findLicenses(securityService.getCurrentTenantId(), text, limit, offset);
	}


}
