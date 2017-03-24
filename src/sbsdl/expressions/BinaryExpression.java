package sbsdl.expressions;

import java.math.BigInteger;
import sbsdl.InternalExecutionException;
import sbsdl.ParseLocation;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.statements.Statement;
import sbsdl.values.VBoolean;
import sbsdl.values.VNumber;
import sbsdl.values.VSeq;
import sbsdl.values.VString;
import sbsdl.values.VUnavailable;
import sbsdl.values.Value;

public class BinaryExpression extends SkeletonExpression {
    public static enum Operator {
        PLUS(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                Value result;
                if (operand1.evaluate() instanceof VNumber) {
                    result = operand1.evaluate().assertIsNumber(
                                    operand1.getParseLocation())
                            .add(operand2.evaluate().assertIsNumber(
                                    operand2.getParseLocation()));
                }
                else if (operand1.evaluate() instanceof VString) {
                    result = new VString(operand1.evaluate().assertIsString(
                                    operand1.getParseLocation())
                            .getValue() + operand2.evaluate());
                }
                else {
                    throw InternalExecutionException
                            .plusFirstParamMustBeNumOrString(
                                    operand1.getParseLocation(),
                                    operand1.evaluate());
                }
                
                return result;
            }
        },
        MINUS(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return operand1.evaluate().assertIsNumber(
                            operand1.getParseLocation())
                        .subtract(operand2.evaluate().assertIsNumber(
                            operand2.getParseLocation()));
            }
        },
        TIMES(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return operand1.evaluate().assertIsNumber(
                            operand1.getParseLocation())
                        .multiply(operand2.evaluate().assertIsNumber(
                            operand2.getParseLocation()));
            }
        },
        DIVIDED_BY(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return operand1.evaluate().assertIsNumber(
                            operand1.getParseLocation())
                        .divide(operand2.evaluate().assertIsNumber(
                                    operand2.getParseLocation()),
                                operand2.getParseLocation());
            }
        },
        RAISED_TO(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return operand1.evaluate().assertIsNumber(
                                operand1.getParseLocation())
                        .raiseTo(operand2.evaluate().assertIsNumber(
                                operand2.getParseLocation()),
                                operand2.getParseLocation());
            }
        },
        AND(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return VBoolean.of(
                        operand1.evaluate().assertIsBoolean(
                                    operand1.getParseLocation()).getValue()
                                && operand2.evaluate().assertIsBoolean(
                                    operand2.getParseLocation()).getValue());
            }
        },
        OR(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return VBoolean.of(
                        operand1.evaluate().assertIsBoolean(
                                    operand1.getParseLocation()).getValue()
                                || operand2.evaluate().assertIsBoolean(
                                    operand2.getParseLocation()).getValue());
            }
        },
        LOOK_UP_KEY(true) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return operand1.evaluate().assertIsDict(
                        operand1.getParseLocation()).get(operand2.evaluate());
            }
        },
        INDEX_SEQ(true) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                VSeq sequenceValue = operand1.evaluate().assertIsSeq(
                        operand1.getParseLocation());
                VNumber indexValue = operand2.evaluate().assertIsNumber(
                        operand2.getParseLocation());

                if (!indexValue.getDenominator().equals(BigInteger.ONE)) {
                    throw InternalExecutionException.nonIntegral("index",
                            operand2.getParseLocation(), indexValue);
                }
                else if (indexValue.getNumerator().compareTo(
                        BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                    throw InternalExecutionException.tooLarge("index",
                            operand2.getParseLocation(), indexValue);
                }
                else if (indexValue.getNumerator()
                        .compareTo(BigInteger.ZERO) < 0) {
                    throw InternalExecutionException.negative("index",
                            operand2.getParseLocation(), indexValue);
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
                return VBoolean.of(operand1.evaluate().assertIsNumber(
                            operand1.getParseLocation())
                        .compareTo(operand2.evaluate().assertIsNumber(
                            operand2.getParseLocation())) < 0);
            }
        },
        LESS_THAN_EQ(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return VBoolean.of(operand1.evaluate().assertIsNumber(
                            operand1.getParseLocation())
                        .compareTo(operand2.evaluate().assertIsNumber(
                            operand2.getParseLocation())) <= 0);
            }
        },
        GREATER_THAN_EQ(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return VBoolean.of(operand1.evaluate().assertIsNumber(
                            operand1.getParseLocation())
                        .compareTo(operand2.evaluate().assertIsNumber(
                            operand2.getParseLocation())) >= 0);
            }
        },
        GREATER_THAN(false) {
            @Override
            public Value apply(Lazy operand1, Lazy operand2) {
                return VBoolean.of(operand1.evaluate().assertIsNumber(
                            operand1.getParseLocation())
                        .compareTo(operand2.evaluate().assertIsNumber(
                            operand2.getParseLocation())) > 0);
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
    
    public BinaryExpression(ParseLocation l, Expression operand1,
            Operator operator, Expression operand2) {
        super(l);
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

                    @Override
                    public ParseLocation getParseLocation() {
                        return myOperand1.getParseLocation();
                    }
                };
        Lazy op2Val = new Lazy() {
                    @Override
                    public Value noCacheEvaluate() {
                        return myOperand2.evaluate(h, s);
                    }
                    
                    @Override
                    public ParseLocation getParseLocation() {
                        return myOperand2.getParseLocation();
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
    
    public static abstract class Lazy {
        private Value myCachedValue;
        
        public final Value evaluate() {
            if (myCachedValue == null) {
                myCachedValue = noCacheEvaluate();
            }
            
            return myCachedValue;
        }
        
        public abstract Value noCacheEvaluate();
        public abstract ParseLocation getParseLocation();
    }
}
