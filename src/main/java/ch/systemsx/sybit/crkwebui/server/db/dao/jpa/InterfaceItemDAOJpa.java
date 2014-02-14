package ch.systemsx.sybit.crkwebui.server.db.dao.jpa;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import eppic.model.InterfaceItemDB_;
import eppic.model.PDBScoreItemDB_;
import ch.systemsx.sybit.crkwebui.server.db.EntityManagerHandler;
import ch.systemsx.sybit.crkwebui.server.db.dao.InterfaceItemDAO;
import ch.systemsx.sybit.crkwebui.shared.exceptions.DaoException;
import ch.systemsx.sybit.crkwebui.shared.model.InterfaceItem;
import eppic.model.InterfaceDB;
import eppic.model.PdbInfoDB;

/**
 * Implementation of InterfaceItemDAO.
 * @author AS
 *
 */
public class InterfaceItemDAOJpa implements InterfaceItemDAO 
{
	@Override
	public List<InterfaceItem> getInterfacesWithScores(int pdbScoreUid) throws DaoException
	{
		EntityManager entityManager = null;
		
		try
		{
			List<InterfaceItem> result = new ArrayList<InterfaceItem>();
			
			entityManager = EntityManagerHandler.getEntityManager();
			
			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery<InterfaceDB> criteriaQuery = criteriaBuilder.createQuery(InterfaceDB.class);
			
			Root<InterfaceDB> interfaceItemRoot = criteriaQuery.from(InterfaceDB.class);
			Path<PdbInfoDB> pdbScoreItemDB = interfaceItemRoot.get(InterfaceItemDB_.pdbScoreItem);
			criteriaQuery.where(criteriaBuilder.equal(pdbScoreItemDB.get(PDBScoreItemDB_.uid), pdbScoreUid));
			
			Query query = entityManager.createQuery(criteriaQuery);
			
			@SuppressWarnings("unchecked")
			List<InterfaceDB> interfaceItemDBs = query.getResultList();
			
			for(InterfaceDB interfaceItemDB : interfaceItemDBs)
			{
				interfaceItemDB.setResidues(null);
				result.add(InterfaceItem.create(interfaceItemDB));
			}
			
			return result;
		}
		catch(Throwable e)
		{
			e.printStackTrace();
			throw new DaoException(e);
		}
		finally
		{
			try
			{
				entityManager.close();
			}
			catch(Throwable t)
			{
				t.printStackTrace();
			}
		}
	}
	
	@Override
	public List<InterfaceItem> getInterfacesWithScores(int pdbScoreUid, List<Integer> interfaceIds)throws DaoException
	{
		EntityManager entityManager = null;
		
		try
		{
			List<InterfaceItem> result = new ArrayList<InterfaceItem>();
			
			entityManager = EntityManagerHandler.getEntityManager();
			
			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery<InterfaceDB> criteriaQuery = criteriaBuilder.createQuery(InterfaceDB.class);
			
			Root<InterfaceDB> interfaceItemRoot = criteriaQuery.from(InterfaceDB.class);
			Path<PdbInfoDB> pdbScoreItemDB = interfaceItemRoot.get(InterfaceItemDB_.pdbScoreItem);
			criteriaQuery.where(criteriaBuilder.equal(pdbScoreItemDB.get(PDBScoreItemDB_.uid), pdbScoreUid));
			
			Query query = entityManager.createQuery(criteriaQuery);
			
			@SuppressWarnings("unchecked")
			List<InterfaceDB> interfaceItemDBs = query.getResultList();
			
			for(InterfaceDB interfaceItemDB : interfaceItemDBs)
			{
				interfaceItemDB.setResidues(null);
				if(interfaceIds.contains(interfaceItemDB.getInterfaceId())){
					result.add(InterfaceItem.create(interfaceItemDB));
				}
			}
			
			return result;
		}
		catch(Throwable e)
		{
			e.printStackTrace();
			throw new DaoException(e);
		}
		finally
		{
			try
			{
				entityManager.close();
			}
			catch(Throwable t)
			{
				t.printStackTrace();
			}
		}
		
	}
	
	@Override
	public List<InterfaceItem> getInterfacesWithResidues(int pdbScoreUid) throws DaoException
	{
		EntityManager entityManager = null;
		
		try
		{
			List<InterfaceItem> result = new ArrayList<InterfaceItem>();
			
			entityManager = EntityManagerHandler.getEntityManager();
			
			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery<InterfaceDB> criteriaQuery = criteriaBuilder.createQuery(InterfaceDB.class);
			
			Root<InterfaceDB> interfaceItemRoot = criteriaQuery.from(InterfaceDB.class);
			Path<PdbInfoDB> pdbScoreItemDB = interfaceItemRoot.get(InterfaceItemDB_.pdbScoreItem);
			criteriaQuery.where(criteriaBuilder.equal(pdbScoreItemDB.get(PDBScoreItemDB_.uid), pdbScoreUid));
			
			Query query = entityManager.createQuery(criteriaQuery);
			
			@SuppressWarnings("unchecked")
			List<InterfaceDB> interfaceItemDBs = query.getResultList();
			
			for(InterfaceDB interfaceItemDB : interfaceItemDBs)
			{
				interfaceItemDB.setInterfaceScores(null);
				interfaceItemDB.setInterfaceWarnings(null);
				result.add(InterfaceItem.create(interfaceItemDB));
			}
			
			return result;
		}
		catch(Throwable e)
		{
			e.printStackTrace();
			throw new DaoException(e);
		}
		finally
		{
			try
			{
				entityManager.close();
			}
			catch(Throwable t)
			{
				t.printStackTrace();
			}
		}
	}
}
