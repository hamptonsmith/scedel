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
import com.shieldsbetter.flexcompilator.matchers.MRequireAhead;
import com.shieldsbetter.flexcompilator.matchers.MDo;
import com.shieldsbetter.flexcompilator.matchers.MEndOfInput;
import com.shieldsbetter.flexcompilator.matchers.MExclude;
import com.shieldsbetter.flexcompilator.matchers.MForbid;
import com.shieldsbetter.flexcompilator.matchers.MLiteral;
import com.shieldsbetter.flexcompilator.matchers.MOptional;
import com.shieldsbetter.flexcompilator.matchers.MPlaceholder;
import com.shieldsbetter.flexcompilator.matchers.MSequence;
import com.shieldsbetter.flexcompilator.matchers.MRepeated;
import com.shieldsbetter.flexcompilator.matchers.MRequire;
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
import sbsdl.statements.VariableIntroductionStatement;
import sbsdl.values.VBoolean;
import sbsdl.values.VFunction;
import sbsdl.values.VNone;
import sbsdl.values.VNumber;
import sbsdl.values.VString;
import sbsdl.values.VUnavailable;
import sbsdl.values.Value;

public class Sbsdl {    
    public static final boolean DEBUG = true;
    
    private final String[] KEYWORDS =
            {"from", "if", "for", "each", "pick", "unique", "true", "false",
             "unavailable", "none", "intro", "not", "and", "or"};
    
    private final Matcher KEYWORD;
    {
        List<Matcher> keywords = new LinkedList<>();
        for (String keyword : KEYWORDS) {
            keywords.add(new MLiteral(keyword));
        }
        KEYWORD = new MAlternatives(keywords.toArray(new Matcher[0]));
    }
    
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
                                            new MLiteral("t"),
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
                    String innards = h.popCapture();
                    innards = innards.replace("\\n", "\n");
                    innards = innards.replace("\\t", "\t");
                    innards = innards.replace("\\'", "'");
                    
                    h.pushOnParseStack(new VString(innards));
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
                    String fractionalPartText = h.popCapture();
                    String integralPartText = h.popCapture();
                    
                    long integralPart = Long.parseLong(integralPartText);
                    VNumber n = VNumber.of(integralPart, 1);
                    
                    long pow = 1;
                    for (int i = 0; i < fractionalPartText.length(); i++) {
                        pow *= 10;
                    }
                    n = n.multiply(pow);
                    n = n.add(Long.parseLong(fractionalPartText));
                    n = n.divide(pow);
                    
