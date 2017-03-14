package sbsdl;

import com.shieldsbetter.flexcompilator.NoMatchException;
import com.shieldsbetter.flexcompilator.ParseHead;
import com.shieldsbetter.flexcompilator.WellFormednessException;
import com.shieldsbetter.flexcompilator.matchers.COneOf;
import com.shieldsbetter.flexcompilator.matchers.CSet;
import com.shieldsbetter.flexcompilator.matchers.CSubtract;
import com.shieldsbetter.flexcompilator.matchers.MAction;
import com.shieldsbetter.flexcompilator.matchers.MAlternatives;
import com.shieldsbetter.flexcompilator.matchers.MCapture;
import com.shieldsbetter.flexcompilator.matchers.MDo;
import com.shieldsbetter.flexcompilator.matchers.MEndOfInput;
import com.shieldsbetter.flexcompilator.matchers.MForbid;
import com.shieldsbetter.flexcompilator.matchers.MLiteral;
import com.shieldsbetter.flexcompilator.matchers.MOptional;
import com.shieldsbetter.flexcompilator.matchers.MPlaceholder;
import com.shieldsbetter.flexcompilator.matchers.MSequence;
import com.shieldsbetter.flexcompilator.matchers.MRepeated;
import com.shieldsbetter.flexcompilator.matchers.MWithSkipper;
import com.shieldsbetter.flexcompilator.matchers.Matcher;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import sbsdl.expressions.BinaryExpression;
import sbsdl.expressions.DictionaryExpression;
import sbsdl.expressions.Expression;
import sbsdl.expressions.FunctionCallExpression;
import sbsdl.expressions.HostExpression;
import sbsdl.expressions.PickExpression;
import sbsdl.expressions.SequenceExpression;
import sbsdl.expressions.UnaryExpression;
import sbsdl.expressions.VariableNameExpression;
import sbsdl.statements.EvaluateStatement;
import sbsdl.statements.FieldAssignmentStatement;
import sbsdl.statements.ForEachStatement;
import sbsdl.statements.IfStatement;
import sbsdl.statements.MultiplexingStatement;
import sbsdl.statements.ReturnStatement;
import sbsdl.statements.SequenceAssignmentStatement;
import sbsdl.statements.Statement;
import sbsdl.statements.TopLevelVariableAssignmentStatement;
import sbsdl.values.VBoolean;
import sbsdl.values.VFunction;
import sbsdl.values.VNumber;
import sbsdl.values.VString;
import sbsdl.values.Value;

public class Sbsdl {    
    public static final boolean DEBUG = true;
    
    private final MPlaceholder EXP = new MPlaceholder();
    private final MPlaceholder CODE_BLOCK = new MPlaceholder();
    
    private final Matcher STRING_INNARDS =
            new MCapture(new MRepeated(
                    new MAlternatives(
                            new MForbid(new MLiteral("\n"), "Encountered end "
                                    + "of line before close of string "
                                    + "literal."),
                            new CSubtract(CSet.ANY, new COneOf('\'', '\\')),
                            new MSequence(
                                    new MLiteral("\\"),
                                    new MAlternatives(
                                            new MLiteral("n"),
                                            new MLiteral("r"),
                                            new MLiteral("'"),
                                            new MForbid(CSet.ANY,
                                                    "Unrecognized control "
                                                    + "character in string."))),
                            new MForbid(MEndOfInput.INSTANCE, "Encountered end "
                                    + "of input before string closed.")),
                    0, 300));
    
    private final Matcher STRING_LITERAL =
            new MAction(
                    new MWithSkipper(
                            new MSequence(new MLiteral("'"), STRING_INNARDS,
                                    new MLiteral("'")),
                            null)) {
                @Override
                public void onMatched(ParseHead h) {
                    myParseStack.push(new VString(h.nextCapture()));
                }
            };
    
    private final Matcher FRACTIONAL_LITERAL =
            new MAction(
                    new MSequence(
                            new MCapture(
                                    new MRepeated(CSet.ISO_LATIN_DIGIT, 0, 10)),
                            new MLiteral("."),
                            new MCapture(new MRepeated(
                                    CSet.ISO_LATIN_DIGIT, 1, 10)))) {
                @Override
                public void onMatched(ParseHead h) {
                    String integralPartText = h.nextCapture();
                    String fractionalPartText = h.nextCapture();
                    
                    long integralPart = Long.parseLong(integralPartText);
                    VNumber n = VNumber.of(integralPart, 1);
                    
                    long pow = 1;
                    for (int i = 0; i < fractionalPartText.length(); i++) {
                        pow *= 10;
                    }
                    n = n.multiply(pow);
                    n = n.add(Long.parseLong(fractionalPartText));
                    n = n.divide(pow);
                    
                    myParseStack.push(n);
                }
            };
    
