package ch.uzh.ifi.tg.serviceMarket.queryUtils.provider;

import java.util.List;

/**
 * Created by Tobias Grubenmann on 10/04/16.
 */
public class StatementAndProviderVars {
    private StringBuilder statement;
    private List<String> vars;

    public StatementAndProviderVars(StringBuilder statement, List<String> vars) {
        this.statement = statement;
        this.vars = vars;
    }

    public StringBuilder getStatement() {
        return statement;
    }

    public List<String> getVars() {
        return vars;
    }
}
