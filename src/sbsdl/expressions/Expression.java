package sbsdl.expressions;

import sbsdl.ParseLocation;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.values.Value;

public interface Expression {
    public ParseLocation getParseLocation();
    public Value evaluate(Sbsdl.HostEnvironment h, ScriptEnvironment s);
    public boolean yeildsBakedLValues();
    
    public void prettyRender(int indentUnit, int indentLevels, StringBuilder b);
}
