package sbsdl.statements;

import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;

public class TopLevelVariableAssignmentStatement implements Statement {
    private final Sbsdl.Symbol myVarName;
    private final Expression myExpression;
    
    public TopLevelVariableAssignmentStatement(
            Sbsdl.Symbol varName, Expression value) {
        myVarName = varName;
        myExpression = value;
    }

    @Override
    public void execute(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        s.assignValue(myVarName, myExpression.evaluate(h, s).copy(null));
    }
}
