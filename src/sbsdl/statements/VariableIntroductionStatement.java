package sbsdl.statements;

import sbsdl.ExecutionException;
import sbsdl.InternalExecutionException;
import sbsdl.ParseLocation;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;;
import sbsdl.values.Value;

public class VariableIntroductionStatement extends SkeletonStatement {
    private final Sbsdl.Symbol myName;
    private final Expression myInitialValue;
    
    public VariableIntroductionStatement(
            ParseLocation l, Sbsdl.Symbol name, Expression initialValue) {
        super(l);
        myName = name;
        myInitialValue = initialValue;
    }
    
    @Override
    public void execute(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        Value initialValue = myInitialValue.evaluate(h, s);
        
        ExecutionException proxyGuard;
        if (myName.isBaked()) {
            proxyGuard = InternalExecutionException.cannotBakeProxy(
                    getParseLocation()).getExecutionException();
        }
        else {
            proxyGuard = null;
        }
        
        s.introduceSymbol(myName, initialValue.copy(proxyGuard));
    }
    
    private String cannotContainProxyMessage(boolean bake, Value v) {
        String result;
        if (bake) {
            result = "Cannot bake a value containing a proxy object: " + v;
        }
        else {
            result = null;
        }
        return result;
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("STMT DIMENSIONALIZE VARIABLE (");
        
        if (myName.isBaked()) {
            b.append("baked");
        }
        else {
            b.append("not baked");
        }
        
        b.append(")\n");
        
        Util.indent(indentUnit, indentLevels + 1, b);
        b.append("var: ");
        b.append(myName);
        b.append("\n");
        
        Util.labeledChild(indentUnit, indentLevels, "initial value expression:",
                myInitialValue, b);
    }
}
