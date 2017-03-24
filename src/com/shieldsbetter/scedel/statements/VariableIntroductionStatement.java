package com.shieldsbetter.scedel.statements;

import com.shieldsbetter.scedel.ExecutionException;
import com.shieldsbetter.scedel.InternalExecutionException;
import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.expressions.Expression;import com.shieldsbetter.scedel.values.Value;

public class VariableIntroductionStatement extends SkeletonStatement {
    private final Scedel.Symbol myName;
    private final Expression myInitialValue;
    
    public VariableIntroductionStatement(
            ParseLocation l, Scedel.Symbol name, Expression initialValue) {
        super(l);
        myName = name;
        myInitialValue = initialValue;
    }
    
    @Override
    public void execute(Scedel.HostEnvironment h, ScriptEnvironment s) {
        Value initialValue = myInitialValue.evaluate(h, s);
        
        ExecutionException proxyGuard;
        if (myName.isBaked()) {
            proxyGuard = InternalExecutionException.cannotBakeProxy(
                    getParseLocation()).getExecutionException();
        }
        else {
            proxyGuard = null;
        }
        
        s.introduceSymbol(myName, initialValue.copy(proxyGuard));
    }
    
    private String cannotContainProxyMessage(boolean bake, Value v) {
        String result;
        if (bake) {
            result = "Cannot bake a value containing a proxy object: " + v;
        }
        else {
            result = null;
        }
        return result;
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("STMT DIMENSIONALIZE VARIABLE (");
        
        if (myName.isBaked()) {
            b.append("baked");
        }
        else {
            b.append("not baked");
        }
        
        b.append(")\n");
        
        Util.indent(indentUnit, indentLevels + 1, b);
        b.append("var: ");
        b.append(myName);
        b.append("\n");
        
        Util.labeledChild(indentUnit, indentLevels, "initial value expression:",
                myInitialValue, b);
    }

    @Override
    public void accept(Visitor v) {
        v.visitVariableIntroductionStatement(this);
    }
}
