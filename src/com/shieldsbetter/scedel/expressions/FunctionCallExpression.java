package com.shieldsbetter.scedel.expressions;

import java.util.ArrayList;
import java.util.List;
import com.shieldsbetter.scedel.InternalExecutionException;
import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.statements.Statement;
import com.shieldsbetter.scedel.values.VFunction;
import com.shieldsbetter.scedel.values.Value;

public class FunctionCallExpression extends SkeletonExpression {
    private final Expression myFunction;
    private final List<Expression> myParameters;
    
    public FunctionCallExpression(ParseLocation l, Expression f,
            List<Expression> parameters) {
        super(l);
        myFunction = f;
        myParameters = new ArrayList<>(parameters);
    }
    
    public Expression getFunction() {
        return myFunction;
    }
    
    public List<Expression> getParameters() {
        return myParameters;
    }
    
    @Override
    public Value evaluate(Scedel.HostEnvironment h, ScriptEnvironment s) {
        VFunction fValue = myFunction.evaluate(h, s).assertIsFunction(
                myFunction.getParseLocation());
        
        List<Value> paramVals = new ArrayList<>(myParameters.size());
        for (Expression p : myParameters) {
            paramVals.add(p.evaluate(h, s).copy(null));
        }
        
        if (myParameters.size() != fValue.getArgumentCount()) {
            throw InternalExecutionException.incorrectNumberOfParameters(
                    getParseLocation(), myParameters.size(),
                    fValue.getArgumentCount());
        }
        
        Value result;
        try {
            result = fValue.call(getParseLocation(), h, s, paramVals);
        }
        catch (InternalExecutionException iee) {
            iee.pushStackLevel(getParseLocation());
            throw iee;
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
        b.append("EXP FUNCTION CALL\n");
        
        Statement.Util.labeledChild(
                indentUnit, indentLevels, "function:", myFunction, b);
        
        Statement.Util.indent(indentUnit, indentLevels + 1, b);
        b.append("parameters:\n");
        for (Expression e : myParameters) {
            Statement.Util.indent(indentUnit, indentLevels + 2, b);
            e.prettyRender(indentUnit, indentLevels + 2, b);
        }
    }

    @Override
    public void accept(Visitor v) {
        v.visitFunctionCallExpression(this);
    }
}
