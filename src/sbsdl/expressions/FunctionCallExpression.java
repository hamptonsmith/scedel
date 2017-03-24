package sbsdl.expressions;

import java.util.ArrayList;
import java.util.List;
import sbsdl.ParseLocation;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.statements.Statement;
import sbsdl.values.VFunction;
import sbsdl.values.Value;

public class FunctionCallExpression extends SkeletonExpression {
    private final Expression myFunction;
    private final List<Expression> myParameters;
    
    public FunctionCallExpression(ParseLocation l, Expression f,
            List<Expression> parameters) {
        super(l);
        myFunction = f;
        myParameters = new ArrayList<>(parameters);
    }
    
    @Override
    public Value evaluate(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        VFunction fValue = myFunction.evaluate(h, s).assertIsFunction(
                myFunction.getParseLocation());
        
        List<Value> paramVals = new ArrayList<>(myParameters.size());
        for (Expression p : myParameters) {
            paramVals.add(p.evaluate(h, s).copy(null));
        }
        
        return fValue.call(getParseLocation(), h, s, paramVals);
    }

    @Override
    public boolean yeildsBakedLValues() {
        return false;
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("EXP FUNCTION CALL\n");
        
        Statement.Util.labeledChild(
                indentUnit, indentLevels, "function:", myFunction, b);
        
        Statement.Util.indent(indentUnit, indentLevels + 1, b);
        b.append("parameters:\n");
        for (Expression e : myParameters) {
            Statement.Util.indent(indentUnit, indentLevels + 2, b);
            e.prettyRender(indentUnit, indentLevels + 2, b);
        }
    }
}
