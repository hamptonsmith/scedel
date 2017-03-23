package sbsdl.statements;

import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;
import sbsdl.values.VDict;
import sbsdl.values.VProxy;
import sbsdl.values.Value;

public class FieldAssignmentStatement implements Statement {
    private final Expression myBase;
    private final Expression myField;
    private final Expression myValue;
    
    public FieldAssignmentStatement(
            Expression base, Expression field, Expression value) {
        if (base.yeildsBakedLValues()) {
            throw new RuntimeException();
        }
        
        myBase = base;
        myField = field;
        myValue = value;
    }
    
    @Override
    public void execute(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        VDict baseDict = myBase.evaluate(h, s).assertIsDict();
        Value field = myField.evaluate(h, s);
        
        Value newVal = myValue.evaluate(h, s);
        
        baseDict.put(field, newVal.copy(
                VProxy.cannotContainProxyMessage(baseDict.forbidsProxies())));
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
