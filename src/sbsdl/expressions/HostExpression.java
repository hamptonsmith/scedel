package sbsdl.expressions;

import java.util.ArrayList;
import java.util.List;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.values.Value;

public class HostExpression implements Expression {
    private final String myId;
    private final List<Expression> myParameters;
    
    public HostExpression(String id, List<Expression> params) {
        myId = id;
        myParameters = params == null ? null : new ArrayList<>(params);
    }
    
    @Override
    public Value evaluate(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        List<Value> evaluatedParams = null;
        
        if (myParameters != null) {
            evaluatedParams = new ArrayList<>(myParameters.size());
            for (Expression p : myParameters) {
                evaluatedParams.add(p.evaluate(h, s).copy(null));
            }
        }
        
        Value result;
        try {
            result = h.evaluate(myId, evaluatedParams);
            
            if (result == null) {
                throw new RuntimeException("Host environment "
                        + "evaluated host expression #" + myId
                        + (evaluatedParams == null ? ""
                                : evaluatedParams.toString()) + " to "
                        + "null.");
            }
        }
        catch (Sbsdl.HostEnvironmentException hee) {
            throw new Sbsdl.ExecutionException(
                    "Host expression evaluation error: " + hee.getMessage());
        }
        
        return result;
    }

    @Override
    public boolean yeildsBakedLValues() {
       return false;
    }
}
