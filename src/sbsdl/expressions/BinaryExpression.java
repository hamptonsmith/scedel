package sbsdl.expressions;

import java.math.BigInteger;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.statements.Statement;
import sbsdl.values.VBoolean;
import sbsdl.values.VNumber;
import sbsdl.values.VSeq;
import sbsdl.values.VString;
import sbsdl.values.VUnavailable;
import sbsdl.values.Value;

public class BinaryExpression implements Expression {
    public static enum Operator {
        PLUS(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                Value result;
                if (operand1.evaluate() instanceof VNumber) {
                    result = operand1.evaluate().assertIsNumber().add(
                            operand2.evaluate().assertIsNumber());
                }
                else if (operand1.evaluate() instanceof VString) {
                    result = new VString(
                            operand1.evaluate().assertIsString().getValue()
                                    + operand2.evaluate());
                }
                else {
                    throw new Sbsdl.ExecutionException("Plus operator must "
                            + "operate on a number or a string.  Found: "
                            + operand1);
                }
                
                return result;
            }
        },
        MINUS(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return operand1.evaluate().assertIsNumber()
                        .subtract(operand2.evaluate().assertIsNumber());
            }
        },
        TIMES(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return operand1.evaluate().assertIsNumber()
                        .multiply(operand2.evaluate().assertIsNumber());
            }
        },
        DIVIDED_BY(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return operand1.evaluate().assertIsNumber()
                        .divide(operand2.evaluate().assertIsNumber());
            }
        },
        RAISED_TO(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return operand1.evaluate().assertIsNumber()
                        .raiseTo(operand2.evaluate().assertIsNumber());
            }
        },
        AND(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return VBoolean.of(
                        operand1.evaluate().assertIsBoolean().getValue()
                                && operand2.evaluate().assertIsBoolean()
                                        .getValue());
            }
        },
        OR(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return VBoolean.of(
                        operand1.evaluate().assertIsBoolean().getValue()
                                || operand2.evaluate().assertIsBoolean()
                                        .getValue());
            }
        },
        LOOK_UP_KEY(true) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return operand1.evaluate().assertIsDict().get(
                        operand2.evaluate());
            }
        },
        INDEX_SEQ(true) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                VSeq sequenceValue = operand1.evaluate().assertIsSeq();
                VNumber indexValue = operand2.evaluate().assertIsNumber();

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
        EQUAL(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return VBoolean.of(operand1.evaluate().equals(
                        operand2.evaluate()));
            }
        },
        NOT_EQUAL(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return VBoolean.of(!operand1.evaluate().equals(
                        operand2.evaluate()));
            }
        },
        LESS_THAN(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return VBoolean.of(operand1.evaluate().assertIsNumber()
                        .compareTo(operand2.evaluate().assertIsNumber()) < 0);
            }
        },
        LESS_THAN_EQ(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return VBoolean.of(operand1.evaluate().assertIsNumber()
                        .compareTo(operand2.evaluate().assertIsNumber()) <= 0);
            }
        },
        GREATER_THAN_EQ(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return VBoolean.of(operand1.evaluate().assertIsNumber()
                        .compareTo(operand2.evaluate().assertIsNumber()) >= 0);
            }
        },
        GREATER_THAN(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return VBoolean.of(operand1.evaluate().assertIsNumber()
                        .compareTo(operand2.evaluate().assertIsNumber()) > 0);
            }
        },
        OTHERWISE(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                Value result;
                
                if (operand1.evaluate() == VUnavailable.INSTANCE) {
                    result = operand2.evaluate();
                }
                else {
                    result = operand1.evaluate();
                }
                
                return result;
            }
        };
    
        private final boolean myTransfersBakedBehaviorFlag;
        
        Operator(boolean transfersBaked) {
            myTransfersBakedBehaviorFlag = transfersBaked;
        }
        
        public final boolean transfersBaked() {
            return myTransfersBakedBehaviorFlag;
        }
        
        abstract Value apply(Lazy operand1, Lazy operand2);
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
    public Value evaluate(
            final Sbsdl.HostEnvironment h, final ScriptEnvironment s) {
        Lazy op1Val = new Lazy() {
                    @Override
                    public Value noCacheEvaluate() {
                        return myOperand1.evaluate(h, s);
                    }
                };
        Lazy op2Val = new Lazy() {
                    @Override
                    public Value noCacheEvaluate() {
                        return myOperand2.evaluate(h, s);
                    }
                };
        
        return myOperator.apply(op1Val, op2Val);
    }

    @Override
    public boolean yeildsBakedLValues() {
        boolean result;
        
        if (myOperator.transfersBaked()) {
            result = myOperand1.yeildsBakedLValues();
        }
        else {
            result = false;
        }
        
        return result;
    }

    @Override
    public void prettyRender(int indentUnit, int indentLevels, StringBuilder b) {
        b.append("EXP BINARY\n");
        
        Statement.Util.indent(indentUnit, indentLevels + 1, b);
        b.append("operator: ");
        b.append(myOperator);
        b.append("\n");
        
        Statement.Util.labeledChild(
                indentUnit, indentLevels, "operand1:", myOperand1, b);
        Statement.Util.labeledChild(
                indentUnit, indentLevels, "operand2:", myOperand2, b);
    }
    
    private static abstract class Lazy {
        private Value myCachedValue;
        
        public final Value evaluate() {
            if (myCachedValue == null) {
                myCachedValue = noCacheEvaluate();
            }
            
            return myCachedValue;
        }
        
        public abstract Value noCacheEvaluate();
    }
}
