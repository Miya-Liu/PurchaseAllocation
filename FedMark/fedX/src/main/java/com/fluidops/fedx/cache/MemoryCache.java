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

package com.fluidops.fedx.cache;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.EmptyIteration;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.openrdf.model.Statement;

import com.fluidops.fedx.exception.EntryAlreadyExistsException;
import com.fluidops.fedx.exception.EntryUpdateException;
import com.fluidops.fedx.exception.FedXException;
import com.fluidops.fedx.exception.FedXRuntimeException;
import com.fluidops.fedx.structures.Endpoint;
import com.fluidops.fedx.structures.SubQuery;


/**
 * A simple implementation of a Main memory cache which is persisted to the provided cache location.
 * 
 * Currently only binary provenance information is maintained.
 * 
 * @author Andreas Schwarte
 *
 */
public class MemoryCache implements Cache {

	public static Logger log = Logger.getLogger(MemoryCache.class);
	
	protected HashMap<SubQuery, CacheEntry> cache = new HashMap<SubQuery, CacheEntry>();
	protected String cacheLocation;
	
	public MemoryCache(String cacheLocation) {
		if (cacheLocation==null)
			throw new FedXRuntimeException("The provided cacheLocation must not be null.");
		this.cacheLocation = cacheLocation;
	}
	
	@Override
	public void addEntry(SubQuery subQuery, CacheEntry cacheEntry) throws EntryAlreadyExistsException {

		synchronized (cache) {
			
			if (cache.containsKey(subQuery))
				throw new EntryAlreadyExistsException("Entry for statement " + subQuery + " already exists in cache. Use update functionality instead.");
		
			cache.put(subQuery, cacheEntry);
		}
		
	}
	
	
	@Override
	public void updateEntry(SubQuery subQuery, CacheEntry merge) throws EntryUpdateException {
		
		synchronized (cache) {
			
			CacheEntry entry = cache.get(subQuery);
			
			if (entry==null)
				cache.put(subQuery, merge);
			else
				entry.merge(merge);
		}
		
	}


	@Override
	public void removeEntry(SubQuery subQuery) throws EntryUpdateException {
		
		synchronized (cache) {
			cache.remove(subQuery);
		}
		
	}
	
	@Override
	public StatementSourceAssurance canProvideStatements(SubQuery subQuery, Endpoint endpoint) {
		CacheEntry entry = cache.get(subQuery);
		if (entry == null)
			return StatementSourceAssurance.POSSIBLY_HAS_STATEMENTS;
		if (entry.hasLocalStatements(endpoint))
			return StatementSourceAssurance.HAS_LOCAL_STATEMENTS;
		return entry.canProvideStatements(endpoint);
	}

	
	@Override
	public CacheEntry getCacheEntry(SubQuery subQuery) {
		CacheEntry entry = cache.get(subQuery);
		// TODO use clone or some copy/wrapping method to have read only capability
		return entry;
	}

	@Override
	public CloseableIteration<? extends Statement, Exception> getStatements(
			SubQuery subQuery) {
		CacheEntry entry = cache.get(subQuery);
		return entry == null ? new EmptyIteration<Statement, Exception>() : entry.getStatements();
	}

	@Override
	public CloseableIteration<? extends Statement, Exception> getStatements(
			SubQuery subQuery, Endpoint endpoint) {
		CacheEntry entry = cache.get(subQuery);
		return entry == null ? new EmptyIteration<Statement, Exception>() : entry.getStatements(endpoint);
	}

	@Override
	public List<Endpoint> hasLocalStatements(SubQuery subQuery) {
		CacheEntry entry = cache.get(subQuery);
		return entry == null ? Collections.<Endpoint>emptyList() : entry.hasLocalStatements();
	}

	@Override
	public boolean hasLocalStatements(SubQuery subQuery, Endpoint endpoint) {
		CacheEntry entry = cache.get(subQuery);
		return entry == null ? false : entry.hasLocalStatements(endpoint);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize() throws FedXException {
		// Do not use persistent cache
		return;
		/*File f = new File(cacheLocation);
		
		if (!f.exists())
			return;
		try {
			ObjectInputStream in = new ObjectInputStream( new BufferedInputStream( new FileInputStream(f)));
			cache = (HashMap<SubQuery, CacheEntry>)in.readObject();
			in.close();
		} catch (Exception e) {
			throw new FedXException("Error initializing cache.", e);
		}*/
	}

	@Override
	public void invalidate() throws FedXException {
		; 	// no-op		
	}

	@Override
	public void persist() throws FedXException {
		
		// XXX write to a temporary file first, to prevent a corrupt file
		
		/*try {
			ObjectOutputStream out = new ObjectOutputStream( new BufferedOutputStream( new FileOutputStream( new File(cacheLocation))));
			out.writeObject(this.cache);
			out.flush();
			out.close();	
		} catch (Exception e) {
			throw new FedXException("Error persisting cache data.", e);
		}*/
	}

	@Override
	public void clear() {
		log.info("Clearing the cache.");
		cache = new HashMap<SubQuery, CacheEntry>();
		
	}




}
