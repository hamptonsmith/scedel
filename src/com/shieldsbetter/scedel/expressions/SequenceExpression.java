package com.shieldsbetter.scedel.expressions;

import java.util.LinkedList;
import java.util.List;
import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.statements.Statement;
import com.shieldsbetter.scedel.values.VSeq;
import com.shieldsbetter.scedel.values.Value;

public class SequenceExpression extends SkeletonExpression {
    private final List<Expression> myExpressionElements;
    
    public SequenceExpression(ParseLocation l, List<Expression> elements) {
        super(l);
        myExpressionElements = new LinkedList<>(elements);
    }
    
    @Override
    public Value evaluate(Scedel.HostEnvironment h, ScriptEnvironment s) {
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