    private final Matcher INTEGRAL_LITERAL =
            new MAction(
                    new MCapture(new MRepeated(CSet.ISO_LATIN_DIGIT, 1, 10))) {
                @Override
                public void onMatched(ParseHead h) {
                    String integerText = h.nextCapture();
                    VNumber n = VNumber.of(Long.parseLong(integerText), 1);
                    
                    myParseStack.push(n);
                }
            };
    
    // Fractional has to come first so that integral doesn't gobble up its
    // integer part.
    private final Matcher NUMERIC_LITERAL =
            new MWithSkipper(
                    new MAlternatives(FRACTIONAL_LITERAL, INTEGRAL_LITERAL),
                    null);
    
    private final Matcher IDENTIFIER =
            new MAction(new MCapture(new MWithSkipper(
                    new MAlternatives(
                            new MSequence(CSet.LETTER,
                                    new MRepeated(new MAlternatives(
                                            CSet.LETTER_OR_DIGIT,
                                            new MLiteral("_")))),
                            new MLiteral("@")),
                    null))) {
                @Override
                public void onMatched(ParseHead h) {
                    myParseStack.push(h.nextCapture());
                }
            };
    
    private final Matcher EXP_FOR_PARAM_LIST =
            new MAction(EXP) {
                @Override
                public void onMatched(ParseHead h) {
                    Expression expValue = (Expression) myParseStack.pop();
                    ((List) myParseStack.peek()).add(expValue);
                }
            };
    
    private final Matcher PARAM_LIST_INNARDS =
            new MAction(new MOptional(EXP_FOR_PARAM_LIST,
                    new MRepeated(new MLiteral(","), EXP_FOR_PARAM_LIST))) {
                @Override
                public void before(ParseHead h) {
                    myParseStack.push(new LinkedList());
                }
            };
    
    private final Matcher HOST_EXP =
            new MAction(new MSequence(
                    new MAction(new MWithSkipper(
                            new MSequence(new MLiteral("#"),
                                    new MCapture(
                                            CSet.LETTER,
                                            new MRepeated(new MAlternatives(
                                                    CSet.LETTER_OR_DIGIT,
                                                    new MLiteral("_"))))),
                            null)) {
                        @Override
                        public void onMatched(ParseHead h) {
                            myParseStack.push(h.nextCapture());
                        }
                    },
                    new MOptional(
                            new MAction(new MLiteral("(")) {
                                @Override
                                public void onFailed(ParseHead h) {
                                    myParseStack.push(null);
                                }
                            },
                            PARAM_LIST_INNARDS,
                            new MLiteral(")")))) {
                @Override
                public void onMatched(ParseHead h) {
                    List parameterList = (List) myParseStack.pop();
                    String hostId = (String) myParseStack.pop();

                    myParseStack.push(
                            new HostExpression(hostId, parameterList));
                }
            };
    
    private final Matcher PARENTHETICAL_EXP =
            new MSequence(new MLiteral("("), EXP, new MLiteral(")"));
    
    private final Matcher NAKED_STRING_IDENTIFIER =
            new MAction(IDENTIFIER) {
                @Override
                public void onMatched(ParseHead h) {
                    myParseStack.push(new VString((String) myParseStack.pop()));
                }
            };
    
    private final Matcher SINGLE_ARGUMENT_DECLARATION =
            new MAction(IDENTIFIER) {
                @Override
                public void onMatched(ParseHead h) {
                    String id = (String) myParseStack.pop();
                    ((List) myParseStack.peek()).add(id);
                }
            };
    
    private final Matcher ARGUMENT_DECLARATION_INNARDS =
            new MAction(
                    new MOptional(SINGLE_ARGUMENT_DECLARATION, 
                            new MRepeated(new MLiteral(","),
                                    SINGLE_ARGUMENT_DECLARATION))) {
                @Override
                public void before(ParseHead h) {
                    myParseStack.push(new LinkedList());
                }
            };
    
    private final Matcher FUNCTION_LITERAL =
            new MAction(new MSequence(new MLiteral("fn"), new MLiteral("("),
                    ARGUMENT_DECLARATION_INNARDS, new MLiteral(")"),
                    CODE_BLOCK)) {
                @Override
                public void onMatched(ParseHead h) {
                    MultiplexingStatement code =
                            (MultiplexingStatement) myParseStack.pop();
                    myParseStack.push(
                            new VFunction(
                                    (List<String>) myParseStack.pop(), code));
                }
            };
    
