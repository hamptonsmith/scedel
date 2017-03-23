package sbsdl.expressions;

import java.util.LinkedList;
import java.util.List;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.statements.Statement;
import sbsdl.values.VSeq;
import sbsdl.values.Value;

public class SequenceExpression implements Expression {
    private final List<Expression> myExpressionElements;
    
    public SequenceExpression(List<Expression> elements) {
        myExpressionElements = new LinkedList<>(elements);
    }
    
    @Override
    public Value evaluate(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        VSeq seq = new VSeq();
        for (Expression e : myExpressionElements) {
            seq.enqueue(e.evaluate(h, s));
        }
        return seq;
    }

    @Override
    public boolean yeildsBakedLValues() {
        return false;
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("EXP SEQUENCE\n");
        for (Expression e : myExpressionElements) {
            Statement.Util.indent(indentUnit, indentLevels + 1, b);
            e.prettyRender(indentUnit, indentLevels + 1, b);
        }
    }
}
