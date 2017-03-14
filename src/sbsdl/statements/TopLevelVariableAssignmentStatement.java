package sbsdl.statements;

import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;

public class TopLevelVariableAssignmentStatement implements Statement {
    private final String myVarName;
    private final Expression myExpression;
    
    public TopLevelVariableAssignmentStatement(
            String varName, Expression value) {
        myVarName = varName;
        myExpression = value;
    }

    @Override
    public void execute(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        s.assignValue(myVarName, myExpression.evaluate(h, s));
    }
}
