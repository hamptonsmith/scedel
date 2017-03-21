package sbsdl.expressions;

import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.values.Value;

public class VariableNameExpression implements Expression {
    private final Sbsdl.Symbol myVariableName;
    
    public VariableNameExpression(Sbsdl.Symbol name) {
        myVariableName = name;
    }
    
    @Override
    public Value evaluate(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        return s.lookupVariable(myVariableName);
    }
}
