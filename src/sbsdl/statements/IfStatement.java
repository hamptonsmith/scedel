package sbsdl.statements;

import sbsdl.ParseLocation;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;

public class IfStatement extends SkeletonStatement {
    private final Expression myCondition;
    private final Statement myOnTrue;
    private final Statement myOnFalse;
    
    public IfStatement(ParseLocation l, Expression condition, Statement onTrue,
            Statement onFalse) {
        super(l);
        myCondition = condition;
        myOnTrue = onTrue;
        myOnFalse = onFalse;
    }
    
    @Override
    public void execute(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        boolean conditionResult =
                myCondition.evaluate(h, s).assertIsBoolean(
                        myCondition.getParseLocation()).getValue();
        
        if (conditionResult) {
            myOnTrue.execute(h, s);
        }
        else {
            myOnFalse.execute(h, s);
        }
    }

    @Override
    public void prettyRender(int indentUnit, int indentLevels, StringBuilder b) {
        b.append("STMT IF\n");
        Util.labeledChild(
                indentUnit, indentLevels, "condition:", myCondition, b);
        Util.labeledChild(indentUnit, indentLevels, "if code:", myOnTrue, b);
        Util.labeledChild(
                indentUnit, indentLevels, "else code:", myOnFalse, b);
    }
}