    private final Matcher BOUNDED_EXP = new MAlternatives(
            new MAction(FUNCTION_LITERAL) {
                @Override
                public void onMatched(ParseHead h) {
                    final Expression e = (Expression) myParseStack.pop();
                    myParseStack.push(new LHSAccess() {
                                @Override
                                public Expression toValue() {
                                    return e;
                                }

                                @Override
                                public Statement toAssignment(Expression value)
                                        throws WellFormednessException {
                                    throw new WellFormednessException("Cannot "
                                            + "assign to function literal.");
                                }
                            });
                }
            },
            new MAction(STRING_LITERAL) {
                @Override
                public void onMatched(ParseHead h)
                        throws WellFormednessException {
                    final Expression e = (Expression) myParseStack.pop();
                    myParseStack.push(new LHSAccess() {
                                @Override
                                public Expression toValue() {
                                    return e;
                                }

                                @Override
                                public Statement toAssignment(Expression value)
                                        throws WellFormednessException {
                                    throw new WellFormednessException("Cannot "
                                            + "assign to string literal.");
                                }
                            });
                }
            },
            new MAction(NUMERIC_LITERAL) {
                @Override
                public void onMatched(ParseHead h)
                        throws WellFormednessException {
                    final Expression e = (Expression) myParseStack.pop();
                    myParseStack.push(new LHSAccess() {
                                @Override
                                public Expression toValue() {
                                    return e;
                                }

                                @Override
                                public Statement toAssignment(Expression value)
                                        throws WellFormednessException {
                                    throw new WellFormednessException("Cannot "
                                            + "assign to numeric literal.");
                                }
                            });
                }
            },
            new MAction(PARENTHETICAL_EXP) {
                @Override
                public void onMatched(ParseHead h)
                        throws WellFormednessException {
                    final Expression e = (Expression) myParseStack.pop();
                    myParseStack.push(new LHSAccess() {
                                @Override
                                public Expression toValue() {
                                    return e;
                                }

                                @Override
                                public Statement toAssignment(Expression value)
                                        throws WellFormednessException {
                                    throw new WellFormednessException("Cannot "
                                            + "assign to parenthetical "
                                            + "expression.");
                                }
                            });
                }
            },
            new MAction(HOST_EXP) {
                @Override
                public void onMatched(ParseHead h)
                        throws WellFormednessException {
                    final Expression e = (Expression) myParseStack.pop();
                    myParseStack.push(new LHSAccess() {
                                @Override
                                public Expression toValue() {
                                    return e;
                                }

                                @Override
                                public Statement toAssignment(Expression value)
                                        throws WellFormednessException {
                                    throw new WellFormednessException("Cannot "
                                            + "assign to a host expression.");
                                }
                            });
                }
            },
            new MAction(IDENTIFIER) {
                @Override
                public void onMatched(ParseHead h)
                        throws WellFormednessException {
                    final String e = (String) myParseStack.pop();
                    myParseStack.push(new LHSAccess() {
                                @Override
                                public Expression toValue() {
                                    return new VariableNameExpression(e);
                                }

                                @Override
                                public Statement toAssignment(
                                        Expression value) {
                                    return new TopLevelVariableAssignmentStatement(
                                            e, value);
                                }
                            });
                }
            });
    
    private final Matcher LHS =
            new MSequence(BOUNDED_EXP, new MRepeated(new MAlternatives(
                    new MAction(new MSequence(new MLiteral("."), 
                            new MAlternatives(NAKED_STRING_IDENTIFIER,
                                    PARENTHETICAL_EXP))) {
                        @Override
                        public void onMatched(ParseHead h) {
                            final Expression key =
                                    (Expression) myParseStack.pop();
                            final Expression dict =
                                    (Expression) myParseStack.pop();
                            
                            myParseStack.push(
                                    new LHSAccess() {
                                        @Override
                                        public Expression toValue() {
                                            return new BinaryExpression(dict,
                                                    BinaryExpression.Operator
                                                            .LOOK_UP_KEY,
                                                    key);
                                        }

                                        @Override
                                        public Statement toAssignment(
                                                Expression value) {
                                            return new FieldAssignmentStatement(
                                                    dict, key, value);
                                        }
                                    });
                        }
                    },
                    new MAction(new MSequence(
                            new MLiteral("["), EXP, new MLiteral("]"))) {
                        @Override
                        public void onMatched(ParseHead h) {
                            final Expression index =
                                    (Expression) myParseStack.pop();
                            final Expression seq =
                                    (Expression) myParseStack.pop();
                            
                            myParseStack.push(
                                    new LHSAccess() {
                                        @Override
                                        public Expression toValue() {
                                            return new BinaryExpression(seq,
                                                    BinaryExpression.Operator
                                                            .INDEX_SEQ,
                                                    index);
                                        }

                                        @Override
                                        public Statement toAssignment(
                                                Expression value) {
                                            return new SequenceAssignmentStatement(
                                                    seq, index, value);
                                        }
                                    });
                        }
                    },
                    new MAction(new MSequence(
                            new MLiteral("("), PARAM_LIST_INNARDS,
                            new MLiteral(")"))) {
                        @Override
                        public void onMatched(ParseHead h) {
                            final List<Expression> args =
                                    (List) myParseStack.pop();
                            final Expression f =
                                    (Expression) myParseStack.pop();
                            
                            myParseStack.push(new LHSAccess() {
                                        @Override
                                        public Expression toValue() {
                                            return new FunctionCallExpression(
                                                    f, args);
                                        }

                                        @Override
                                        public Statement toAssignment(
                                                Expression value)
                                                throws WellFormednessException {
                                            throw new WellFormednessException(
                                                    "Expected end of "
                                                    + "statement.");
                                        }
                                    });
                        }
                    }
            )));
    
