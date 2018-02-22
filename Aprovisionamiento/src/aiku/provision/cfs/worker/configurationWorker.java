package aiku.provision.cfs.worker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.communications.inventory.api.characteristic.CharacteristicManager;
import oracle.communications.inventory.api.entity.CharacteristicSpecification;
import oracle.communications.inventory.api.entity.InventoryConfigurationSpec;
import oracle.communications.inventory.api.entity.Service;
import oracle.communications.inventory.api.entity.ServiceAssignment;
import oracle.communications.inventory.api.entity.ServiceConfigurationItem;
import oracle.communications.inventory.api.entity.ServiceConfigurationItemCharacteristic;
import oracle.communications.inventory.api.entity.ServiceConfigurationVersion;
import oracle.communications.inventory.api.entity.Specification;
import oracle.communications.inventory.api.entity.SpecificationRel;
import oracle.communications.inventory.api.entity.common.CharValue;
import oracle.communications.inventory.api.entity.common.ConfigurationReferenceEnabled;
import oracle.communications.inventory.api.entity.common.ConsumableResource;
import oracle.communications.inventory.api.entity.common.InventoryConfigurationItem;
import oracle.communications.inventory.api.exception.ValidationException;
import oracle.communications.inventory.api.framework.logging.Log;
import oracle.communications.inventory.api.framework.logging.LogFactory;
import oracle.communications.inventory.api.framework.policy.RequestPolicyHelper;
import oracle.communications.inventory.api.service.ServiceConfigurationManager;
import oracle.communications.inventory.api.util.Utils;

import oracle.communications.platform.persistence.Finder;
import oracle.communications.platform.persistence.PersistenceHelper;
import oracle.communications.platform.persistence.Persistent;

public class configurationWorker {
	private static Log log = LogFactory.getLog(configurationWorker.class);
	
	ServiceConfigurationManager servConfMan;

	public configurationWorker(){
		servConfMan = PersistenceHelper.makeServiceConfigurationManager();
	}

	/**
	 * Retrieves the RFS from the CFS configuration
	 * @param scv - current CFS configuration
	 * @return Service
	 * @throws ValidationException
	 */
	public Service getRFSFromCFSConfiguration
	(ServiceConfigurationVersion scv) throws ValidationException
	{
		//Retrieve the RFS
		ServiceConfigurationItem configItem = findServiceConfigItemByName(scv.getConfigItems(), "L3_Service_RFS").iterator().next();
		if(configItem==null)
			throw new ValidationException("RFS Config Item not found.");
		Persistent entity = configItem.getToEntity();
		Service rfs = null;
		if (entity != null && entity instanceof ServiceAssignment) {
			rfs = ((ServiceAssignment) entity).getService();
		}	
		return rfs;
	}

	public ServiceConfigurationItem findServiceConfigItemByPath
	(ServiceConfigurationItem rootConfigItem, String itemPath) throws ValidationException {
		System.out.println("Entering findServiceConfigItemByPath() method");
		if (Utils.isEmpty(itemPath)) {
			return null;
		}	
		String[] tokens = itemPath.trim().split("\\.");
		Pattern pattern = Pattern.compile("(?<name>\\w+)(?:\\[(?<index>\\d+)\\])?");
		ServiceConfigurationItem foundItem = rootConfigItem;
		for(String subPath : tokens){
			System.out.println("Getting full item *"+subPath+"*");

			Matcher matcher = pattern.matcher(subPath);
			matcher.find();
			int configIndex = (matcher.group("index")!=null)? Integer.valueOf(matcher.group("index")):0;

			System.out.println("Current serviceConfigItem: "+foundItem.getEntityId());
			System.out.println("Size of the ChildConfigItems "+foundItem.getChildConfigItems().size());
			
			List<ServiceConfigurationItem> search = findServiceConfigItemByName(foundItem.getChildConfigItems(), matcher.group("name"));
			if(search.size()<configIndex)
				throw new ValidationException("Config Item Index out of bounds, name "+matcher.group("name"));
			foundItem = search.get(configIndex);
		}
		return foundItem;
	}
	
	public List<ServiceConfigurationItem> findServiceConfigItemByName
	(List<ServiceConfigurationItem> configItems, String itemName) throws ValidationException {
		System.out.println("Entering findServiceConfigItemByName() method");
		if (Utils.isEmpty(itemName)) {
			return null;
		}	
		List<ServiceConfigurationItem> returnList = new ArrayList<ServiceConfigurationItem>();

		for (ServiceConfigurationItem item : configItems){
			if (configItemMatch(item, itemName)) {
				returnList.add(item);				
			}	
		}		
		return returnList;	
	}
	
