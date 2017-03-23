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

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("STMT RETURN\n");
        myExpression.prettyRender(indentUnit, indentLevels + 1, b);
    }
}
