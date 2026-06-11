package br.com.userflow.fluig.dao;

import com.totvs.technology.foundation.common.AbstractDAO;
import com.totvs.technology.foundation.common.exception.FDNRuntimeException;

import br.com.userflow.fluig.entity.LicenseEntity;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;


@Stateless(name = "dao/LicenseEntity", mappedName = "dao/LicenseEntity")
public class LicenseDAO extends AbstractDAO<LicenseEntity> {

	public LicenseDAO() {
		super(LicenseEntity.class);
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
    public List<LicenseEntity> findLicenses(Long tenantId, String text, int limit, int offset) throws FDNRuntimeException {
        try {
            TypedQuery<LicenseEntity> query = getEntityManager().createNamedQuery(LicenseEntity.FIND_BY_NAME, LicenseEntity.class);
            query.setParameter("tenantId", tenantId);
            query.setParameter("name", "%"+text.toLowerCase()+"%");
            query.setFirstResult(offset);
            query.setMaxResults(limit);
            
            return query.getResultList();
        } catch (FDNRuntimeException e) {
            log.error(e.getMessage(), e);
            throw new FDNRuntimeException(e.getMessage(), e.getCause());
        }
    }

}