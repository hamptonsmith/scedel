package sbsdl.statements;

import sbsdl.ParseLocation;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;

public class EvaluateStatement extends SkeletonStatement {
    private final Expression myExpression;
    
    public EvaluateStatement(ParseLocation l, Expression e) {
        super(l);
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
