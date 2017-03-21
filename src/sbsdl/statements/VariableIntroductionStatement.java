package sbsdl.statements;

import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;

public class VariableIntroductionStatement implements Statement {
    private final Sbsdl.Symbol myName;
    private final Expression myInitialValue;
    
    public VariableIntroductionStatement(
            Sbsdl.Symbol name, Expression initialValue) {
        myName = name;
        myInitialValue = initialValue;
    }
    
    @Override
    public void execute(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        s.introduceSymbol(myName, myInitialValue.evaluate(h, s).copy(false));
    }
}
