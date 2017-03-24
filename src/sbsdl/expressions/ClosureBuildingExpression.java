package sbsdl.expressions;

import java.util.LinkedList;
import java.util.List;
import sbsdl.ParseLocation;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.statements.MultiplexingStatement;
import sbsdl.statements.Statement;
import sbsdl.values.VFunction;
import sbsdl.values.Value;

public class ClosureBuildingExpression extends SkeletonExpression {
    private final List<Sbsdl.Symbol> myArgumentNames;
    private final MultiplexingStatement myCode;
    
    public ClosureBuildingExpression(ParseLocation l,
            List<Sbsdl.Symbol> argNames, MultiplexingStatement code) {
        super(l);
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

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("EXP BUILD FUNCTION CLOSURE\n");
        Statement.Util.indent(indentUnit, indentLevels + 1, b);
        b.append("arg names: ");
        b.append(myArgumentNames);
        b.append("\n");
        
        Statement.Util.labeledChild(
                indentUnit, indentLevels, "code:", myCode, b);
    }
}
