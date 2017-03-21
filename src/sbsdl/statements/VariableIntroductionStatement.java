package sbsdl.statements;

import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;
import sbsdl.values.VProxy;
import sbsdl.values.Value;

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
        Value initialValue = myInitialValue.evaluate(h, s);
        s.introduceSymbol(myName, initialValue.copy(
                cannotContainProxyMessage(myName.isBaked(), initialValue)));
    }
    
    private String cannotContainProxyMessage(boolean bake, Value v) {
        String result;
        if (bake) {
            result = "Cannot bake a value containing a proxy object: " + v;
        }
        else {
            result = null;
        }
        return result;
    }
}