    private final Matcher LHS_EXP =
            new MAction(LHS) {
                @Override
                public void onMatched(ParseHead h) {
                    myParseStack.push(
                            ((LHSAccess) myParseStack.pop()).toValue());
                }
            };
    
    private final Matcher PREFIX_EXP =
            new MSequence(
                    new MRepeated(
                            new MPush(new MLiteral("!"),
                                    UnaryExpression.Operator.BOOLEAN_NEGATE)),
                    new MAction(LHS_EXP) {
                        @Override
                        public void onMatched(ParseHead h) {
                            Expression baseExp =
                                    (Expression) myParseStack.pop();
                            while (!myParseStack.isEmpty()
                                    && myParseStack.peek() instanceof
                                        UnaryExpression.Operator) {
                                baseExp = new UnaryExpression(
                                        (UnaryExpression.Operator)
                                                myParseStack.pop(),
                                        baseExp);
                            }
                            
                            myParseStack.push(baseExp);
                        }
                    });
    
    private final Matcher POW_PRECEDENCE_EXP =
            new MSequence(PREFIX_EXP, new MRepeated(
                    new MAction(new MSequence(new MLiteral("^"), PREFIX_EXP)) {
                        @Override
                        public void onMatched(ParseHead h) {
                            Expression right = (Expression) myParseStack.pop();
                            Expression left = (Expression) myParseStack.pop();
                            
                            myParseStack.push(new BinaryExpression(left,
                                    BinaryExpression.Operator.RAISED_TO,
                                    right));
                        }
                    }));
    
    private final Matcher MULT_PRECEDENCE_EXP =
            new MSequence(POW_PRECEDENCE_EXP, new MRepeated(
                    new MBinaryExpression(
                            new MAlternatives(
                                    new MPush(new MLiteral("*"),
                                            BinaryExpression.Operator.TIMES),
                                    new MPush(new MLiteral("/"),
                                            BinaryExpression.Operator
                                                    .DIVIDED_BY)),
                            POW_PRECEDENCE_EXP)));
    
    private final Matcher ADD_PRECEDENCE_EXP =
            new MSequence(MULT_PRECEDENCE_EXP, new MRepeated(
                    new MBinaryExpression(new MAlternatives(
                            new MPush(new MLiteral("+"),
                                    BinaryExpression.Operator.PLUS),
                            new MPush(new MLiteral("-"),
                                    BinaryExpression.Operator.MINUS)),
                            MULT_PRECEDENCE_EXP)));
    
    private final Matcher NUMERIC_EXP = ADD_PRECEDENCE_EXP;
    
    private final Matcher BINARY_COMPARISON_EXP =
            new MSequence(NUMERIC_EXP,
                    new MRepeated(new MBinaryExpression(
                            new MAlternatives(
                                    // Order important!  Prefixes can't come
                                    // first!
                                    new MPush(new MLiteral("!="),
                                            BinaryExpression.Operator
                                                    .NOT_EQUAL),
                                    new MPush(new MLiteral("<="),
                                            BinaryExpression.Operator.EQUAL),
                                    new MPush(new MLiteral("<"),
                                            BinaryExpression.Operator
                                                    .LESS_THAN),
                                    new MPush(new MLiteral("="),
                                            BinaryExpression.Operator.EQUAL),
                                    new MPush(new MLiteral(">="),
                                            BinaryExpression.Operator
                                                    .GREATER_THAN_EQ),
                                    new MPush(new MLiteral(">"),
                                            BinaryExpression.Operator
                                                    .GREATER_THAN)),
                            NUMERIC_EXP)));
    
    private final Matcher COMPARISON_EXP = BINARY_COMPARISON_EXP;
    
    private final Matcher AND_PRECEDENCE_EXP =
            new MSequence(COMPARISON_EXP, new MRepeated(
                    new MBinaryExpression(
                            new MPush(new MLiteral("and"),
                                    BinaryExpression.Operator.AND),
                            COMPARISON_EXP)));
    
