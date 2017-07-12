package com.shieldsbetter.scedel.statements;

import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.expressions.Expression;

public class ReturnStatement extends SkeletonStatement {
    private final Expression myExpression;
    
    public ReturnStatement(ParseLocation l, Expression e) {
        super(l);
        myExpression = e;
    }
    
    public Expression getExpression() {
        return myExpression;
    }
    
    @Override
    public void execute(Scedel.HostEnvironment h, ScriptEnvironment s) {
        s.setReturn(myExpression.evaluate(h, s));
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("STMT RETURN\n");
        Statement.Util.indent(indentUnit, indentLevels + 1, b);
        myExpression.prettyRender(indentUnit, indentLevels + 1, b);
    }

    @Override
    public void accept(Visitor v) {
        v.visitReturnStatement(this);
    }
}
