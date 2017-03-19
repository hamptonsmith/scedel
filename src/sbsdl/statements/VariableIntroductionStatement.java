package sbsdl.statements;

import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;

public class VariableIntroductionStatement implements Statement {
    private final String myName;
    private final Expression myInitialValue;
    
    public VariableIntroductionStatement(String name, Expression initialValue) {
        myName = name;
        myInitialValue = initialValue;
    }
    
    @Override
    public void execute(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        s.putSymbol(myName, myInitialValue.evaluate(h, s).copy(false));
    }
}
