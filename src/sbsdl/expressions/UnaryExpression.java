package sbsdl.expressions;

import sbsdl.ParseLocation;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.statements.Statement;
import sbsdl.values.Value;

public class UnaryExpression extends SkeletonExpression {
    public static enum Operator {
        BOOLEAN_NEGATE {
            @Override
            public Value apply(BinaryExpression.Lazy operand,
                    Sbsdl.HostEnvironment he, ScriptEnvironment s) {
                return operand.evaluate().assertIsBoolean(
                        operand.getParseLocation()).not();
            }
        };
        
        public abstract Value apply(BinaryExpression.Lazy operand,
                Sbsdl.HostEnvironment he, ScriptEnvironment s);
    }
    
    private final Operator myOperator;
    private final Expression myOperand;
    
    public UnaryExpression(
            ParseLocation l, Operator operator, Expression operand) {
        super(l);
        myOperator = operator;
        myOperand = operand;
    }
    
    @Override
    public Value evaluate(
            final Sbsdl.HostEnvironment h, final ScriptEnvironment s) {
        BinaryExpression.Lazy operandValue = new BinaryExpression.Lazy() {
                    @Override
                    public Value noCacheEvaluate() {
                        return myOperand.evaluate(h, s);
                    }

                    @Override
                    public ParseLocation getParseLocation() {
                        return myOperand.getParseLocation();
                    }
                };
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
