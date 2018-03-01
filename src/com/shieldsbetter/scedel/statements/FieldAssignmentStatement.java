package com.shieldsbetter.scedel.statements;

import com.shieldsbetter.scedel.InternalExecutionException;
import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.expressions.Expression;
import com.shieldsbetter.scedel.values.VDict;
import com.shieldsbetter.scedel.values.Value;

public class FieldAssignmentStatement extends SkeletonStatement {
    private final Expression myBase;
    private final Expression myField;
    private final Expression myValue;
    
    public FieldAssignmentStatement(ParseLocation l, Expression base,
            Expression field, Expression value) {
        super(l);
        if (base.yeildsBakedLValues()) {
            throw new RuntimeException();
        }
        
        myBase = base;
        myField = field;
        myValue = value;
    }
    
    public Expression getBase() {
        return myBase;
    }
    
    public Expression getField() {
        return myField;
    }
    
    public Expression getAssigned() {
        return myValue;
    }
    
    @Override
    public void execute(Scedel.HostEnvironment h, ScriptEnvironment s) {
        VDict baseDict = myBase.evaluate(h, s).assertIsDict(
                myBase.getParseLocation());
        Value field = myField.evaluate(h, s);
        
        Value newVal = myValue.evaluate(h, s);
        
        try {
            baseDict.put(field, newVal.copy(baseDict.forbidsProxies()));
        }
        catch (Value.CannotCopyVProxyException ccvpe) {
            throw InternalExecutionException.illegalProxyContainment(
                    getParseLocation());
        }
    }

    @Override
    public void prettyRender(int indentUnit, int indentLevels, StringBuilder b) {
        b.append("STMT ASSIGN TO FIELD\n");
        Util.labeledChild(indentUnit, indentLevels, "base:", myBase, b);
        Util.labeledChild(indentUnit, indentLevels, "field:", myField, b);
        Util.labeledChild(indentUnit, indentLevels, "expression to assign:",
                myValue, b);
    }

    @Override
    public void accept(Visitor v) {
        v.visitFieldAssignmentStatement(this);
    }
}
