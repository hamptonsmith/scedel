package sbsdl.expressions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.values.VDict;
import sbsdl.values.VFunction;
import sbsdl.values.VSeq;
import sbsdl.values.VUnavailable;
import sbsdl.values.Value;

public class PickExpression implements Expression {
    private final String myExemplar;
    private final Expression myPool;
    private final Expression myCount;
    private final Expression myUniqueFlag;
    private final Expression myWeighter;
    private final Expression myWhere;
    
    public PickExpression(String exemplar, Expression pool, Expression count,
            Expression unique, Expression weighter, Expression where) {
        myExemplar = exemplar;
        myPool = pool;
        myCount = count;
        myUniqueFlag = unique;
        myWeighter = weighter;
        myWhere = where;
    }
    
    @Override
    public Value evaluate(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        VSeq poolSeq = myPool.evaluate(h, s).assertIsSeq();
        Value weighter = myWeighter.evaluate(h, s);
        
        int requestedCt = myCount.evaluate(h, s).assertIsNumber()
                .assertNonNegativeReasonableInteger();
        boolean unique =
                myUniqueFlag.evaluate(h, s).assertIsBoolean().getValue();
        
        int nonZeroWeightCt = 0;
        int totalWeight = 0;
        Map<Value, Integer> weights = new HashMap<>();
        for (Value e : poolSeq.elements()) {
            s.pushScope(false);
            s.putSymbol(myExemplar, e);
            
            if (myWhere.evaluate(h, s).assertIsBoolean().getValue()) {
                int weight = weight(weighter, e, h, s);
                totalWeight += weight;
                
                if (weight > 0) {
                    nonZeroWeightCt++;
                }
                
                weights.put(e, weight);
            }
        }
        
        Value result = null;
        if (unique && nonZeroWeightCt < requestedCt) {
            result = VUnavailable.INSTANCE;
        }
        else {
            if (requestedCt == 0) {
                result = VUnavailable.INSTANCE;
            }
            else if (requestedCt > 1) {
                result = new VSeq();
            }
            
            for (int i = 0; i < requestedCt; i++) {
                Value chosen = null;
                double remainingChance = 1.0;
                for (Map.Entry<Value, Integer> option : weights.entrySet()) {
                    if (Math.random() < remainingChance) {
                        chosen = option.getKey();
                    }

                    remainingChance -=
                            (option.getValue() / (double) totalWeight);
                }

                if (unique) {
                    int weight = weights.remove(chosen);
                    totalWeight -= weight;
                }
                
                if (requestedCt == 1) {
                    result = chosen;
                }
                else {
                    ((VSeq) result).enqueue(chosen);
                }
            }
        }
        
        return result;
    }
    
    private int weight(Value weighter, Value query, Sbsdl.HostEnvironment h,
            ScriptEnvironment s) {
        Value weight;
        if (weighter instanceof VDict) {
            weight = ((VDict) weighter).get(query);
        }
        else if (weighter instanceof VFunction) {
            List<Expression> params = new ArrayList<>(1);
            params.add(new VariableNameExpression(myExemplar));
            
            FunctionCallExpression fCall =
                    new FunctionCallExpression(weighter, params);
            weight = fCall.evaluate(h, s);
        }
        else {
            throw new Sbsdl.ExecutionException(
                    "Weighter is not a dictionary or a function: " + weighter);
        }
        
        return weight.assertIsNumber().assertNonNegativeReasonableInteger();
    }
}