    private final Matcher OR_PRECEDENCE_EXP =
            new MSequence(AND_PRECEDENCE_EXP, new MRepeated(
                    new MBinaryExpression(
                            new MPush(new MLiteral("or"),
                                    BinaryExpression.Operator.OR),
                            AND_PRECEDENCE_EXP)));
    
    private final Matcher BOOLEAN_LEVEL_EXPRESSION = OR_PRECEDENCE_EXP;
    
    private final Matcher POOL_ELEMENT =
            new MSequence(EXP,
                    new MOptional(
                            new MAction(new MSequence(
                                    new MLiteral("{"), EXP,
                                    new MLiteral("}"))) {
                                @Override
                                public void onMatched(ParseHead h) {
                                    Expression weight =
                                            (Expression) myParseStack.pop();
                                    Expression exp =
                                            (Expression) myParseStack.pop();
                                    
                                    ((List) myParseStack.peek()).add(
                                            new PickSetEntry(exp, weight));
                                }
                                        
                                @Override
                                public void onFailed(ParseHead h) {
                                    Expression exp =
                                            (Expression) myParseStack.pop();
                                    
                                    ((List) myParseStack.peek()).add(
                                            new PickSetEntry(exp, null));
                                }
                            }));
    
    private final Matcher FROM_POOL = new MSequence(
            new MOptional(
                    new MAction(new MSequence(IDENTIFIER, new MLiteral(":"))) {
                        @Override
                        public void onFailed(ParseHead h) {
                            myParseStack.push("@");
                        }
                    }),
            new MAlternatives(
                    new MAction(EXP) {
                        @Override
                        public void onMatched(ParseHead h) {
                            myParseStack.push(new IntermediatePickExpression(
                                    (Expression) myParseStack.pop()));
                        }
                    },
                    new MSequence(
                            new MAction(new MLiteral("{")) {
                                @Override
                                public void onMatched(ParseHead h) {
                                    myParseStack.push(new LinkedList());
                                }
                            },
                            new MOptional(POOL_ELEMENT), 
                                    new MRepeated(
                                            new MLiteral(","), POOL_ELEMENT),
                            new MAction(new MLiteral("}")) {
                                @Override
                                public void onMatched(ParseHead h)
                                        throws WellFormednessException {
                                    Map<Expression, Expression> weighter =
                                            new HashMap<>();
                                    List<Expression> seq = new LinkedList<>();
                                    
                                    boolean explicitWeight = false;
                                    
                                    List<PickSetEntry> pickSetEntries =
                                            (List) myParseStack.pop();
                                    for (PickSetEntry elObj : pickSetEntries) {
                                        seq.add(elObj.getExpression());
                                        
                                        if (seq.size() == 1) {
                                            explicitWeight =
                                                    (elObj.getWeight() != null);
                                        }
                                        else {
                                            if (explicitWeight
                                                    && elObj.getWeight()
                                                        == null) {
                                                throw new WellFormednessException(
                                                        "If first element of "
                                                        + "pool has an explicit"
                                                        + " weight, then all "
                                                        + "elements must have "
                                                        + "explicit weights.");
                                            }
                                            else if (!explicitWeight
                                                    && elObj.getWeight()
                                                        != null) {
                                                throw new WellFormednessException(
                                                        "If first element of "
                                                        + "pool has no explicit"
                                                        + " weight, no elements"
                                                        + " may have explicit "
                                                        + "weight.");
                                            }
                                        }
                                        
                                        if (elObj.getWeight() != null) {
                                            weighter.put(elObj.getExpression(),
                                                    elObj.getWeight());
                                        }
                                    }
                                    
                                    IntermediatePickExpression ipe =
                                            new IntermediatePickExpression(
                                                    new SequenceExpression(
                                                            seq));
                                    if (explicitWeight) {
                                        ipe.fillInWeighter(
                                                new DictionaryExpression(
                                                        weighter));
                                    }
                                    
                                    myParseStack.push(ipe);
                                }
                            })));
    
    private final Matcher WHERE_CLAUSE =
            new MAction(new MSequence(new MLiteral("where"),
                    BOOLEAN_LEVEL_EXPRESSION)) {
                @Override
                public void onFailed(ParseHead h) {
                    myParseStack.push(VBoolean.TRUE);
                }
            };
    
    /**
     * <p>Results in {@code [where_expression, IntermediatePickExpression,
     * exemplar_string, ...]}.</p>
     */
    private final Matcher REQUIRED_SINGLE_PICK_TAIL_EXP =
            new MSequence(FROM_POOL, new MOptional(WHERE_CLAUSE));
    
