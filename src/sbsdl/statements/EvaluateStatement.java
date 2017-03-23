package sbsdl.statements;

import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;

public class EvaluateStatement implements Statement {
    private final Expression myExpression;
    
    public EvaluateStatement(Expression e) {
        myExpression = e;
    }

    @Override
    public void execute(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        myExpression.evaluate(h, s);
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("STMT EVALUATE\n");
        Util.indent(indentUnit, indentLevels + 1, b);
        myExpression.prettyRender(indentUnit, indentLevels + 1, b);
    }
}
