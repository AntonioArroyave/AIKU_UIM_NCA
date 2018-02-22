package aiku.provision.cfs.worker;

import java.util.Collection;
import java.util.Set;

import oracle.communications.inventory.api.common.AttachmentManager;
import oracle.communications.inventory.api.common.EntityUtils;
import oracle.communications.inventory.api.entity.Party;
import oracle.communications.inventory.api.entity.PartyServiceRel;
import oracle.communications.inventory.api.entity.Service;
import oracle.communications.inventory.api.entity.ServiceCharacteristic;
import oracle.communications.inventory.api.entity.ServiceSpecification;
import oracle.communications.inventory.api.entity.ServiceStatus;
import oracle.communications.inventory.api.entity.common.Involvement;
import oracle.communications.inventory.api.exception.ValidationException;
import oracle.communications.inventory.api.service.ServiceManager;
import oracle.communications.inventory.api.util.Utils;
import oracle.communications.inventory.xmlbeans.PartyType;
import oracle.communications.platform.persistence.Finder;
import oracle.communications.platform.persistence.PersistenceHelper;

public class serviceWorker {
	ServiceManager servMan;
	Finder find;
	
	public serviceWorker(Finder finder){
		servMan = PersistenceHelper.makeServiceManager();
		find = finder;
	}

	public void setServiceChar(Service workService, String charName, String charValue){
		ServiceSpecification spec = workService.getSpecification();
		EntityUtils.setValue( workService, charName, charValue );   
		Set<ServiceCharacteristic> newChars = (Set<ServiceCharacteristic>)workService.getCharacteristics();
		EntityUtils.defaultMissingCharacteristics( workService, newChars, spec.getCharacteristicSpecUsages() );
		workService.setCharacteristics( newChars );
	}

	public void findAndAssociatePartytoService(Service workService, PartyType subscriber)
			throws ValidationException {
		Collection<Party> parties = find.findById(Party.class, subscriber.getId());
		if(Utils.isEmpty(parties))
			throw new ValidationException("Subscriber not found in the inventory.");
		Party party = parties.iterator().next();
		find.reset();
		Collection<PartyServiceRel> relation = findrelationBetweenPartyAndService(party, workService);
		if (Utils.isEmpty(relation))
			addPartyToService(party, workService);
		find.reset();
	}

	private Collection<PartyServiceRel> findrelationBetweenPartyAndService(
			Party serviceAccount, Service service) {
		String jpqlQuery = "select o from PartyServiceRel o"
				+ " where o.party.id='"+serviceAccount.getId()+ "'"
				+ " and o.service.adminState not in (:disconnected , :cancelled)"
				+ " and o.service.entityId='"+service.getEntityId()+"'";
		find.setParameters(new String[] {"disconnected", "cancelled"}, 
				new ServiceStatus[] {ServiceStatus.DISCONNECTED, ServiceStatus.CANCELLED});
		@SuppressWarnings("unchecked")
		Collection<PartyServiceRel> relList = (Collection<PartyServiceRel>) find.findByJPQL(jpqlQuery);
		return relList;
	}

	private void addPartyToService(Party serviceAccount, Service service) throws ValidationException {
		AttachmentManager attachmentMgr = PersistenceHelper.makeAttachmentManager();
		Involvement involvement = attachmentMgr.makeRel(PartyServiceRel.class);
		involvement.setToEntity(service);
		involvement.setFromEntity(serviceAccount);
		involvement.setFromEntityRoleKey(serviceAccount.getInvRoles().iterator().next().getOid());
		attachmentMgr.createRel(involvement);
		attachmentMgr.flushTransaction();
	}

}
