package sbsdl.expressions;

import java.util.LinkedList;
import java.util.List;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.statements.MultiplexingStatement;
import sbsdl.values.VFunction;
import sbsdl.values.Value;

public class ClosureBuildingExpression implements Expression {
    private final List<Sbsdl.Symbol> myArgumentNames;
    private final MultiplexingStatement myCode;
    
    public ClosureBuildingExpression(
            List<Sbsdl.Symbol> argNames, MultiplexingStatement code) {
        myArgumentNames = new LinkedList<>(argNames);
        myCode = code;
    }

    @Override
    public Value evaluate(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        return new VFunction(myArgumentNames, myCode, s);
    }

    @Override
    public boolean yeildsBakedLValues() {
        return false;
    }
}
