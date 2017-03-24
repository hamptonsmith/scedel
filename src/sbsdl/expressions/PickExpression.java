package sbsdl.expressions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sbsdl.InternalExecutionException;
import sbsdl.ParseLocation;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.statements.Statement;
import sbsdl.values.VDict;
import sbsdl.values.VFunction;
import sbsdl.values.VNumber;
import sbsdl.values.VSeq;
import sbsdl.values.VUnavailable;
import sbsdl.values.Value;

public class PickExpression extends SkeletonExpression {
    private final Sbsdl.Decider myDecider;
    private final Sbsdl.Symbol myExemplar;
    private final Expression myPool;
    private final Expression myCount;
    private final Expression myUniqueFlag;
    private final Expression myWeighter;
    private final Expression myWhere;
    
    public PickExpression(ParseLocation l, Sbsdl.Symbol exemplar,
            Expression pool, Expression count, Expression unique,
            Expression weighter, Expression where, Sbsdl.Decider decider) {
        super(l);
        myDecider = decider;
        myExemplar = exemplar;
        myPool = pool;
        myCount = count;
        myUniqueFlag = unique;
        myWhere = where;
        
        if (weighter == null) {
            myWeighter = VFunction.buildConstantFunction(VNumber.of(1, 1));
        }
        else {
            myWeighter = weighter;
        }
    }
    
    @Override
    public Value evaluate(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        VSeq poolSeq =
                myPool.evaluate(h, s).assertIsSeq(myPool.getParseLocation());
        Value weighter = myWeighter.evaluate(h, s);
        
        int requestedCt = myCount.evaluate(h, s).assertIsNumber(
                    myCount.getParseLocation())
                .assertNonNegativeReasonableInteger(
                    myCount.getParseLocation(), "pick count");
        boolean unique =
                myUniqueFlag.evaluate(h, s).assertIsBoolean(
                        myUniqueFlag.getParseLocation()).getValue();
        
        int index = 0;
        int nonZeroWeightCt = 0;
        int totalWeight = 0;
        Map<Value, Integer> weights = new HashMap<>();
        for (Value e : poolSeq.elements()) {
            s.pushScope(false);
            s.introduceSymbol(myExemplar, e);
            
            if (myWhere.evaluate(h, s).assertIsBoolean(
                    myWhere.getParseLocation()).getValue()) {
                int weight = weight(weighter, e, index, h, s);
                totalWeight += weight;
                
                if (weight > 0) {
                    nonZeroWeightCt++;
                }
                
                weights.put(e, weight);
            }
            
            index++;
        }
        
        Value result = null;
        if (nonZeroWeightCt == 0) {
            if (requestedCt == 1) {
                result = VUnavailable.INSTANCE;
            }
            else {
                result = new VSeq();
                
                for (int i = 0; i < requestedCt; i++) {
                    ((VSeq) result).enqueue(VUnavailable.INSTANCE);
                }
            }
        }
        else {
            if (unique && nonZeroWeightCt < requestedCt) {
                result = VUnavailable.INSTANCE;
            }
            else {
                if (requestedCt != 1) {
                    result = new VSeq();
                }

                for (int i = 0; i < requestedCt; i++) {
                    Value chosen = null;
                    double remainingChance = 1.0;

                    // For testability, we always iterate over the elements in
                    // order.
                    for (Value potentialOption : poolSeq.elements()) {
                        if (weights.containsKey(potentialOption)) {
                            if (myDecider.randomize(remainingChance)) {
                                chosen = potentialOption;
                            }

                            remainingChance -= (weights.get(potentialOption)
                                    / (double) totalWeight);
                        }
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
        }
        
        return result.copy(null);
    }

    @Override
    public boolean yeildsBakedLValues() {
        return false;
    }
    
    private int weight(Value weighter, Value query, int queryIndex,
            Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        Value weight;
        if (weighter instanceof VDict) {
            weight = ((VDict) weighter).get(query);
        }
        else if (weighter instanceof VFunction) {
            List<Expression> params = new ArrayList<>(1);
            params.add(new VariableNameExpression(
                    myWeighter.getParseLocation(), myExemplar));
            
            FunctionCallExpression fCall = new FunctionCallExpression(
                    getParseLocation(), weighter, params);
            weight = fCall.evaluate(h, s);
        }
        else {
            throw InternalExecutionException.invalidWeighter(
                    myWeighter.getParseLocation(), weighter);
        }
        
        return weight.assertIsNumber(myWeighter.getParseLocation())
                .assertNonNegativeReasonableInteger(
                        myWeighter.getParseLocation(), "calculated weight for "
                                + "element " + queryIndex + " (" + query + ")");
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("EXP PICK\n");
        
        Statement.Util.indent(indentUnit, indentLevels + 1, b);
        b.append("exemplar: ");
        b.append(myExemplar);
        b.append("\n");
        
        Statement.Util.labeledChild(
                indentUnit, indentLevels, "count:", myCount, b);
        Statement.Util.labeledChild(
                indentUnit, indentLevels, "unique:", myUniqueFlag, b);
        Statement.Util.labeledChild(
                indentUnit, indentLevels, "collection:", myPool, b);
        Statement.Util.labeledChild(
                indentUnit, indentLevels, "where:", myWhere, b);
        Statement.Util.labeledChild(
                indentUnit, indentLevels, "weighter:", myWeighter, b);
    }
}
