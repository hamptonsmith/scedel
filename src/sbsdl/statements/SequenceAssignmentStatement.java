package sbsdl.statements;

import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;
import sbsdl.values.VProxy;
import sbsdl.values.VSeq;
import sbsdl.values.Value;

public class SequenceAssignmentStatement implements Statement {
    private final Expression myBase;
    private final Expression myIndex;
    private final Expression myValue;
    
    public SequenceAssignmentStatement(
            Expression base, Expression index, Expression value) {
        if (base.yeildsBakedLValues()) {
            throw new RuntimeException();
        }
        
        myBase = base;
        myIndex = index;
        myValue = value;
    }
    
    @Override
    public void execute(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        VSeq baseSeq = myBase.evaluate(h, s).assertIsSeq();
        int index = myIndex.evaluate(h, s).assertIsNumber()
                .assertNonNegativeReasonableInteger();
        
        Value newVal = myValue.evaluate(h, s);
        
        baseSeq.set(index, newVal.copy(
                VProxy.cannotContainProxyMessage(baseSeq.forbidsProxies())));
    }
}
