package com.shieldsbetter.scedel.expressions;

import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.statements.Statement;
import com.shieldsbetter.scedel.values.Value;

public class UnaryExpression extends SkeletonExpression {
    public static enum Operator {
        BOOLEAN_NEGATE("not") {
            @Override
            public Value apply(BinaryExpression.Lazy operand,
                    Scedel.HostEnvironment he, ScriptEnvironment s) {
                return operand.evaluate().assertIsBoolean(
                        operand.getParseLocation()).not();
            }

            @Override
            public void accept(OperatorVisitor v, Expression operand) {
                v.visitBooleanNegate(operand);
            }
        };
        
        private final String myKey;
        
        Operator(String key) {
            myKey = key;
        }
        
        public String getKey() {
            return myKey;
        }
        
        public abstract Value apply(BinaryExpression.Lazy operand,
                Scedel.HostEnvironment he, ScriptEnvironment s);
        
        public abstract void accept(OperatorVisitor v, Expression operand);
    }
    
    private final Operator myOperator;
    private final Expression myOperand;
    
    public UnaryExpression(
            ParseLocation l, Operator operator, Expression operand) {
        super(l);
        myOperator = operator;
        myOperand = operand;
    }
    
    public Operator getOperator() {
        return myOperator;
    }
    
    public Expression getOperand() {
        return myOperand;
    }
    
    @Override
    public Value evaluate(
            final Scedel.HostEnvironment h, final ScriptEnvironment s) {
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

    @Override
    public void accept(Visitor v) {
        v.visitUnaryExpression(this);
    }
    
    public static interface OperatorVisitor {
        public void visitBooleanNegate(Expression operand);
    }
}
