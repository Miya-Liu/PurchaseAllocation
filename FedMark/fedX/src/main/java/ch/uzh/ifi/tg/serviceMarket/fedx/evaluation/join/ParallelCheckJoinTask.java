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

package ch.uzh.ifi.tg.serviceMarket.fedx.evaluation.join;

import ch.uzh.ifi.tg.serviceMarket.fedx.algebra.CheckStatementPattern;
import ch.uzh.ifi.tg.serviceMarket.fedx.evaluation.FederationEvalStrategy;
import ch.uzh.ifi.tg.serviceMarket.fedx.evaluation.concurrent.ParallelExecutor;
import ch.uzh.ifi.tg.serviceMarket.fedx.evaluation.concurrent.ParallelTask;
import info.aduna.iteration.CloseableIteration;

import java.util.List;

import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;

import ch.uzh.ifi.tg.serviceMarket.fedx.algebra.CheckStatementPattern;
import ch.uzh.ifi.tg.serviceMarket.fedx.evaluation.FederationEvalStrategy;
import ch.uzh.ifi.tg.serviceMarket.fedx.evaluation.concurrent.ParallelExecutor;
import ch.uzh.ifi.tg.serviceMarket.fedx.evaluation.concurrent.ParallelTask;

/**
 * A task implementation representing a grouped bound check. See {@link FederationEvalStrategy#evaluateGroupedCheck(CheckStatementPattern, List)}
 * for further details. 
 * 
 * @author Andreas Schwarte
 */
public class ParallelCheckJoinTask implements ParallelTask<BindingSet> {
	
	protected final FederationEvalStrategy strategy;
	protected final CheckStatementPattern expr;
	protected final List<BindingSet> bindings;
	protected final ParallelExecutor<BindingSet> joinControl;
	
	public ParallelCheckJoinTask(ParallelExecutor<BindingSet> joinControl, FederationEvalStrategy strategy, CheckStatementPattern expr, List<BindingSet> bindings) {
		this.strategy = strategy;
		this.expr = expr;
		this.bindings = bindings;
		this.joinControl = joinControl;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> performTask() throws Exception {
		return strategy.evaluateGroupedCheck(expr, bindings);
	}

	@Override
	public ParallelExecutor<BindingSet> getControl() {
		return joinControl;
	}
}
