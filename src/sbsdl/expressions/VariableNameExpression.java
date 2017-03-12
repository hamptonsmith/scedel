package sbsdl.expressions;

import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.values.Value;

public class VariableNameExpression implements Expression {
    private final String myVariableName;
    
    public VariableNameExpression(String name) {
        myVariableName = name;
    }
    
    @Override
    public Value evaluate(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        return s.lookupVariable(myVariableName);
    }
}
