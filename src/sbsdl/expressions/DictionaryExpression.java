package sbsdl.expressions;

import java.util.HashMap;
import java.util.Map;
import sbsdl.ParseLocation;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.statements.Statement;
import sbsdl.values.VDict;
import sbsdl.values.Value;

public class DictionaryExpression extends SkeletonExpression {
    private final Map<Expression, Expression> myExpressions = new HashMap<>();
    
    public DictionaryExpression(
            ParseLocation l, Map<Expression, Expression> exps) {
        super(l);
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

    @Override
    public boolean yeildsBakedLValues() {
        return false;
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("EXP DICTIONARY\n");
        for (Map.Entry<Expression, Expression> entry
                : myExpressions.entrySet()) {
            Statement.Util.indent(indentUnit, indentLevels + 1, b);
            b.append("entry:\n");
            Statement.Util.labeledChild(
                    indentUnit, indentLevels + 1, "key:\n", entry.getKey(), b);
            Statement.Util.labeledChild(indentUnit, indentLevels + 1,
                    "value:\n", entry.getKey(), b);
        }
    }
}
