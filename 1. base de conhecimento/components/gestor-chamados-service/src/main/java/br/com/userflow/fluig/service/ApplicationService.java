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
import com.fluig.sdk.tenant.AdminUserVO;
import com.totvs.technology.foundation.common.exception.FDNCreateException;
import com.totvs.technology.foundation.common.exception.FDNRemoveException;
import com.totvs.technology.foundation.common.exception.FDNRuntimeException;
import com.totvs.technology.foundation.common.exception.FDNUpdateException;

import br.com.userflow.fluig.contract.Application;
import br.com.userflow.fluig.dao.ApplicationDAO;
import br.com.userflow.fluig.entity.ApplicationEntity;


@Remote(Application.class)
@Stateless(name = Application.JNDI_NAME, mappedName = Application.JNDI_NAME)
public class ApplicationService implements Application{
	
	@EJB
	private ApplicationDAO dao;

	@EJB(lookup = SecurityService.JNDI_REMOTE_NAME)
	private SecurityService securityService;

	@EJB(lookup = UserService.JNDI_REMOTE_NAME)
	private UserService userService;
	
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public ApplicationEntity get(long id) {
		Optional<ApplicationEntity> application = Optional.ofNullable(dao.find(id));
		return application.isPresent() ? application.get() : null;
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public long create(ApplicationEntity app) throws FDNCreateException {
		try {
			if (!isUserLoggedAdmin())
				throw new FDNCreateException("Only admin can create this resource");
			
			Optional<ApplicationEntity> newApplication = Optional.ofNullable(dao.create(app));
			return newApplication.isPresent() ? newApplication.get().getId() : null;
		} catch (FDNCreateException e) {
            throw new FDNCreateException(e.getMessage(), e);
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void update(ApplicationEntity app) throws FDNUpdateException {
		try {
			if (!isUserLoggedAdmin())
				throw new FDNCreateException("Only admin can create this resource");
			
			Optional<ApplicationEntity> application = Optional.ofNullable(dao.find(app.getId()));
			if (!application.isPresent())
				throw new FDNUpdateException("No App found for ID: " + app.getId());
			dao.edit(app);
		} catch (Exception e) {
			throw new FDNUpdateException(e.getMessage(), e);
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public void delete(long id) throws FDNRemoveException {
		try {
			if (!isUserLoggedAdmin())
				throw new FDNCreateException("Only admin can create this resource");
			
			Optional<ApplicationEntity> application = Optional.ofNullable(dao.find(id));
			if (!application.isPresent())
				throw new FDNRemoveException("No App found for ID: " + id);
			dao.remove(application.get());
		} catch (Exception e) {
			throw new FDNRemoveException(e.getMessage(), e);
		}
	}
	
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public List<ApplicationEntity> find(String text, int limit, int offset) throws SDKException{
		try {
			return dao.findApps(securityService.getCurrentTenantId(), text, limit, offset);
		} catch (FDNRuntimeException | SDKException e) {
			throw new FDNRuntimeException(e.getMessage(), e.getCause());
		}
	}
	
	private boolean isUserLoggedAdmin() {
		try {
			String login = userService.getCurrent().getLogin();
			List<AdminUserVO> tenantAdmins = securityService.listTenantAdmins(securityService.getCurrentTenantId());
			for (AdminUserVO admin : tenantAdmins)
				if (admin.getLogin().equals(login))
					return true;
			return false;
		} catch (SDKException e) {
			throw new RuntimeException("Can't request tenant admin list");
		}
	}
}
