package aiku.provision.cfs.worker;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlbeans.SimpleValue;

import oracle.communications.inventory.api.entity.BusinessInteraction;
import oracle.communications.inventory.api.entity.BusinessInteractionAttachment;
import oracle.communications.inventory.api.entity.DeviceInterface;
import oracle.communications.inventory.api.entity.LogicalDevice;
import oracle.communications.inventory.api.entity.PhysicalDevice;
import oracle.communications.inventory.api.entity.PhysicalDevicePhysicalDeviceRel;
import oracle.communications.inventory.api.entity.PhysicalPort;
import oracle.communications.inventory.api.exception.ValidationException;
import oracle.communications.inventory.api.framework.logging.Log;
import oracle.communications.inventory.api.framework.logging.LogFactory;
import oracle.communications.inventory.api.framework.resource.MessageResource;
import oracle.communications.inventory.api.util.Utils;
import oracle.communications.inventory.xmlbeans.BusinessInteractionType;
import oracle.communications.inventory.xmlbeans.EntityType;
import oracle.communications.inventory.xmlbeans.GeographicAddressType;
import oracle.communications.inventory.xmlbeans.IPSubnetType;
import oracle.communications.inventory.xmlbeans.InteractionDocument;
import oracle.communications.inventory.xmlbeans.ParameterType;
import oracle.communications.inventory.xmlbeans.PartyType;
import oracle.communications.inventory.xmlbeans.PhysicalDeviceType;
import oracle.communications.inventory.xmlbeans.PhysicalPortType;
import oracle.communications.inventory.xmlbeans.PropertyType;
import oracle.communications.platform.persistence.Finder;

public class xmlbeanWorker {
private static Log log = LogFactory.getLog(xmlbeanWorker.class);
	
	Finder find;
	
	public xmlbeanWorker(Finder finder){
		find = finder;
		parameterMap = new HashMap<String, String>();
		deviceMap = new HashMap<String, PhysicalDeviceType>();
		logicalResourceMap = new HashMap<String, IPSubnetType>();
	}

	public PropertyType findPropertyByName
			(String propertyName, List<PropertyType> propertyList) {
		for(PropertyType property : propertyList)
			if(propertyName.equals(property.getName()))
					return property;
		return null;
	}
	
	public LogicalDevice findLogicalDeviceByEntity(PhysicalDeviceType deviceType) throws ValidationException {
		Collection<LogicalDevice> logDevResults = null;
		if(!Utils.isBlank(deviceType.getName()))
			logDevResults = find.findByName(LogicalDevice.class, deviceType.getName());
		else
			throw new ValidationException("The Name is necessary to find a Logical Device");
		if(Utils.isEmpty(logDevResults)) {
			find.reset();
			return null;
		}
		LogicalDevice deviceResult=logDevResults.iterator().next();
		find.reset();
		return deviceResult;
	}
	
	public PhysicalDevice findPhysicalDeviceByEntity(PhysicalDeviceType deviceType) throws ValidationException {
		Collection<PhysicalDevice> physDevResults = null;
		if(!Utils.isBlank(deviceType.getName()))
			physDevResults = find.findByName(PhysicalDevice.class, deviceType.getName());
		else
			throw new ValidationException("The Name is necessary to find a Physical Device");
		if(Utils.isEmpty(physDevResults)) {
			find.reset();
			return null;
		}
		PhysicalDevice deviceResult=physDevResults.iterator().next();
		find.reset();
		return deviceResult;
	}
	
	public PhysicalPortType findPhysicalPortType(String portType, List<EntityType> inputPorts){
		for(EntityType physicalPort: inputPorts)
			if(portType.equals(physicalPort.getEntityNote()))
				return (PhysicalPortType) physicalPort;
		return null;
	}
	
	public PhysicalPort findPortByEntity(PhysicalPortType portType) throws ValidationException {
		Collection<PhysicalPort> physPortResults = null;
		if(!Utils.isBlank(portType.getId()))
			physPortResults = find.findById(PhysicalPort.class, portType.getId());
		else
			throw new ValidationException("The ID is necessary to find a UIM indexed port");
		if(Utils.isEmpty(physPortResults)) {
			find.reset();
			return null;
		}
		PhysicalPort resultPort=physPortResults.iterator().next();
		find.reset();
		return resultPort;
	}
	
