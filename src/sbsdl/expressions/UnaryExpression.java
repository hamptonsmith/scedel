package sbsdl.expressions;

import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.statements.Statement;
import sbsdl.values.Value;

public class UnaryExpression implements Expression {
    public static enum Operator {
        BOOLEAN_NEGATE {
            @Override
            public Value apply(Value operand, Sbsdl.HostEnvironment he,
                    ScriptEnvironment s) {
                return operand.assertIsBoolean().not();
            }
        };
        
        public abstract Value apply(Value operand,
                Sbsdl.HostEnvironment he, ScriptEnvironment s);
    }
    
    private final Operator myOperator;
    private final Expression myOperand;
    
    public UnaryExpression(Operator operator, Expression operand) {
        myOperator = operator;
        myOperand = operand;
    }
    
    @Override
    public Value evaluate(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        Value operandValue = myOperand.evaluate(h, s);
        return myOperator.apply(operandValue, h, s);
    }

    @Override
    public boolean yeildsBakedLValues() {
        return false;
    }

    @Override
    public void prettyRender(
            int indentUnit, int indentLevels, StringBuilder b) {
        b.append("EXP UNARY\n");
        Statement.Util.indent(indentUnit, indentLevels + 1, b);
        b.append("operator: ");
        b.append(myOperator);
        b.append("\n");
        
        Statement.Util.labeledChild(
                indentUnit, indentLevels, "operand:", myOperand, b);
    }
}