    private final Matcher PICK_COUNT_SPECIFIER = new MSequence(
            new MOptional(new MAction(NUMERIC_EXP) {
                @Override
                public void onFailed(ParseHead h) {
                    myParseStack.push(VNumber.of(1, 1));
                }
            }),
            new MOptional(new MAction(new MSequence(
                    new MLiteral("unique"),
                    new MAction(PARENTHETICAL_EXP) {
                        @Override
                        public void onFailed(ParseHead h) {
                            myParseStack.push(VBoolean.TRUE);
                        }
                    })) {
                @Override
                public void onFailed(ParseHead h) {
                    myParseStack.push(VBoolean.FALSE);
                }
            }));
    
    private final Matcher REQUIRED_PICK_EXP = new MAction(new MSequence(
                    new MLiteral("pick"), PICK_COUNT_SPECIFIER,
                    new MLiteral("from"), REQUIRED_SINGLE_PICK_TAIL_EXP)) {
                @Override
                public void onMatched(ParseHead h) {
                    Expression where = (Expression) myParseStack.pop();
                    IntermediatePickExpression ipe =
                            (IntermediatePickExpression) myParseStack.pop();
                    String exemplar = (String) myParseStack.pop();
                    Expression unique = (Expression) myParseStack.pop();
                    Expression count = (Expression) myParseStack.pop();
                    
                    myParseStack.push(new PickExpression(exemplar,
                            ipe.getPool(), count, unique, ipe.getWeighter(),
                            where));
                }
            };
    
    // Explicit pick has to come first so we don't gobble up "pick" as an
    // identifier.
    private final Matcher PICK_EXP =
            new MAlternatives(REQUIRED_PICK_EXP, BOOLEAN_LEVEL_EXPRESSION);
    
    {
        EXP.fillIn(PICK_EXP);
    }
    
    private final MPlaceholder STATEMENT = new MPlaceholder();
    
    private final Matcher CODE = new MSequence(
            new MDo() {
                @Override
                public void run() {
                    myParseStack.push(new LinkedList());
                }
            },
            new MRepeated(new MAction(STATEMENT) {
                @Override
                public void onMatched(ParseHead h) {
                    Statement stmt = (Statement) myParseStack.pop();
                    ((List) myParseStack.peek()).add(stmt);
                }
            }),
            new MDo() {
                @Override
                public void run() {
                    List<Statement> stmts =
                            (List<Statement>) myParseStack.pop();
                    myParseStack.push(new MultiplexingStatement(stmts));
                }
            });
    
    {
        CODE_BLOCK.fillIn(new MLiteral("{"), CODE, new MLiteral("}"));
    }
    
    private final Matcher ASSIGN_OR_CALL_STMT = new MSequence(
            LHS,
            new MAlternatives(
                    new MAction(new MLiteral(";")) {
                        @Override
                        public void onMatched(ParseHead h)
                                throws WellFormednessException {
                            Expression eval =
                                    ((LHSAccess) myParseStack.pop()).toValue();
                            
                            if (!(eval instanceof FunctionCallExpression)
                                    && !(eval instanceof HostExpression)) {
                                throw new WellFormednessException(
                                        "Expected statement.");
                            }
                            
                            myParseStack.push(new EvaluateStatement(eval));
                        }
                    },
                    new MAction(new MSequence(
                            new MLiteral("="), EXP, new MLiteral(";"))) {
                        @Override
                        public void onMatched(ParseHead h)
                                throws WellFormednessException {
                            Expression value = (Expression) myParseStack.pop();
                            
                            myParseStack.push(((LHSAccess) myParseStack.pop())
                                    .toAssignment(value));
                        }
                    }
            ));
    
    private final Matcher FOR_EACH_STMT =
            new MAction(new MSequence(new MLiteral("for"), new MLiteral("each"),
                    REQUIRED_SINGLE_PICK_TAIL_EXP, CODE_BLOCK)) {
                @Override
                public void onMatched(ParseHead h)
                        throws WellFormednessException {
                    Statement codeBlock = (Statement) myParseStack.pop();
                    Expression where = (Expression) myParseStack.pop();
                    IntermediatePickExpression ipe =
                            (IntermediatePickExpression) myParseStack.pop();
                    String exemplar = (String) myParseStack.pop();
                    
                    if (ipe.getWeighter() != null) {
                        throw new WellFormednessException("Weights not allowed "
                                + "in for-each statement.");
                    }
                    
                    ((List) myParseStack.peek()).add(new ForEachStatement(
                            exemplar, ipe.getPool(), where, codeBlock));
                }
            };
    
