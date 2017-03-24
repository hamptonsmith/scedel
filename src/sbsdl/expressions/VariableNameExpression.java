package sbsdl.expressions;

import sbsdl.ParseLocation;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.values.Value;

public class VariableNameExpression extends SkeletonExpression {
    private final Sbsdl.Symbol myVariableName;
    
    public VariableNameExpression(ParseLocation l, Sbsdl.Symbol name) {
        super(l);
        myVariableName = name;
    }
    
    @Override
    public Value evaluate(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        return s.lookupVariable(myVariableName);
    }

    @Override
    public boolean yeildsBakedLValues() {
        return myVariableName.isBaked();
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("EXP VAR LOOKUP: ");
        b.append(myVariableName);
        b.append("\n");
    }
}
