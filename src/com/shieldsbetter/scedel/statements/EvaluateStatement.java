package com.shieldsbetter.scedel.statements;

import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.expressions.Expression;

public class EvaluateStatement extends SkeletonStatement {
    private final Expression myExpression;
    
    public EvaluateStatement(ParseLocation l, Expression e) {
        super(l);
        myExpression = e;
    }

    @Override
    public void execute(Scedel.HostEnvironment h, ScriptEnvironment s) {
        myExpression.evaluate(h, s);
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("STMT EVALUATE\n");
        Util.indent(indentUnit, indentLevels + 1, b);
        myExpression.prettyRender(indentUnit, indentLevels + 1, b);
    }

    @Override
    public void accept(Visitor v) {
        v.visitEvaluateStatement(this);
    }
}
