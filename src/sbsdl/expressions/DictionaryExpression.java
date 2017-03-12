package sbsdl.expressions;

import java.util.HashMap;
import java.util.Map;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.values.VDict;
import sbsdl.values.Value;

public class DictionaryExpression implements Expression {
    private final Map<Expression, Expression> myExpressions = new HashMap<>();
    
    public DictionaryExpression(Map<Expression, Expression> exps) {
        myExpressions.putAll(exps);
    }
    
    @Override
    public Value evaluate(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        VDict result = new VDict();
        for (Map.Entry<Expression, Expression> entry
                : myExpressions.entrySet()) {
            result.put(
                    entry.getKey().evaluate(h, s),
                    entry.getValue().evaluate(h, s));
        }
        
        return result;
    }
}
