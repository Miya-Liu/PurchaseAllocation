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

import ch.uzh.ifi.tg.serviceMarket.fedx.Config;
import ch.uzh.ifi.tg.serviceMarket.fedx.Config;

public class MonitoringFactory
{

	/**
	 * Create a new monitoring instance depending on 
	 * {@link Config#isEnableMonitoring()}
	 * 
	 * @return
	 */
	public static Monitoring createMonitoring() {
		
		if (Config.getConfig().isEnableMonitoring())
			return new MonitoringImpl();
		return new NoopMonitoringImpl();
	}
}