	private boolean configItemMatch(ServiceConfigurationItem item, String name)	
	{
		if (item == null || Utils.isEmpty(name))
			return false;
		String itemName = item.getName();
		if (!Utils.isEmpty(itemName) && itemName.equals(name) ) 
			return true;
		else 			
			return false;		
	}	

	public ServiceConfigurationItem findOrCreateUnassignedConfigItems
	(ServiceConfigurationVersion scv, String tokenizedPath, boolean addUnreferitem) throws ValidationException { 
		System.out.println("findOrCreateConfigItemsCharValue............................................" + tokenizedPath);

		String[] tokens = null;
		tokens = tokenizedPath.trim().split("\\.");

		ServiceConfigurationItem rootConfigItem = (ServiceConfigurationItem) scv.getConfigItemTypeConfig();
		ServiceConfigurationItem parentConfigItem = rootConfigItem;
		ServiceConfigurationItem currentConfigItem = null;

		if (tokens.length > 1) {
			System.out.println("******entering for more than 1*****");
			for (int i = 0; i < tokens.length; i++) { 
				try {
					List<ServiceConfigurationItem> configItems = null;
					if(i>0) {
						System.out.println("Inside i>0"+tokens[i-1]+"::"+tokens[i]);

						configItems = findServiceConfigItemByName(parentConfigItem.getChildConfigItems(),  tokens[i]);

					} else {
						System.out.println("Inside else of i>0");
						configItems = findServiceConfigItemByName(scv.getConfigItems() ,tokens[i]);

					}
					if (Utils.checkNull(configItems) || configItems.isEmpty()) {					
						currentConfigItem = addChildConfigItem(scv, parentConfigItem, tokens[i]);	
						System.out.println("Inside configItems null"+currentConfigItem);
					}

					else if((!Utils.checkNull(configItems.get(0).getReference())||!Utils.checkNull(configItems.get(0).getAssignment())) && addUnreferitem) {
						currentConfigItem = addChildConfigItem(scv, parentConfigItem, tokens[i]);
						System.out.println("Inside configItems refernced"+currentConfigItem);
					}
					else {
						currentConfigItem = configItems.get(0);	
						System.out.println("Inside unrefernced configItems present"+currentConfigItem);
					}
					parentConfigItem = currentConfigItem;
					System.out.println("currentConfigItem::"+currentConfigItem);
				} catch (Exception e) { 
					throw new ValidationException(e);

				}

			}
		}
		return currentConfigItem;
	}

	public ServiceConfigurationItem addChildConfigItem(ServiceConfigurationVersion scv, ServiceConfigurationItem parentItem, String childItemName)
			throws ValidationException 
	{
		Finder finder = PersistenceHelper.makeFinder();
		try {
			if (scv == null || parentItem == null || Utils.isEmpty(childItemName))
				return null;		

			Specification childSpec = null;
			if (parentItem.getSpecification() != null) {
				Set<SpecificationRel> setOfRelatedSpecs = parentItem.getSpecification().getRelatedSpecs(); 
				if (!Utils.isEmpty(setOfRelatedSpecs)) {
					for (SpecificationRel relatedSpec : setOfRelatedSpecs) {
						childSpec = relatedSpec.getChild();
						if(childItemName.equals(childSpec.getName())) {
							break;
						}
					}
				}
			}

			// ensure we found something related in the list of more than one
			if (childSpec == null)
				log.validationException("commonTechPack.specRequired",  new java.lang.IllegalArgumentException(), childItemName);

			Collection<InventoryConfigurationItem> items = PersistenceHelper.makeConfigurationManager(scv.getClass()).
					createConfigurationItems(parentItem, (InventoryConfigurationSpec)childSpec, 1);

			ServiceConfigurationItem item = null;
			if (!Utils.isEmpty(items))
				item = (ServiceConfigurationItem)items.iterator().next(); 

			return item;
		}
		finally {
			if (finder != null)
				finder.close();
			RequestPolicyHelper.checkPolicy();
		}	
	}  
	
	public void assignResourceToConfigItem
	(ServiceConfigurationItem configItem, ConsumableResource childResource) 
			throws ValidationException {
		try{
			if (childResource == null || configItem == null)
				return;
			servConfMan.assignResource(configItem, childResource, configItem.getName(), configItem.getParentConfigItem().getName());		
			log.debug("", "Resource:" + childResource.getId()+ " got assigned.");
		} catch(Exception ex){
			System.out.println( "Exception occurred while assignResourceToConfigItem.");
			ex.printStackTrace();
		}
	}