    private final Matcher IF_STMT = new MSequence(
            new MLiteral("if"),
            EXP, CODE_BLOCK,
            new MDo() {
                @Override
                public void run() {
                    myParseStack.push(new LinkedList());
                    
                    Statement codeBlock = (Statement) myParseStack.pop();
                    Expression exp = (Expression) myParseStack.pop();
                    ((Deque) myParseStack.peek()).push(exp);
                    ((Deque) myParseStack.peek()).push(codeBlock);
                }
            },
            new MRepeated(new MLiteral("else"), new MLiteral("if"), EXP,
                    CODE_BLOCK,
                    new MDo() {
                        @Override
                        public void run() {
                            Statement codeBlock =
                                    (Statement) myParseStack.pop();
                            Expression exp = (Expression) myParseStack.pop();
                            ((Deque) myParseStack.peek()).push(exp);
                            ((Deque) myParseStack.peek()).push(codeBlock);
                        }
                    }),
            new MOptional(new MLiteral("else"), CODE_BLOCK,
                    new MDo() {
                        @Override
                        public void run() {
                            Statement codeBlock =
                                    (Statement) myParseStack.pop();
                            ((Deque) myParseStack.peek()).push(VBoolean.TRUE);
                            ((Deque) myParseStack.peek()).push(codeBlock);
                        }
                    }),
            new MDo() {
                @Override
                public void run() {
                    Deque ifComponents = (Deque) myParseStack.pop();
                    
                    Statement curIf = Statement.NO_OP;
                    do {
                        Statement onTrue = (Statement) ifComponents.pop();
                        Expression condition = (Expression) ifComponents.pop();
                        
                        curIf = new IfStatement(condition, onTrue, curIf);
                    } while (!ifComponents.isEmpty());
                    
                    myParseStack.push(curIf);
                }
            });
    
    private final Matcher RETURN_STMT =
            new MAction(new MSequence(new MLiteral("return"), EXP)) {
                @Override
                public void onMatched(ParseHead h) {
                    myParseStack.push(new ReturnStatement(
                            (Expression) myParseStack.pop()));
                }
            };
    
    {
        STATEMENT.fillIn(new MAlternatives(
                RETURN_STMT, FOR_EACH_STMT, IF_STMT, ASSIGN_OR_CALL_STMT));
    }
    
    private final MPlaceholder COMMENT = new MPlaceholder();
    {
        COMMENT.fillIn(new MWithSkipper(new MSequence(new MLiteral("(*"),
                new MRepeated(new MAlternatives(
                        new CSubtract(CSet.ANY, new COneOf('*')),
                        new MSequence(new MLiteral("*"),
                                new CSubtract(CSet.ANY, new COneOf(')'))),
                        COMMENT)), new MLiteral("*)")), null));
    }
    
    private final Matcher DEFAULT_SKIPPER =
            new MWithSkipper(
                    new MRepeated(new MAlternatives(COMMENT, CSet.WHITESPACE)),
                    null);
    
    private final ParseStack myParseStack = new ParseStack();
    private final HostEnvironment myHostEnvironment;
    
    public Sbsdl(HostEnvironment e) {
        myHostEnvironment = e;
    }
    
    public void run(String input) throws WellFormednessException {
        ParseHead h = new ParseHead(input);
        h.setSkip(DEFAULT_SKIPPER);
        
        try {
            h.advanceOver(CODE);
        }
        catch (NoMatchException nme) {
            throw new WellFormednessException("Couldn't parse input.");
        }
        
        ScriptEnvironment s = new ScriptEnvironment();
        ((MultiplexingStatement) myParseStack.pop())
                .execute(myHostEnvironment, s);
        
        if (!myParseStack.isEmpty()) {
            throw new RuntimeException();
        }
    }
    
    public void parse(String input)
            throws NoMatchException, WellFormednessException {
        ParseHead h = new ParseHead(input);
        h.setSkip(DEFAULT_SKIPPER);
        h.advanceOver(CODE);
        
        System.out.println("REMAINING TEXT: " + h.remainingText());
    }
    
    public class HostEnvironmentException extends Exception {
        public HostEnvironmentException(String msg) {
            super(msg);
        }
    }
    
    public static interface HostEnvironment {
        /**
         * <p>Called to evaluate host expressions like {@code #foo} or
         * {@code #bar(5)}.</p>
         * 
         * <p>{@code name} will contain the identifier after the
         * hash mark ({@code foo} or {@code bar} in the previous examples).</p>
         * 
         * <p>If the host expression has a parameter list (including the case
         * where it has an empty parameter list), then {@code parameters} will
         * contain the evaluated value of each of those parameters in order (or
         * be the empty list in the case where the parameter list is empty.)  If
         * the host expression does not have a parameter list,
         * {@code parameters} will be {@code null}.  {@code #bazz} can thus be
         * distinguished from {@code #bazz()} by whether or not
         * {@code parameters} is {@code null}.</p>
         * 
         * @param name The text of the identifier following the hash mark.
         * @param parameters The list of evaluated parameters provided in the
         *              parameter list, or {@code null} if no parameter list is
         *              present.
         * @return The evaluated value of the host expression.  May not return
         *         {@code null}.
         * 
         * @throws sbsdl.Sbsdl.HostEnvironmentException If for any reason the
         *               given host expression cannot be evaluated into a
         *               {@link Value}.
         */
        public Value evaluate(String name, List<Value> parameters)
                throws HostEnvironmentException;
    }
    
