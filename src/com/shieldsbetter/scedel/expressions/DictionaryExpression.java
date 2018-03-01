package com.shieldsbetter.scedel.expressions;

import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.statements.Statement;
import com.shieldsbetter.scedel.values.VDict;
import com.shieldsbetter.scedel.values.Value;
import java.util.LinkedList;
import java.util.List;

public class DictionaryExpression extends SkeletonExpression {
    
    // We avoid an actual Map here because we want to avoid non-determinancy.
    // If two different expressions evaluate to the same value, they should be
    // evaluated in the proper intended order so that the expected behavior
    // occurs (the later mapping clobbers the earlier mapping.)
    private final List<Mapping> myMappings = new LinkedList<>();
    
    public DictionaryExpression(ParseLocation l, List<Mapping> mappings) {
        super(l);
        myMappings.addAll(mappings);
    }
    
    public Iterable<Mapping> entries() {
        return myMappings;
    }
    
    public int size() {
        return myMappings.size();
    }
    
    @Override
    public Value evaluate(Scedel.HostEnvironment h, ScriptEnvironment s) {
        VDict result = new VDict();
        for (Mapping mapping : myMappings) {
            result.put(
                    mapping.getKey().evaluate(h, s),
                    mapping.getValue().evaluate(h, s));
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
        for (Mapping m : myMappings) {
            Statement.Util.indent(indentUnit, indentLevels + 1, b);
            b.append("entry:\n");
            Statement.Util.labeledChild(
                    indentUnit, indentLevels + 1, "key:\n", m.getKey(), b);
            Statement.Util.labeledChild(indentUnit, indentLevels + 1,
                    "value:\n", m.getKey(), b);
        }
    }

    @Override
    public void accept(Visitor v) {
        v.visitDictionaryExpression(this);
    }
    
    public static class Mapping {
        private final Expression myKey;
        private final Expression myValue;
        
        public Mapping(Expression key, Expression value) {
            myKey = key;
            myValue = value;
        }
        
        public Expression getKey() {
            return myKey;
        }
        
        public Expression getValue() {
            return myValue;
        }
    }
}