	public void referenceResourceToConfigItem
	(ServiceConfigurationItem configItem, ConfigurationReferenceEnabled childResource) 
			throws ValidationException {
		try{
			if (childResource == null || configItem == null)
				return;
			if (!configItem.getReferencesMap().isEmpty()){
				Collection<ServiceConfigurationItem> derefConfItem = new ArrayList<ServiceConfigurationItem>();
				derefConfItem.add(configItem);
				servConfMan.dereferenceInventoryConfigurationItems(derefConfItem);
			}
			servConfMan.referenceEntity(configItem, childResource);		
			log.debug("", "Resource:" + childResource.getOid()+ " got referenced.");
		} catch(Exception ex){
			System.out.println( "Exception occurred while referenceResourceToConfigItem.");
			ex.printStackTrace();
		}
	}

	public void ammendReferenceToConfigItem(ServiceConfigurationItem configItem,
			ConfigurationReferenceEnabled childResource) {
		try{
			if (childResource == null || configItem == null)
				return;
			if (childResource.getEntityId() == configItem.getReference().getEntityId())
				return;
			List<ServiceConfigurationItem> configItems = new ArrayList<ServiceConfigurationItem>();
			configItems.add(configItem);
			servConfMan.dereferenceInventoryConfigurationItems(configItems);
			servConfMan.flushTransaction();
			servConfMan.referenceEntity(configItem, childResource);
			servConfMan.flushTransaction();
			log.debug("", "Resource:" + childResource.getOid()+ " got referenced.");
		} catch(Exception ex){
			System.out.println( "Exception occurred while referenceResourceToConfigItem.");
			ex.printStackTrace();
		}
	}

	public HashMap<String,List<ServiceConfigurationItem>> buildDictionaryWithServiceConfig(List<ServiceConfigurationItem> configItems) {
		HashMap<String,List<ServiceConfigurationItem>> configItemMap = null;
		if (configItems != null) {
			configItemMap = new HashMap<String,List<ServiceConfigurationItem>>();
			for (ServiceConfigurationItem serviceConfigurationItem : configItems) {
				String parentItemName = serviceConfigurationItem.getName();
				if(parentItemName!=null){
					ServiceConfigurationItem temp = serviceConfigurationItem;
					while (temp.getParentConfigItem() != null && temp.getParentConfigItem().getName()!=null) {
						temp = temp.getParentConfigItem();
						parentItemName = temp.getName() + "." + parentItemName;
					}
					if(configItemMap.get(parentItemName)==null)
						configItemMap.put(parentItemName, new ArrayList<ServiceConfigurationItem>());
					configItemMap.get(parentItemName).add(serviceConfigurationItem);
				}
			}
		}
		return configItemMap;
	}
	
	
	
	
	public void addUpdateConfigItemCharacteristic(ServiceConfigurationItem parentConfigItem, String characteristicName,
	        String value) throws ValidationException {
		try {
			CharacteristicManager charMan = PersistenceHelper.makeCharacteristicManager();
			if(parentConfigItem.getCharacteristicMap().containsKey(characteristicName)){
				ServiceConfigurationItemCharacteristic updatableChar = 
						(ServiceConfigurationItemCharacteristic) parentConfigItem.getCharacteristicMap().get(characteristicName);
				log.debug("", "Updating Config Item Characteristic "+characteristicName+" with value "+value);
				updatableChar.setValue(value);
			}
			else {
				CharacteristicSpecification charSpec = charMan.getCharacteristicSpecification(characteristicName);
				if (charSpec == null) 
					log.error("c2a.addUpdateConfigItemCharacteristic", characteristicName);
				log.debug("", "Creating Config Item Characteristic "+characteristicName+" with value "+value);
				CharValue e = parentConfigItem.makeCharacteristicInstance();
				e.setCharacteristicSpecification(charSpec);
				e.setName(characteristicName);
				e.setValue(value);
				e.setLabel(characteristicName);
				parentConfigItem.getCharacteristics().add((ServiceConfigurationItemCharacteristic) e);
			}
			charMan.validate(parentConfigItem);
		} catch (Exception er) {
			er.printStackTrace();

		}

	}

	public ServiceConfigurationItem findRootServiceConfigurationItem(ServiceConfigurationVersion version) {
		for(ServiceConfigurationItem item : version.getConfigItems())
			if(Utils.isBlank(item.getName()) && item.getParentConfigItem()==null)
				return item;
		return null;
	}

}
