package sbsdl.expressions;

import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.values.Value;

public interface Expression {
    public Value evaluate(Sbsdl.HostEnvironment h, ScriptEnvironment s);
    public boolean yeildsBakedLValues();
}
