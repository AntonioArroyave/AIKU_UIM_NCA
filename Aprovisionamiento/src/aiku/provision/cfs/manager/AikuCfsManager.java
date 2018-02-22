package aiku.provision.cfs.manager;

import oracle.communications.inventory.extensibility.extension.util.ExtensionPointContext;
import oracle.communications.inventory.xmlbeans.BusinessInteractionItemType;
import oracle.communications.platform.persistence.Finder;
import oracle.communications.platform.persistence.PersistenceHelper;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import aiku.provision.cfs.worker.configurationWorker;
import aiku.provision.cfs.worker.serviceWorker;
import aiku.provision.cfs.worker.xmlbeanWorker;
import oracle.communications.inventory.api.businessinteraction.BusinessInteractionManager;
import oracle.communications.inventory.api.entity.ServiceConfigurationItem;
import oracle.communications.inventory.api.entity.ServiceConfigurationVersion;
import oracle.communications.inventory.api.exception.ValidationException;
import oracle.communications.inventory.api.framework.logging.Log;
import oracle.communications.inventory.api.framework.logging.LogFactory;

public class AikuCfsManager {
	protected Log log = LogFactory.getLog(AikuCfsManager.class);
	private ExtensionPointContext context = null;
	Finder find;
	
	private static enum BI_ACTIONS{
		CREATE("CREATE"), CHANGE("CHANGE");
		BI_ACTIONS(String input){};
	}
	
	public AikuCfsManager(ExtensionPointContext context, Log log){
		this.log = log;
		this.context = context;
	}
	public void processCfsServiceConfiguration(ServiceConfigurationVersion scv) throws ValidationException {
		log.debug("", "Begin of processCfsServiceConfiguration");
		 List<ServiceConfigurationItem> configItems = scv.getConfigItems();
		 BusinessInteractionItemType item = (BusinessInteractionItemType) context.getArguments()[1];
		 String serviceAction = null;;
		if(item != null){
				serviceAction = item.getService().getAction().toUpperCase();
		}
		
		 switch(BI_ACTIONS.valueOf(serviceAction)){
			case CREATE:
				createCFSConfiguration(scv,item);
				break;
			case CHANGE:
				changeCFSConfiguration(scv,item);
				break;
			default:  
				throw new ValidationException("Invalid BI Action");			
		 }
		 log.debug("", "End of processCfsServiceConfiguration");
	}
	
	 private void changeCFSConfiguration(ServiceConfigurationVersion scv,BusinessInteractionItemType itemType) {
		 log.debug("", "Begin of changeCFSConfiguration");
		 log.debug("", "End of changeCFSConfiguration");
	}
	
	 private void createCFSConfiguration(ServiceConfigurationVersion scv, BusinessInteractionItemType xmlEntrada) {
		log.debug("", "Processing CreateCFSConfiguration : Service Name :"+scv.getService().getExternalObjectId() +" ");
		find = PersistenceHelper.makeFinder();
		BusinessInteractionManager biManager = PersistenceHelper.makeBusinessInteractionManager();
		biManager.switchContext(scv, null);
		configurationWorker confWorker = new configurationWorker();
		serviceWorker servWorker = new serviceWorker(find);
		xmlbeanWorker xmlWorker = new xmlbeanWorker(find);
		try {
		//¿Cual es la ventaja de hacerlo en dos métodos separads
		xmlWorker.parseParameterList(xmlEntrada.getParameterList()); // Los parsea solo para almacenarlo en el worker?
		Map<String,String> parameterMap = xmlWorker.getParameterMap(); // y que luego sean retornados por ete método R: es para poder retornar partes seperadas
		
		String bandwith = parameterMap.get("bandwidth");
		log.debug("", "bandwidth recovered:"+ bandwith);

		ServiceConfigurationItem rootItem = confWorker.findRootServiceConfigurationItem(scv);
		
		if(bandwith != null){
		confWorker.addUpdateConfigItemCharacteristic(rootItem, "bandwidth", bandwith);
		log.debug("", "updateConfigItemChracteristic bandwith");
		}
		biManager.flushTransaction();
		} catch (ValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 //Poblar una caracteristica del service configuration
//		 xmlEntrada.getParameterList()
//		 scv.setCharacteristics(arg0);
		 //Si hay RFS agregar una BI Hija
		log.debug("", "End of createCFSConfiguration");
	}

}
	