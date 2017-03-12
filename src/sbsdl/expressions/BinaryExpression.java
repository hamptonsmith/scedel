package sbsdl.expressions;

import java.math.BigInteger;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.values.VBoolean;
import sbsdl.values.VNumber;
import sbsdl.values.VSeq;
import sbsdl.values.Value;

public class BinaryExpression implements Expression {
    public static enum Operator {
        PLUS {
            @Override
            public Value apply(Value operand1, Value operand2) {
                return operand1.assertIsNumber().add(operand2.assertIsNumber());
            }
        },
        MINUS {
            @Override
            public Value apply(Value operand1, Value operand2) {
                return operand1.assertIsNumber()
                        .subtract(operand2.assertIsNumber());
            }
        },
        TIMES {
            @Override
            public Value apply(Value operand1, Value operand2) {
                return operand1.assertIsNumber()
                        .multiply(operand2.assertIsNumber());
            }
        },
        DIVIDED_BY {
            @Override
            public Value apply(Value operand1, Value operand2) {
                return operand1.assertIsNumber()
                        .divide(operand2.assertIsNumber());
            }
        },
        RAISED_TO {
            @Override
            public Value apply(Value operand1, Value operand2) {
                return operand1.assertIsNumber()
                        .raiseTo(operand2.assertIsNumber());
            }
        },
        AND {
            @Override
            public Value apply(Value operand1, Value operand2) {
                return operand1.assertIsBoolean()
                        .and(operand2.assertIsBoolean());
            }
        },
        OR {
            @Override
            public Value apply(Value operand1, Value operand2) {
                return operand1.assertIsBoolean()
                        .or(operand2.assertIsBoolean());
            }
        },
        LOOK_UP_KEY {
            @Override
            public Value apply(Value operand1, Value operand2) {
                return operand1.assertIsDict().get(operand2);
            }
        },
        INDEX_SEQ {
            @Override
            public Value apply(Value operand1, Value operand2) {
                VSeq sequenceValue = operand1.assertIsSeq();
                VNumber indexValue = operand2.assertIsNumber();

                if (!indexValue.getDenominator().equals(BigInteger.ONE)) {
                    throw new Sbsdl.ExecutionException(
                            "Index is not integral: " + indexValue);
                }
                else if (indexValue.getNumerator().compareTo(
                        BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                    throw new Sbsdl.ExecutionException(
                            "Index overflow: " + indexValue);
                }
                else if (indexValue.getNumerator()
                        .compareTo(BigInteger.ZERO) < 0) {
                    throw new Sbsdl.ExecutionException(
                            "Negative index: " + indexValue);
                }

                return sequenceValue.get(indexValue.getNumerator().intValue());
            }
        },
        EQUAL {
            @Override
            public Value apply(Value operand1, Value operand2) {
                return VBoolean.of(operand1.equals(operand2));
            }
        },
        NOT_EQUAL {
            @Override
            public Value apply(Value operand1, Value operand2) {
                return VBoolean.of(!operand1.equals(operand2));
            }
        },
        LESS_THAN {
            @Override
            public Value apply(Value operand1, Value operand2) {
                return VBoolean.of(operand1.assertIsNumber()
                        .compareTo(operand2.assertIsNumber()) < 0);
            }
        },
        LESS_THAN_EQ {
            @Override
            public Value apply(Value operand1, Value operand2) {
                return VBoolean.of(operand1.assertIsNumber()
                        .compareTo(operand2.assertIsNumber()) <= 0);
            }
        },
        GREATER_THAN_EQ {
            @Override
            public Value apply(Value operand1, Value operand2) {
                return VBoolean.of(operand1.assertIsNumber()
                        .compareTo(operand2.assertIsNumber()) >= 0);
            }
        },
        GREATER_THAN {
            @Override
            public Value apply(Value operand1, Value operand2) {
                return VBoolean.of(operand1.assertIsNumber()
                        .compareTo(operand2.assertIsNumber()) > 0);
            }
        };
    
        public abstract Value apply(Value operand1, Value operand2);
    }
    
    private final Expression myOperand1;
    private final Expression myOperand2;
    
    private final Operator myOperator;
    
    public BinaryExpression(
            Expression operand1, Operator operator, Expression operand2) {
        myOperand1 = operand1;
        myOperand2 = operand2;
        myOperator = operator;
    }
    
    @Override
    public Value evaluate(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        Value op1Val = myOperand1.evaluate(h, s);
        Value op2Val = myOperand2.evaluate(h, s);
        
        return myOperator.apply(op1Val, op2Val);
    }
}
