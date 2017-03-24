package sbsdl.statements;

import sbsdl.ExecutionException;
import sbsdl.InternalExecutionException;
import sbsdl.ParseLocation;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;
import sbsdl.values.VDict;
import sbsdl.values.Value;

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
    
    @Override
    public void execute(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        VDict baseDict = myBase.evaluate(h, s).assertIsDict(
                myBase.getParseLocation());
        Value field = myField.evaluate(h, s);
        
        Value newVal = myValue.evaluate(h, s);
        
        ExecutionException proxyGuard;
        if (baseDict.forbidsProxies()) {
            proxyGuard = InternalExecutionException.illegalProxyContainment(
                    getParseLocation()).getExecutionException();
        }
        else {
            proxyGuard = null;
        }
        
        baseDict.put(field, newVal.copy(proxyGuard));
    }

    @Override
    public void prettyRender(int indentUnit, int indentLevels, StringBuilder b) {
        b.append("STMT ASSIGN TO FIELD\n");
        Util.labeledChild(indentUnit, indentLevels, "base:", myBase, b);
        Util.labeledChild(indentUnit, indentLevels, "field:", myField, b);
        Util.labeledChild(indentUnit, indentLevels, "expression to assign:",
                myValue, b);
    }
}
