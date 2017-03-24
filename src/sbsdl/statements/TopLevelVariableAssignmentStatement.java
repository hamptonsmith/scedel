package sbsdl.statements;

import sbsdl.ParseLocation;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;

public class TopLevelVariableAssignmentStatement extends SkeletonStatement {
    private final Sbsdl.Symbol myVarName;
    private final Expression myExpression;
    
    public TopLevelVariableAssignmentStatement(ParseLocation l,
            Sbsdl.Symbol varName, Expression value) {
        super(l);
        if (varName.isBaked()) {
            throw new RuntimeException();
        }
        
        myVarName = varName;
        myExpression = value;
    }

    @Override
    public void execute(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        s.assignValue(myVarName, myExpression.evaluate(h, s).copy(null));
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("STMT ASSIGN TO TOP LEVEL VARIABLE\n");
        Util.indent(indentUnit, indentLevels + 1, b);
        b.append("var: ");
        b.append(myVarName);
        b.append("\n");
        Util.labeledChild(indentUnit, indentLevels, "expression to assign:",
                myExpression, b);
    }
}
