package sbsdl.statements;

import sbsdl.ExecutionException;
import sbsdl.InternalExecutionException;
import sbsdl.ParseLocation;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;
import sbsdl.values.VSeq;
import sbsdl.values.Value;

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
    public void execute(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
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
