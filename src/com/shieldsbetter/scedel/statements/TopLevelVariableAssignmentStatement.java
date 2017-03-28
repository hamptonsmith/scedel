package com.shieldsbetter.scedel.statements;

import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.expressions.Expression;

public class TopLevelVariableAssignmentStatement extends SkeletonStatement {
    private final Scedel.Symbol myVarName;
    private final Expression myExpression;
    
    public TopLevelVariableAssignmentStatement(ParseLocation l,
            Scedel.Symbol varName, Expression value) {
        super(l);
        if (varName.isBaked()) {
            throw new RuntimeException();
        }
        
        myVarName = varName;
        myExpression = value;
    }

    public Scedel.Symbol getSymbol() {
        return myVarName;
    }
    
    public Expression getAssigned() {
        return myExpression;
    }
    
    @Override
    public void execute(Scedel.HostEnvironment h, ScriptEnvironment s) {
        s.assignValue(myVarName, myExpression.evaluate(h, s).copy(null));
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("STMT ASSIGN TO TOP LEVEL VARIABLE\n");
        Util.indent(indentUnit, indentLevels + 1, b);
        b.append("var: ");
        b.append(myVarName);
        b.append("\n");
        Util.labeledChild(indentUnit, indentLevels, "expression to assign:",
                myExpression, b);
    }

    @Override
    public void accept(Visitor v) {
        v.visitTopLevelVariableAssignmentStatement(this);
    }
}
