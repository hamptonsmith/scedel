package com.shieldsbetter.scedel.expressions;

import java.util.HashMap;
import java.util.Map;
import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.statements.Statement;
import com.shieldsbetter.scedel.values.VDict;
import com.shieldsbetter.scedel.values.Value;
import java.io.PrintWriter;

public class DictionaryExpression extends SkeletonExpression {
    private final Map<Expression, Expression> myExpressions = new HashMap<>();
    
    public DictionaryExpression(
            ParseLocation l, Map<Expression, Expression> exps) {
        super(l);
        myExpressions.putAll(exps);
    }
    
    @Override
    public Value evaluate(Scedel.HostEnvironment h, ScriptEnvironment s) {
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

    @Override
    public void accept(Visitor v) {
        v.visitDictionaryExpression(this);
    }
}