    public static class ExecutionException extends RuntimeException {
        public ExecutionException(String msg) {
            super(msg);
        }
    }
    
    private class MPush extends MAction {
        private final Object myObject;
        private final StackTraceElement myInstantiationLocation;
        
        public MPush(Matcher base, Object o) {
            super(base);
            
            myObject = o;
            myInstantiationLocation = new RuntimeException().getStackTrace()[1];
        }

        @Override
        public void before(ParseHead h) {
            if (DEBUG) {
                System.out.println("MPush start " + myInstantiationLocation);
            }
        }
        
        @Override
        public void onMatched(ParseHead h) {
            myParseStack.push(myObject);
            
            if (DEBUG) {
                System.out.println("MPush matched " + myInstantiationLocation);
            }
        }

        @Override
        public void onFailed(ParseHead h) {
            if (DEBUG) {
                System.out.println("MPush failed " + myInstantiationLocation);
            }
        }
    }
    
    private class MBinaryExpression extends MAction {
        private final StackTraceElement myInstantiationLocation;
        
        public MBinaryExpression(Matcher ... ms) {
            this(new MSequence(ms));
        }
        
        public MBinaryExpression(Matcher base) {
            super(base);
            
            myInstantiationLocation = new RuntimeException().getStackTrace()[1];
        }

        @Override
        public void before(ParseHead h) {
            if (DEBUG) {
                System.out.println(
                        "MBinaryExpression start " + myInstantiationLocation);
            }
        }
        
        @Override
        public void onMatched(ParseHead h) {
            Expression right = (Expression) myParseStack.pop();
            BinaryExpression.Operator op =
                    (BinaryExpression.Operator) myParseStack.pop();
            Expression left = (Expression) myParseStack.pop();

            myParseStack.push(new BinaryExpression(left, op, right));
            
            if (DEBUG) {
                System.out.println(
                        "MBinaryExpression matched " + myInstantiationLocation);
            }
        }

        @Override
        public void onFailed(ParseHead h) {
            if (DEBUG) {
                System.out.println(
                        "MBinaryExpression failed " + myInstantiationLocation);
            }
        }
    }
    
    private static class PickSetEntry {
        private final Expression myExpression;
        private final Expression myWeight;
        
        public PickSetEntry(Expression exp, Expression weight) {
            myExpression = exp;
            myWeight = weight;
        }
        
        public Expression getExpression() {
            return myExpression;
        }
        
        public Expression getWeight() {
            return myWeight;
        }
    }
    
    private static class IntermediatePickExpression {
        private final Expression myCollection;
        private Expression myWeighter;
        
        public IntermediatePickExpression(Expression collection) {
            myCollection = collection;
        }
        
        public Expression getPool() {
            return myCollection;
        }
        
        public Expression getWeighter() {
            return myWeighter;
        }
        
        public void fillInWeighter(Expression w)
                throws WellFormednessException {
            if (myWeighter != null) {
                throw new WellFormednessException("Pick pools with explicit "
                        + "weightings cannot also have a 'weighted by' "
                        + "clause.");
            }
            
            myWeighter = w;
        }
    }
    
    private static interface LHSAccess {
        public Expression toValue();
        public Statement toAssignment(Expression value)
                throws WellFormednessException;
    }
    
    private static class ParseStack {
        private final Deque myStack = new LinkedList();
        
        public boolean isEmpty() {
            return myStack.isEmpty();
        }
        
        public void push(Object o) {
            myStack.push(o);
            
            if (DEBUG) {
                System.out.print("\nPush ");
                debugPrint();
            }
        }
        
        public Object pop() {
            Object result = myStack.pop();
            
            if (DEBUG) {
                System.out.print("\nPop ");
                debugPrint();
            }
            
            return result;
        }
        
        public Object peek() {
            return myStack.peek();
        }
        
        private void debugPrint() {
            System.out.println(new RuntimeException().getStackTrace()[2]);
            
            System.out.print("[");
            
            Deque head = new LinkedList();
            while (head.size() < 4 && !myStack.isEmpty()) {
                if (!head.isEmpty()) {
                    System.out.print(", ");
                }
                
                Object o = myStack.pop();
                System.out.print(o);
                
                head.push(o);
            }
            
            if (!myStack.isEmpty()) {
                System.out.print(", ...");
            }
            
            System.out.println("]");
            
            while (!head.isEmpty()) {
                myStack.push(head.pop());
            }
        }
    }
}
