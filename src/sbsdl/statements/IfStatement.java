package sbsdl.statements;

import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;

public class IfStatement implements Statement {
    private final Expression myCondition;
    private final Statement myOnTrue;
    private final Statement myOnFalse;
    
    public IfStatement(
            Expression condition, Statement onTrue, Statement onFalse) {
        myCondition = condition;
        myOnTrue = onTrue;
        myOnFalse = onFalse;
    }
    
    @Override
    public void execute(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        boolean conditionResult =
                myCondition.evaluate(h, s).assertIsBoolean().getValue();
        
        if (conditionResult) {
            myOnTrue.execute(h, s);
        }
        else {
            myOnFalse.execute(h, s);
        }
    }
}
