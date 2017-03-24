package sbsdl.expressions;

import sbsdl.ParseLocation;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.statements.Statement;
import sbsdl.values.Value;

/** 
 * <p>Provides a way to tag {@link sbsdl.values.Value Value}s (which do not
 * carry useful source location information) with their source location
 * information when the value in question derives from a literal in the
 * code.</p>
 */
public class LiteralExpression extends SkeletonExpression {
    private final Value myValue;
    
    public LiteralExpression(ParseLocation l, Value v) {
        super(l);
        
        myValue = v;
    }
    
    @Override
    public Value evaluate(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        return myValue.evaluate(h, s);
    }

    @Override
    public boolean yeildsBakedLValues() {
        return false;
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("EXP LITERAL WRAPPER\n");
        Statement.Util.indent(indentUnit, indentLevels + 1, b);
        myValue.prettyRender(indentUnit, indentLevels + 1, b);
    }
    
}