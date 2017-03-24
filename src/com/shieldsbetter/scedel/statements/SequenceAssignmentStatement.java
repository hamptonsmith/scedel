package com.shieldsbetter.scedel.statements;

import com.shieldsbetter.scedel.ExecutionException;
import com.shieldsbetter.scedel.InternalExecutionException;
import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.expressions.Expression;
import com.shieldsbetter.scedel.values.VSeq;
import com.shieldsbetter.scedel.values.Value;

public class SequenceAssignmentStatement extends SkeletonStatement {
    private final Expression myBase;
    private final Expression myIndex;
    private final Expression myValue;
    
    public SequenceAssignmentStatement(ParseLocation l, Expression base,
            Expression index, Expression value) {
        super(l);
        if (base.yeildsBakedLValues()) {
            throw new RuntimeException();
        }
        
        myBase = base;
        myIndex = index;
        myValue = value;
    }
    
    @Override
    public void execute(Scedel.HostEnvironment h, ScriptEnvironment s) {
        VSeq baseSeq = myBase.evaluate(h, s).assertIsSeq(
                myBase.getParseLocation());
        int index = myIndex.evaluate(h, s).assertIsNumber(
                myIndex.getParseLocation())
                .assertNonNegativeReasonableInteger(myIndex.getParseLocation(),
                        "index");
        
        Value newVal = myValue.evaluate(h, s);
        
        ExecutionException proxyGuard;
        if (baseSeq.forbidsProxies()) {
            proxyGuard = InternalExecutionException.illegalProxyContainment(
                    getParseLocation()).getExecutionException();
        }
        else {
            proxyGuard = null;
        }
        
        baseSeq.set(index, newVal.copy(proxyGuard));
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("STMT ASSIGN TO SEQUENCE INDEX\n");
        Util.labeledChild(indentUnit, indentLevels, "base:", myBase, b);
        Util.labeledChild(indentUnit, indentLevels, "index:", myIndex, b);
        Util.labeledChild(indentUnit, indentLevels, "expression to assign:",
                myValue, b);
    }
}
