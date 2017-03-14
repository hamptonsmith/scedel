package sbsdl.statements;

import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;

public interface Statement {
    public static final Statement NO_OP = new Statement() {
                @Override
                public void execute(
                        Sbsdl.HostEnvironment h, ScriptEnvironment s) { }
            };
    
    public void execute(Sbsdl.HostEnvironment h, ScriptEnvironment s);
}
