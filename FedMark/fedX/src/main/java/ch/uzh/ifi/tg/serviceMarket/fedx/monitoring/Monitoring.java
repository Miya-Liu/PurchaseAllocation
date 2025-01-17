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

package ch.uzh.ifi.tg.serviceMarket.fedx.monitoring;

import ch.uzh.ifi.tg.serviceMarket.fedx.structures.Endpoint;
import org.openrdf.query.algebra.TupleExpr;

import ch.uzh.ifi.tg.serviceMarket.fedx.structures.Endpoint;
import ch.uzh.ifi.tg.serviceMarket.fedx.structures.QueryInfo;

public interface Monitoring
{

	public void monitorRemoteRequest(Endpoint e);
	
	public void resetMonitoringInformation();

	public void monitorQuery(QueryInfo query);
	
	public void logQueryPlan(TupleExpr tupleExpr);
}