                    h.pushOnParseStack(n);
                }
            };
    
    private final Matcher INTEGRAL_LITERAL =
            new MAction(
                    new MCapture(new MRepeated(CSet.ISO_LATIN_DIGIT, 1, 10))) {
                @Override
                public void onMatched(ParseHead h) {
                    String integerText = h.popCapture();
                    VNumber n = VNumber.of(Long.parseLong(integerText), 1);
                    
                    h.pushOnParseStack(n);
                }
            };
    
    // Fractional has to come first so that integral doesn't gobble up its
    // integer part.
    private final Matcher NUMERIC_LITERAL =
            new MWithSkipper(
                    new MAlternatives(FRACTIONAL_LITERAL, INTEGRAL_LITERAL),
                    null);
    
    private final Matcher SEQUENCE_LITERAL =
            new MSequence(new MLiteral("["),
                    new MDo() {
                        @Override
                        public void run(ParseHead h) {
                            h.pushOnParseStack(new LinkedList());
                        }
                    },
                    new MOptional(EXP, 
                            new MDo() {
                                @Override
                                public void run(ParseHead h) {
                                    Expression exp =
                                        (Expression) h.popFromParseStack();
                                    
                                    ((List) h.peekFromParseStack()).add(exp);
                                }
                            },
                            new MRepeated(new MLiteral(","), EXP,
                                    new MDo() {
                                        @Override
                                        public void run(ParseHead h) {
                                            Expression exp =
                                                (Expression)
                                                    h.popFromParseStack();

                                            ((List) h.peekFromParseStack())
                                                    .add(exp);
                                        }
                                    })),
                    new MLiteral("]"),
                    new MDo() {
                        @Override
                        public void run(ParseHead h) {
                            h.pushOnParseStack(new SequenceExpression(
                                    (List<Expression>) h.popFromParseStack()));
                        }
                    });
    
    private final Matcher IDENTIFIER =
            new MAction(
                    new MExclude(
                            KEYWORD,
                            new MCapture(
                                    new MWithSkipper(
                                        new MAlternatives(
                                                new MSequence(CSet.LETTER,
                                                        new MRepeated(new MAlternatives(
                                                                CSet.LETTER_OR_DIGIT,
                                                                new MLiteral("_")))),
                                                new MLiteral("@")),
                                    null)))) {
                @Override
                public void onMatched(ParseHead h) {
                    h.pushOnParseStack(h.popCapture());
                }
            };
    
    private final Matcher EXP_FOR_PARAM_LIST =
            new MAction(new MRequire(EXP, "Expected expression.")) {
                @Override
                public void onMatched(ParseHead h) {
                    Expression expValue = (Expression) h.popFromParseStack();
                    ((List) h.peekFromParseStack()).add(expValue);
                }
            };
    
    private final Matcher PARAM_LIST_INNARDS =
            new MAction(new MOptional(EXP_FOR_PARAM_LIST,
                    new MRepeated(new MLiteral(","), EXP_FOR_PARAM_LIST))) {
                @Override
                public void before(ParseHead h) {
                    h.pushOnParseStack(new LinkedList());
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
                            h.pushOnParseStack(h.popCapture());
                        }
                    },
                    new MOptional(new MLiteral("("),
                            PARAM_LIST_INNARDS,
                            new MLiteral(")")) {
                        @Override
                        public void onOmitted(ParseHead h) {
                            h.pushOnParseStack(null);
                        }
                    })) {
                @Override
                public void onMatched(ParseHead h) {
                    List parameterList = (List) h.popFromParseStack();
                    String hostId = (String) h.popFromParseStack();

                    h.pushOnParseStack(
                            new HostExpression(hostId, parameterList));
                }
            };
    
    private final Matcher PARENTHETICAL_EXP =
            new MSequence(new MLiteral("("), EXP, new MLiteral(")"));
    
    private final Matcher NAKED_STRING_IDENTIFIER =
            new MAction(IDENTIFIER) {
                @Override
                public void onMatched(ParseHead h) {
                    h.pushOnParseStack(
                            new VString((String) h.popFromParseStack()));
                }
            };
    
    private final Matcher DICTIONARY_FIELD_PAIR = new MSequence(
            NAKED_STRING_IDENTIFIER, 
            new MLiteral(":"),
            EXP,
            new MDo() {
                @Override
                public void run(ParseHead h) {
                    Expression value = (Expression) h.popFromParseStack();
                    VString fieldName = (VString) h.popFromParseStack();
                    ((Map<Expression, Expression>) h.peekFromParseStack())
                            .put(fieldName, value);
                }
            });
    
    private final Matcher DICTIONARY_LITERAL = new MSequence(
            new MLiteral("{"),
            new MDo() {
                @Override
                public void run(ParseHead h) {
                    h.pushOnParseStack(new HashMap());
                }
            },
            new MOptional(DICTIONARY_FIELD_PAIR,
                    new MRepeated(
                            new MRequireAhead(new MAlternatives(
                                    new MLiteral(","), new MLiteral("}")),
                                    "Expected ',' or '}'."),
                            new MLiteral(","),
                            DICTIONARY_FIELD_PAIR)),
            new MLiteral("}"),
            new MDo() {
                @Override
                public void run(ParseHead h) {
                    Map<Expression, Expression> fields =
                            (Map<Expression, Expression>) h.popFromParseStack();
                    
                    DictionaryExpression dict =
                            new DictionaryExpression(fields);
                    h.pushOnParseStack(dict);
                }
            });
    
    private final Matcher SINGLE_ARGUMENT_DECLARATION =
            new MAction(IDENTIFIER) {
                @Override
                public void onMatched(ParseHead h) {
                    String id = (String) h.popFromParseStack();
                    ((List) h.peekFromParseStack()).add(id);
                }
            };
    
    private final Matcher ARGUMENT_DECLARATION_INNARDS =
            new MAction(
                    new MOptional(SINGLE_ARGUMENT_DECLARATION, 
                            new MRepeated(new MLiteral(","),
                                    SINGLE_ARGUMENT_DECLARATION))) {
                @Override
                public void before(ParseHead h) {
                    h.pushOnParseStack(new LinkedList());
                }
            };
    
    private final Matcher FUNCTION_LITERAL =
            new MAction(new MSequence(new MLiteral("fn"), new MLiteral("("),
                    ARGUMENT_DECLARATION_INNARDS, new MLiteral(")"),
                    CODE_BLOCK)) {
                @Override
                public void onMatched(ParseHead h) {
                    MultiplexingStatement code =
                            (MultiplexingStatement) h.popFromParseStack();
                    h.pushOnParseStack(
                            new VFunction((List<String>) h.popFromParseStack(),
                                    code));
                }
            };
    
    private final Matcher BOOLEAN_LITERAL =
            new MAlternatives(
                    new MPush(new MLiteral("true"), VBoolean.TRUE),
                    new MPush(new MLiteral("false"), VBoolean.FALSE));
    
    private final Matcher BOUNDED_EXP = new MAlternatives(
            new MNoAssign(
                    new MPush(new MLiteral("unavailable"),
                            VUnavailable.INSTANCE),
                    "an unavailable literal"),
            new MNoAssign(new MPush(new MLiteral("none"), VNone.INSTANCE),
                    "a none literal"),
            new MNoAssign(BOOLEAN_LITERAL, "a boolean literal"),
            new MNoAssign(DICTIONARY_LITERAL, "a dictionary literal"),
            new MNoAssign(SEQUENCE_LITERAL, "a sequence literal"),
            new MNoAssign(FUNCTION_LITERAL, "a function literal"),
            new MNoAssign(STRING_LITERAL, "a string literal"),
            new MNoAssign(NUMERIC_LITERAL, "a numeric literal"),
            new MNoAssign(PARENTHETICAL_EXP, "a parenthetical expression"),
            new MNoAssign(HOST_EXP, "a host expression"),
            new MAction(IDENTIFIER) {
                @Override
                public void onMatched(ParseHead h)
                        throws WellFormednessException {
                    final String e = (String) h.popFromParseStack();
                    h.pushOnParseStack(new LHSAccess() {
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
                                    (Expression) h.popFromParseStack();
                            final Expression dict =
                                    ((LHSAccess) h.popFromParseStack())
                                            .toValue();
                            
                            h.pushOnParseStack(
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
                                    (Expression) h.popFromParseStack();
                            final Expression seq =
                                    ((LHSAccess) h.popFromParseStack())
                                            .toValue();
                            
                            h.pushOnParseStack(
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
                        public void onMatched(final ParseHead h) {
                            final List<Expression> args =
                                    (List) h.popFromParseStack();
                            final Expression f =
                                    ((LHSAccess) h.popFromParseStack())
                                            .toValue();
                            
                            h.pushOnParseStack(new LHSAccess() {
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
                                                    + "statement.", h);
                                        }
                                    });
                        }
                    }
            )));
    
    private final Matcher LHS_EXP =
            new MAction(LHS) {
                @Override
                public void onMatched(ParseHead h) {
                    h.pushOnParseStack(
                            ((LHSAccess) h.popFromParseStack()).toValue());
                }
            };
    
    private final Matcher PREFIX_EXP =
            new MSequence(
                    new MRepeated(
                            new MPush(new MLiteral("not"),
                                    UnaryExpression.Operator.BOOLEAN_NEGATE)),
                    new MAction(LHS_EXP) {
                        @Override
                        public void onMatched(ParseHead h) {
                            Expression baseExp =
                                    (Expression) h.popFromParseStack();
                            while (!h.isParseStackEmpty()
                                    && (h.peekFromParseStack() instanceof
                                        UnaryExpression.Operator)) {
                                baseExp = new UnaryExpression(
                                        (UnaryExpression.Operator)
                                                h.popFromParseStack(),
                                        baseExp);
                            }
                            
                            h.pushOnParseStack(baseExp);
                        }
                    });
    
    private final Matcher POW_PRECEDENCE_EXP =
            new MSequence(PREFIX_EXP, new MRepeated(
                    new MAction(new MSequence(new MLiteral("^"), PREFIX_EXP)) {
                        @Override
                        public void onMatched(ParseHead h) {
                            Expression right =
                                    (Expression) h.popFromParseStack();
                            Expression left =
                                    (Expression) h.popFromParseStack();
                            
                            h.pushOnParseStack(new BinaryExpression(left,
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
                                            (Expression) h.popFromParseStack();
                                    Expression exp =
                                            (Expression) h.popFromParseStack();
                                    
                                    ((List) h.peekFromParseStack()).add(
                                            new PickSetEntry(exp, weight));
                                }
                                        
                                @Override
                                public void onFailed(ParseHead h) {
                                    Expression exp =
                                            (Expression) h.popFromParseStack();
                                    
                                    ((List) h.peekFromParseStack()).add(
                                            new PickSetEntry(exp, null));
                                }
                            }));
    
    private final Matcher FROM_POOL = new MSequence(
            new MOptional(new MSequence(IDENTIFIER, new MLiteral(":"))) {
                @Override
                public void onOmitted(ParseHead h) {
                    h.pushOnParseStack("@");
                }
            },
            new MAlternatives(
                    new MAction(EXP) {
                        @Override
                        public void onMatched(ParseHead h) {
                            h.pushOnParseStack(new IntermediatePickExpression(
                                    (Expression) h.popFromParseStack()));
                        }
                    },
                    new MSequence(
                            new MAction(new MLiteral("{")) {
                                @Override
                                public void onMatched(ParseHead h) {
                                    h.pushOnParseStack(new LinkedList());
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
                                            (List) h.popFromParseStack();
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
                                                        + "explicit weights.",
                                                        h);
                                            }
                                            else if (!explicitWeight
                                                    && elObj.getWeight()
                                                        != null) {
                                                throw new WellFormednessException(
                                                        "If first element of "
                                                        + "pool has no explicit"
                                                        + " weight, no elements"
                                                        + " may have explicit "
                                                        + "weight.",
                                                        h);
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
                                                        weighter), h);
                                    }
                                    
                                    h.pushOnParseStack(ipe);
                                }
                            })));
    
    private final Matcher WHERE_CLAUSE =
            new MOptional(new MSequence(new MLiteral("where"),
                    BOOLEAN_LEVEL_EXPRESSION)) {
                @Override
                public void onOmitted(ParseHead h) {
                    h.pushOnParseStack(VBoolean.TRUE);
                }
            };
    
    /**
     * <p>Results in {@code [where_expression, IntermediatePickExpression,
     * exemplar_string, ...]}.</p>
     */
    private final Matcher REQUIRED_SINGLE_PICK_TAIL_EXP =
            new MSequence(FROM_POOL, WHERE_CLAUSE);
    
    private final Matcher PICK_COUNT_SPECIFIER = new MSequence(
            new MOptional(NUMERIC_EXP) {
                @Override
                public void onOmitted(ParseHead h) {
                    h.pushOnParseStack(VNumber.of(1, 1));
                }
            },
            new MOptional(new MSequence(
                    new MLiteral("unique"),
                    new MOptional(PARENTHETICAL_EXP) {
                        @Override
                        public void onOmitted(ParseHead h) {
                            h.pushOnParseStack(VBoolean.TRUE);
                        }
                    })) {
                @Override
                public void onOmitted(ParseHead h) {
                    h.pushOnParseStack(VBoolean.FALSE);
                }
            });
    
    private final Matcher REQUIRED_PICK_EXP = new MAction(new MSequence(
                    new MLiteral("pick"), PICK_COUNT_SPECIFIER,
                    new MLiteral("from"), REQUIRED_SINGLE_PICK_TAIL_EXP)) {
                @Override
                public void onMatched(ParseHead h) {
                    Expression where = (Expression) h.popFromParseStack();
                    IntermediatePickExpression ipe =
                            (IntermediatePickExpression) h.popFromParseStack();
                    String exemplar = (String) h.popFromParseStack();
                    Expression unique = (Expression) h.popFromParseStack();
                    Expression count = (Expression) h.popFromParseStack();
                    
                    h.pushOnParseStack(new PickExpression(exemplar,
                            ipe.getPool(), count, unique, ipe.getWeighter(),
                            where, myDecider));
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
                public void run(ParseHead h) {
                    h.pushOnParseStack(new LinkedList());
                }
            },
            new MRepeated(new MAction(STATEMENT) {
                @Override
                public void onMatched(ParseHead h) {
                    Statement stmt = (Statement) h.popFromParseStack();
                    ((List) h.peekFromParseStack()).add(stmt);
                }
            }),
            new MDo() {
                @Override
                public void run(ParseHead h) {
                    List<Statement> stmts =
                            (List<Statement>) h.popFromParseStack();
                    h.pushOnParseStack(new MultiplexingStatement(stmts));
                }
            });
    
    {
        CODE_BLOCK.fillIn(new MLiteral("{"), CODE, new MLiteral("}"));
    }
    
    private final Matcher ASSIGN_OR_CALL_STMT = new MSequence(
            LHS,
            new MRequireAhead(
                    new MAlternatives(new MLiteral(";"), new MLiteral("=")),
                    "Expected ';' or '='."),
            new MAlternatives(
                    new MAction(new MLiteral(";")) {
                        @Override
                        public void onMatched(ParseHead h)
                                throws WellFormednessException {
                            Expression eval =
                                    ((LHSAccess) h.popFromParseStack())
                                            .toValue();
                            
                            if (!(eval instanceof FunctionCallExpression)
                                    && !(eval instanceof HostExpression)) {
                                throw new WellFormednessException(
                                        "Expected statement.", h);
                            }
                            
                            h.pushOnParseStack(new EvaluateStatement(eval));
                        }
                    },
                    new MAction(new MSequence(new MLiteral("="), EXP,
                            new MRequire(new MLiteral(";"), "Expected ';'."))) {
                        @Override
                        public void onMatched(ParseHead h)
                                throws WellFormednessException {
                            Expression value =
                                    (Expression) h.popFromParseStack();
                            
                            h.pushOnParseStack(
                                    ((LHSAccess) h.popFromParseStack())
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
                    Statement codeBlock = (Statement) h.popFromParseStack();
                    Expression where = (Expression) h.popFromParseStack();
                    IntermediatePickExpression ipe =
                            (IntermediatePickExpression) h.popFromParseStack();
                    String exemplar = (String) h.popFromParseStack();
                    
                    if (ipe.getWeighter() != null) {
                        throw new WellFormednessException("Weights not allowed "
                                + "in for-each statement.", h);
                    }
                    
                    h.pushOnParseStack(new ForEachStatement(
                            exemplar, ipe.getPool(), where, codeBlock));
                }
            };
    
    private final Matcher IF_STMT = new MSequence(
            new MLiteral("if"),
            EXP, CODE_BLOCK,
            new MDo() {
                @Override
                public void run(ParseHead h) {
                    Statement codeBlock = (Statement) h.popFromParseStack();
                    Expression exp = (Expression) h.popFromParseStack();
                    
                    h.pushOnParseStack(new LinkedList());
                    
                    ((Deque) h.peekFromParseStack()).push(exp);
                    ((Deque) h.peekFromParseStack()).push(codeBlock);
                }
            },
            new MRepeated(new MLiteral("else"), new MLiteral("if"), EXP,
                    CODE_BLOCK,
                    new MDo() {
                        @Override
                        public void run(ParseHead h) {
                            Statement codeBlock =
                                    (Statement) h.popFromParseStack();
                            Expression exp = (Expression) h.popFromParseStack();
                            ((Deque) h.peekFromParseStack()).push(exp);
                            ((Deque) h.peekFromParseStack()).push(codeBlock);
                        }
                    }),
            new MOptional(new MLiteral("else"), CODE_BLOCK,
                    new MDo() {
                        @Override
                        public void run(ParseHead h) {
                            Statement codeBlock =
                                    (Statement) h.popFromParseStack();
                            ((Deque) h.peekFromParseStack())
                                    .push(VBoolean.TRUE);
                            ((Deque) h.peekFromParseStack()).push(codeBlock);
                        }
                    }),
            new MDo() {
                @Override
                public void run(ParseHead h) {
                    Deque ifComponents = (Deque) h.popFromParseStack();
                    
                    Statement curIf = Statement.NO_OP;
                    do {
                        Statement onTrue = (Statement) ifComponents.pop();
                        Expression condition = (Expression) ifComponents.pop();
                        
                        curIf = new IfStatement(condition, onTrue, curIf);
                    } while (!ifComponents.isEmpty());
                    
                    h.pushOnParseStack(curIf);
                }
            });
    
    private final Matcher RETURN_STMT =
            new MAction(new MSequence(
                    new MLiteral("return"), EXP, new MLiteral(";"))) {
                @Override
                public void onMatched(ParseHead h) {
                    h.pushOnParseStack(new ReturnStatement(
                            (Expression) h.popFromParseStack()));
                }
            };
    
    private final Matcher INTRO_STMT =
            new MSequence(
                    new MLiteral("intro"),
                    new MRequire(IDENTIFIER, "Expected identifier."),
                    new MRequireAhead(
                            new MAlternatives(
                                    new MLiteral("="),
                                    new MLiteral(";")
                            ),
                            "Expected '=' or ';'."),
                    new MOptional(new MSequence(
                            new MLiteral("="), EXP)) {
                        @Override
                        public void onOmitted(ParseHead h) {
                            h.pushOnParseStack(VUnavailable.INSTANCE);
                        }
                    },
                    new MLiteral(";"),
                    new MDo() {
                        @Override
                        public void run(ParseHead h) {
                            Expression initialValue =
                                    (Expression) h.popFromParseStack();
                            String name = (String) h.popFromParseStack();
                            
                            h.pushOnParseStack(
                                    new VariableIntroductionStatement(
                                            name, initialValue));
                        }
                    });
    
    {
        STATEMENT.fillIn(new MAlternatives(INTRO_STMT, RETURN_STMT,
                FOR_EACH_STMT, IF_STMT, ASSIGN_OR_CALL_STMT));
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
    
    private final HostEnvironment myHostEnvironment;
    
    private final Decider myDecider;
    
    public Sbsdl(HostEnvironment e) {
        this(e, new Decider() {
                    @Override
                    public boolean randomize(double chance) {
                        return Math.random() < chance;
                    }
                });
    }
    
    public Sbsdl(HostEnvironment e, Decider d) {
        myHostEnvironment = e;
        myDecider = d;
    }
    
    public void run(String input) throws WellFormednessException {
        ParseHead h;
        
        if (DEBUG) {
            h = new DebugParseHead(input);
        }
        else {
            h = new ParseHead(input);
        }
        h.setSkip(DEFAULT_SKIPPER);
        
        try {
            h.require(CODE);
        }
        catch (NoMatchException nme) {
            throw new WellFormednessException("Couldn't parse input.", h);
        }
        
        if (h.hasNextChar()) {
            throw new WellFormednessException("Don't understand: "
                    + h.remainingText().split("\n")[0], h);
        }
        
        ScriptEnvironment s = new ScriptEnvironment();
        ((MultiplexingStatement) h.popFromParseStack())
                .execute(myHostEnvironment, s);
        
        if (!h.getParseStackCopy().isEmpty()) {
            throw new RuntimeException();
        }
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
            h.pushOnParseStack(myObject);
            
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
            Expression right = (Expression) h.popFromParseStack();
            BinaryExpression.Operator op =
                    (BinaryExpression.Operator) h.popFromParseStack();
            Expression left = (Expression) h.popFromParseStack();

            h.pushOnParseStack(new BinaryExpression(left, op, right));
            
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
        
        public void fillInWeighter(Expression w, ParseHead h)
                throws WellFormednessException {
            if (myWeighter != null) {
                throw new WellFormednessException("Pick pools with explicit "
                        + "weightings cannot also have a 'weighted by' "
                        + "clause.", h);
            }
            
            myWeighter = w;
        }
    }
    
    private class MNoAssign extends MAction {
        private final String myDescription;
        
        public MNoAssign(Matcher m, String description) {
            super(m);
            myDescription = description;
        }
        
        @Override
        public void onMatched(final ParseHead h) {
            final Expression e = (Expression) h.popFromParseStack();
            h.pushOnParseStack(new LHSAccess() {
                        @Override
                        public Expression toValue() {
                            return e;
                        }

                        @Override
                        public Statement toAssignment(Expression value)
                                throws WellFormednessException {
                            throw new WellFormednessException("Cannot "
                                    + "assign to " + myDescription + ".", h);
                        }
                    });
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
    
    public static interface Decider {
        public boolean randomize(double chance);
    }
    
    public class DebugParseHead extends ParseHead {
        public DebugParseHead(String input) {
            super(input);
        }

        @Override
        public void pushOnParseStack(Object o) {
            super.pushOnParseStack(o);
            
            System.out.print("\nPush ");
            debugPrint();
        }

        @Override
        public Object popFromParseStack() {
            Object result = super.popFromParseStack();
            
            if (DEBUG) {
                System.out.print("\nPop ");
                debugPrint();
            }
            
            return result;
        }
        
        private void debugPrint() {
            Deque parseStack = getParseStackCopy();
            
            System.out.println(new RuntimeException().getStackTrace()[2]);
            
            System.out.print("[");
            
            Deque head = new LinkedList();
            while (head.size() < 4 && !parseStack.isEmpty()) {
                if (!head.isEmpty()) {
                    System.out.print(", ");
                }
                
                Object o = parseStack.pop();
                System.out.print(o);
                
                head.push(o);
            }
            
            if (!parseStack.isEmpty()) {
                System.out.print(", ...");
            }
            
            System.out.println("]");
            
            while (!head.isEmpty()) {
                parseStack.push(head.pop());
            }
        }
    }
}
