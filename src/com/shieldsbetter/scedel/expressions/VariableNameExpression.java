package com.shieldsbetter.scedel.expressions;

import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.values.Value;

public class VariableNameExpression extends SkeletonExpression {
    private final Scedel.Symbol myVariableName;
    
    public VariableNameExpression(ParseLocation l, Scedel.Symbol name) {
        super(l);
        myVariableName = name;
    }
    
    @Override
    public Value evaluate(Scedel.HostEnvironment h, ScriptEnvironment s) {
        return s.lookupVariable(myVariableName);
    }

    @Override
    public boolean yeildsBakedLValues() {
        return myVariableName.isBaked();
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("EXP VAR LOOKUP: ");
        b.append(myVariableName);
        b.append("\n");
    }
}
