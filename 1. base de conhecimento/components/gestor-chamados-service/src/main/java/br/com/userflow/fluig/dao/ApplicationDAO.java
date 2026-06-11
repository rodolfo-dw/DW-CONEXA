package br.com.userflow.fluig.dao;

import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import com.totvs.technology.foundation.common.AbstractDAO;
import com.totvs.technology.foundation.common.exception.FDNRuntimeException;

import br.com.userflow.fluig.entity.ApplicationEntity;


@Stateless(name = "dao/ApplicationEntity", mappedName = "dao/ApplicationEntity")
public class ApplicationDAO extends AbstractDAO<ApplicationEntity> {

	public ApplicationDAO() {
		super(ApplicationEntity.class);
	}

	private EntityManager em;

	@Override
	public EntityManager getEntityManager() {
		return this.em;
	}

	@Override
	@PersistenceContext(unitName = "AppDS")
	public void setEntityManager(EntityManager em) {
		this.em = em;
	}

	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public List<ApplicationEntity> findApps(Long tenantId, String text, int limit, int offset) {
		try {
			TypedQuery<ApplicationEntity> query = getEntityManager().createNamedQuery(ApplicationEntity.FIND_BY_NAME_DEV, ApplicationEntity.class);
			query.setParameter("tenantId", tenantId);
			query.setParameter("text", "%" + text.toLowerCase() + "%");
			query.setFirstResult(offset);
			query.setMaxResults(limit);

			return query.getResultList();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new FDNRuntimeException(e.getMessage(), e.getCause()); 
		}
	}

}
