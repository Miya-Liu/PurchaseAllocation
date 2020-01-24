/*
 * Copyright (C) 2008-2013, fluid Operations AG
 *
 * FedX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.uzh.ifi.tg.serviceMarket.fedx;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ch.uzh.ifi.tg.serviceMarket.fedx.statistics.StatisticsImpl;
import ch.uzh.ifi.tg.serviceMarket.fedx.util.EndpointFactory;
import ch.uzh.ifi.tg.serviceMarket.queryUtils.provider.ServiceDescription;
import org.apache.log4j.Logger;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.Sail;

import ch.uzh.ifi.tg.serviceMarket.fedx.cache.Cache;
import ch.uzh.ifi.tg.serviceMarket.fedx.cache.MemoryCache;
import ch.uzh.ifi.tg.serviceMarket.fedx.exception.FedXException;
import ch.uzh.ifi.tg.serviceMarket.fedx.statistics.Statistics;
import ch.uzh.ifi.tg.serviceMarket.fedx.statistics.StatisticsImpl;
import ch.uzh.ifi.tg.serviceMarket.fedx.structures.Endpoint;
import ch.uzh.ifi.tg.serviceMarket.fedx.util.EndpointFactory;

/**
 * FedX initialization factory methods for convenience: methods initialize the 
 * {@link FederationManager} and all required FedX structures. See {@link FederationManager}
 * for some a code snippet.
 * 
 * @author Andreas Schwarte
 *
 */
public class FedXFactory {

	protected static Logger log = Logger.getLogger(FedXFactory.class);
	
	
	
	/**
	 * Initialize the federation with the provided sparql endpoints. 
	 * 
	 * NOTE: {@link Config#initialize()} needs to be invoked before.
	 * 
	 * @param dataConfig
	 * 				the location of the data source configuration
	 * 
	 * @return
	 * 			the initialized FedX federation {@link Sail} wrapped in a {@link SailRepository}
	 * 
	 * @throws Exception
	 */
	public static SailRepository initializeSparqlFederation(List<String> sparqlEndpoints, ServiceDescription serviceDescription) throws Exception {

		List<Endpoint> endpoints = new ArrayList<Endpoint>();
		for (String url : sparqlEndpoints) {
			endpoints.add( EndpointFactory.loadSPARQLEndpoint(url));
		}
		return initializeFederation(endpoints, serviceDescription);
	}
	

	
	/**
	 * Initialize the federation with a specified data source configuration file (*.ttl). Federation members are 
	 * constructed from the data source configuration. Sample data source configuration files can be found in the documentation.
	 * 
	 * NOTE: {@link Config#initialize()} needs to be invoked before.
	 * 
	 * @param dataConfig
	 * 				the location of the data source configuration
	 * 
	 * @return
	 * 			the initialized FedX federation {@link Sail} wrapped in a {@link SailRepository}
	 * 
	 * @throws Exception
	 */
	public static SailRepository initializeFederation(String dataConfig, ServiceDescription serviceDescription) throws Exception {
		String cacheLocation = Config.getConfig().getCacheLocation();
		log.info("Loading federation members from dataConfig " + dataConfig + ".");
		List<Endpoint> members  = EndpointFactory.loadFederationMembers(new File(dataConfig));
		return initializeFederation(members, cacheLocation, serviceDescription);
	}
	
	
	
	/**
	 * Initialize the federation by providing information about the fedx configuration (c.f. {@link Config}
	 * for details on configuration parameters) and additional endpoints to add. The fedx configuration
	 * can provide information about the dataConfig to be used which may contain the default federation 
	 * members.
	 * 
	 * The Federation employs a {@link MemoryCache} which is located at {@link Config#getCacheLocation()}.
	 *  
	 * @param fedxConfig
	 * 			the location of the fedx configuration
	 * @param additionalEndpoints
	 * 			additional endpoints to be added, may be null or empty
	 *  
	 * @return
	 * 			the initialized FedX federation {@link Sail} wrapped in a {@link SailRepository}
	 * 
	 * @throws Exception
	 */
	public static SailRepository initializeFederation(String fedxConfig, List<Endpoint> additionalEndpoints, ServiceDescription serviceDescription) throws FedXException {
		if (!(new File(fedxConfig).exists()))
			throw new FedXException("FedX Configuration cannot be accessed at " + fedxConfig);
		Config.initialize(fedxConfig);
		return initializeFederation(additionalEndpoints, serviceDescription);
	}
	
	
	/**
	 * Initialize the federation by providing the endpoints to add. The fedx configuration can provide information
	 * about the dataConfig to be used which may contain the default federation  members.<p>
	 * 
	 * NOTE: {@link Config#initialize()} needs to be invoked before.
	 * 
	 * @param additionalEndpoints
	 * 			additional endpoints to be added, may be null or empty
	 *  
	 * @return
	 * 			the initialized FedX federation {@link Sail} wrapped in a {@link SailRepository}
	 * 
	 * @throws Exception
	 */
	public static SailRepository initializeFederation(List<Endpoint> endpoints, ServiceDescription serviceDescription) throws FedXException {
		
		String dataConfig = Config.getConfig().getDataConfig();
		String cacheLocation = Config.getConfig().getCacheLocation();
		List<Endpoint> members;		
		if (dataConfig==null) {
			if (endpoints!=null && endpoints.size()==0)
				log.warn("No dataConfig specified. Initializing federation without any preconfigured members.");
			members = new ArrayList<Endpoint>(5);
		} else {
			log.info("Loading federation members from dataConfig " + dataConfig + ".");
			members = EndpointFactory.loadFederationMembers(new File(dataConfig));
		}
		
		if (endpoints!=null)
			members.addAll(endpoints);
		
		return initializeFederation(members, cacheLocation, serviceDescription);
	}
	
	
	/**
	 * Helper method to initialize the federation with a {@link MemoryCache}.
	 * 
	 * @param members
	 * @param cacheLocation
	 * @return
	 */
	private static SailRepository initializeFederation(List<Endpoint> members, String cacheLocation, ServiceDescription serviceDescription) throws FedXException {

		Cache cache = new MemoryCache(cacheLocation);
		cache.initialize();
		Statistics statistics = new StatisticsImpl();
		
		return FederationManager.initialize(members, cache, statistics, serviceDescription);
	}
}