	public DeviceInterface findDevIntByEntity(PhysicalPortType portType) throws ValidationException {
		Collection<DeviceInterface> physPortResults = null;
		if(!Utils.isBlank(portType.getId()))
			physPortResults = find.findById(DeviceInterface.class, portType.getId());
		else
			throw new ValidationException("The ID is necessary to find a UIM indexed port");
		if(Utils.isEmpty(physPortResults)) {
			find.reset();
			return null;
		}
		DeviceInterface resultPort=physPortResults.iterator().next();
		find.reset();
		return resultPort;
	}

	public PhysicalPort findMappedPortByName(LogicalDevice peDevice, String pePortName) {
		for(DeviceInterface inter : peDevice.getDeviceInterfaces()){
			if(Utils.isEmpty(inter.getMappedPhysicalPorts()))
				continue;
			if(pePortName.equalsIgnoreCase(inter.getMappedPhysicalPorts().iterator().next().getName()))
				return inter.getMappedPhysicalPorts().iterator().next();
		}
		return null;
	}
	
	public DeviceInterface findChildDevIntByName(LogicalDevice peDevice, String pePortName) {
		for(DeviceInterface inter : peDevice.getDeviceInterfaces()){
			if(pePortName.equalsIgnoreCase(inter.getName()))
				return inter;
		}
		return null;
	}
	
	public BusinessInteractionType parseLatestAttachmentFromBi(BusinessInteraction feasibilityBi) throws ValidationException {
		List<BusinessInteractionAttachment> biAttachmentList = feasibilityBi.getAttachments();
		BusinessInteractionAttachment biAttachment = null;
		if (!Utils.isEmpty(biAttachmentList))
			biAttachment = biAttachmentList.get(biAttachmentList.size() - 1);
		if (biAttachment == null)
			throw new ValidationException(new String(MessageResource.getMessage("businessInteraction.entityAttachmentError",feasibilityBi.getDisplayInfo())));
		try {
			String textualContent = biAttachment.convertContentToString();
			return InteractionDocument.Factory.parse(textualContent).getInteraction();
		} catch (Exception e) {
			log.error("c2a.getServiceForBusinessInteraction.entityAttachmentError", e, feasibilityBi.getId());
			throw new ValidationException(e);
		}		
	}
	
	public void parseParameterList(List<ParameterType> paramList) throws ValidationException {
		for(ParameterType parameter : paramList) {
			if(parameter.getValue() instanceof PartyType)
				subscriber = (PartyType) parameter.getValue();
			else if(parameter.getValue() instanceof GeographicAddressType)
				serviceAddress = (GeographicAddressType) parameter.getValue();
			else if(parameter.getValue() instanceof PhysicalDeviceType)
				deviceMap.put(parameter.getName(), (PhysicalDeviceType) parameter.getValue());
			else if(parameter.getValue() instanceof IPSubnetType)
				logicalResourceMap.put(parameter.getName(), (IPSubnetType) parameter.getValue());
			else
				parameterMap.put(parameter.getName(), ((SimpleValue) parameter.getValue()).getStringValue());
		}
	}
	
	public Map<String, String> populatePropertyMap(List<PropertyType> propertyList) {
		HashMap<String, String> mappedProperties = new HashMap<String, String>();
		for(PropertyType property : propertyList)
			mappedProperties.put(property.getName(), property.getValue());
		return mappedProperties;
	}

	PartyType subscriber;
	GeographicAddressType serviceAddress;
	Map<String, String> parameterMap;
	Map<String, PhysicalDeviceType> deviceMap;
	Map<String, IPSubnetType> logicalResourceMap;
	
	public Map<String, String> getParameterMap() {return parameterMap;}
	public Map<String, PhysicalDeviceType> getDeviceMap() {return deviceMap;}
	public Map<String, IPSubnetType> getLogicalResourceMap() {return logicalResourceMap;}

	public PartyType getSubscriber() {return subscriber;}
	public GeographicAddressType getServiceAddress() {return serviceAddress;}

	public PhysicalPort findStpPortByName(PhysicalDevice stpDevice, String portName) {
		for(PhysicalDevicePhysicalDeviceRel cardRel : stpDevice.getChildPhysicalDevices()){
			PhysicalDevice card = cardRel.getChildPhysicalDevice();
			if("ODP_Panel".equals(card.getSpecification().getName()))
				return findNtePortByName(card, portName);
		}
		return null;
	}

	public PhysicalPort findNtePortByName(PhysicalDevice device, String portName) {
		for(PhysicalPort port : device.getPhysicalPorts())
			if(portName.equals(port.getName()))
				return port;
		return null;
	}
	
}
