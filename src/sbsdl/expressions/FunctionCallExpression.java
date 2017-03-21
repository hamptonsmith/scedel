package sbsdl.expressions;

import java.util.ArrayList;
import java.util.List;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.values.VFunction;
import sbsdl.values.Value;

public class FunctionCallExpression implements Expression {
    private final Expression myFunction;
    private final List<Expression> myParameters;
    
    public FunctionCallExpression(
            Expression f, List<Expression> parameters) {
        myFunction = f;
        myParameters = new ArrayList<>(parameters);
    }
    
    @Override
    public Value evaluate(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        VFunction fValue = myFunction.evaluate(h, s).assertIsFunction();
        
        List<Value> paramVals = new ArrayList<>(myParameters.size());
        for (Expression p : myParameters) {
            paramVals.add(p.evaluate(h, s).copy(null));
        }
        
        return fValue.call(h, s, paramVals);
    }
}
