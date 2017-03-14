package sbsdl.statements;

import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;

public class ReturnStatement implements Statement {
    private final Expression myExpression;
    
    public ReturnStatement(Expression e) {
        myExpression = e;
    }
    
    @Override
    public void execute(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        s.setReturn(myExpression.evaluate(h, s));
    }
}
