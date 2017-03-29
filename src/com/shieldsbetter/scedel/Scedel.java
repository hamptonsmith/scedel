package com.shieldsbetter.scedel;

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
import java.io.PrintWriter;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.shieldsbetter.scedel.expressions.BinaryExpression;
import com.shieldsbetter.scedel.expressions.ClosureBuildingExpression;
import com.shieldsbetter.scedel.expressions.DictionaryExpression;
import com.shieldsbetter.scedel.expressions.Expression;
import com.shieldsbetter.scedel.expressions.FunctionCallExpression;
import com.shieldsbetter.scedel.expressions.HostExpression;
import com.shieldsbetter.scedel.expressions.LiteralExpression;
import com.shieldsbetter.scedel.expressions.PickExpression;
import com.shieldsbetter.scedel.expressions.SequenceExpression;
import com.shieldsbetter.scedel.expressions.UnaryExpression;
import com.shieldsbetter.scedel.expressions.VariableNameExpression;
import com.shieldsbetter.scedel.statements.DecideStatement;
import com.shieldsbetter.scedel.statements.EvaluateStatement;
import com.shieldsbetter.scedel.statements.FieldAssignmentStatement;
import com.shieldsbetter.scedel.statements.ForEachStatement;
import com.shieldsbetter.scedel.statements.IfStatement;
import com.shieldsbetter.scedel.statements.MultiplexingStatement;
import com.shieldsbetter.scedel.statements.ReturnStatement;
import com.shieldsbetter.scedel.statements.SequenceAssignmentStatement;
import com.shieldsbetter.scedel.statements.Statement;
import com.shieldsbetter.scedel.statements.TopLevelVariableAssignmentStatement;
import com.shieldsbetter.scedel.statements.VariableIntroductionStatement;
import com.shieldsbetter.scedel.values.VBoolean;
import com.shieldsbetter.scedel.values.VNone;
import com.shieldsbetter.scedel.values.VNumber;
import com.shieldsbetter.scedel.values.VString;
import com.shieldsbetter.scedel.values.VUnavailable;
import com.shieldsbetter.scedel.values.Value;

public class Scedel {    
    public static final boolean DEBUG = false;
    
    public static Decider buildRandomDecider() {
        return new Decider() {
                    @Override
                    public boolean decide(double chance) {
                        return Math.random() < chance;
                    }
                };
    }
    
    private final String[] KEYWORDS =
            {"from", "if", "for", "each", "pick", "unique", "true", "false",
             "unavailable", "none", "intro", "bake", "not", "and", "or",
             "otherwise", "decide"};
    
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
    private final MPlaceholder REQUIRED_NO_SCOPE_BLOCK = new MPlaceholder();
    private final MPlaceholder REQUIRED_INNER_LEXICAL_SCOPE_BLOCK =
            new MPlaceholder();
    
    private final Matcher STRING_INNARDS =
            new MCapture(new MRepeated(new MSequence(
                    new MForbid(new MLiteral("\n"), "Encountered end "
                            + "of line before close of string "
                            + "literal."),
                    new MAlternatives(
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
                                    + "of input before string closed."))),
                    0, 300));
    
    private final Matcher STRING_LITERAL =
            new MAction(true,
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
                    
                    h.pushOnParseStack(
                            new LiteralExpression(loc(getNotedPosition()),
                                    new VString(innards)));
                }
            };
    
    private final Matcher FRACTIONAL_LITERAL =
            new MAction(true,
                    new MSequence(
                            new MCapture(
                                    new MOptional(new MLiteral("-")),
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
                    
                    h.pushOnParseStack(
                            new LiteralExpression(loc(getNotedPosition()), n));
                }
            };
    
    private final Matcher INTEGRAL_LITERAL =
            new MAction(true,
                    new MCapture(new MOptional(new MLiteral("-")),
                            new MRepeated(CSet.ISO_LATIN_DIGIT, 1, 10))) {
                @Override
                public void onMatched(ParseHead h) {
                    String integerText = h.popCapture();
                    VNumber n = VNumber.of(Long.parseLong(integerText), 1);
                    
                    h.pushOnParseStack(
                            new LiteralExpression(loc(getNotedPosition()), n));
                }
            };
    
    // Fractional has to come first so that integral doesn't gobble up its
    // integer part.
    private final Matcher NUMERIC_LITERAL =
            new MWithSkipper(
                    new MAlternatives(FRACTIONAL_LITERAL, INTEGRAL_LITERAL),
                    null);
    
    private final Matcher SEQUENCE_LITERAL =
            new MAction(true, new MSequence(new MLiteral("["),
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
                    new MLiteral("]"))) {
                        @Override
                        public void onMatched(ParseHead h) {
                            h.pushOnParseStack(new SequenceExpression(
                                    loc(getNotedPosition()),
                                    (List<Expression>) h.popFromParseStack()));
                        }
                    };
    
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
            new MAction(EXP) {
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
            new MAction(true, new MSequence(
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

                    h.pushOnParseStack(new HostExpression(
                            loc(getNotedPosition()), hostId, parameterList));
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
    
    private final Matcher DICTIONARY_LITERAL = new MAction(true, new MSequence(
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
            new MLiteral("}"))) {
                @Override
                public void onMatched(ParseHead h) {
                    Map<Expression, Expression> fields =
                            (Map<Expression, Expression>) h.popFromParseStack();
                    
                    DictionaryExpression dict = new DictionaryExpression(
                            loc(getNotedPosition()), fields);
                    h.pushOnParseStack(dict);
                }
            };
    
    private final Matcher SINGLE_ARGUMENT_DECLARATION =
            new MAction(true, IDENTIFIER) {
                @Override
                public void onMatched(ParseHead h) {
                    String id = (String) h.popFromParseStack();
                    
                    Symbol argSym = 
                            introduceSymbol(id, false, getNotedPosition());
                    
                    ((List) h.peekFromParseStack()).add(argSym);
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
            new MAction(true, new MSequence(new MLiteral("fn"),
                    new MDo() {
                        @Override
                        public void run(ParseHead h) {
                            myLexicalScope =
                                    new ClosingLexicalScope(myLexicalScope);
                        }
                    },
                    new MRequire(new MSequence(
                        new MLiteral("("), ARGUMENT_DECLARATION_INNARDS,
                            new MLiteral(")")), "Expected argument list."),
                    CODE_BLOCK,
                    new MDo() {
                        @Override
                        public void run(ParseHead h) {
                            myLexicalScope = myLexicalScope.getParent();
                        }
                    })) {
                @Override
                public void onMatched(ParseHead h) {
                    MultiplexingStatement code =
                            (MultiplexingStatement) h.popFromParseStack();
                    h.pushOnParseStack(
                            new ClosureBuildingExpression(
                                    loc(getNotedPosition()),
                                    (List<Symbol>) h.popFromParseStack(),
                                    code));
                }
            };
    
    private final Matcher BOOLEAN_LITERAL =
            new MAlternatives(
                    new MAction(true, new MLiteral("true")) {
                        @Override
                        public void onMatched(ParseHead h) {
                            h.pushOnParseStack(new LiteralExpression(
                                    loc(getNotedPosition()),
                                    VBoolean.TRUE));
                        }
                    },
                    new MAction(true, new MLiteral("false")) {
                        @Override
                        public void onMatched(ParseHead h) {
                            h.pushOnParseStack(new LiteralExpression(
                                    loc(getNotedPosition()),
                                    VBoolean.FALSE));
                        }
                    });
    
    private final Matcher BOUNDED_EXP = new MAlternatives(
            new MNoAssign(
                    new MAction(true, new MLiteral("unavailable")) {
                        @Override
                        public void onMatched(ParseHead h) {
                            h.pushOnParseStack(new LiteralExpression(
                                    loc(getNotedPosition()),
                                    VUnavailable.INSTANCE));
                        }
                    },
                    "an unavailable literal"),
            new MNoAssign(new MAction(true, new MLiteral("none")) {
                        @Override
                        public void onMatched(ParseHead h) {
                            h.pushOnParseStack(new LiteralExpression(
                                    loc(getNotedPosition()), VNone.INSTANCE));
                        }
                    },
                    "a none literal"),
            new MNoAssign(BOOLEAN_LITERAL, "a boolean literal"),
            new MNoAssign(DICTIONARY_LITERAL, "a dictionary literal"),
            new MNoAssign(SEQUENCE_LITERAL, "a sequence literal"),
            new MNoAssign(FUNCTION_LITERAL, "a function literal"),
            new MNoAssign(STRING_LITERAL, "a string literal"),
            new MNoAssign(NUMERIC_LITERAL, "a numeric literal"),
            new MNoAssign(PARENTHETICAL_EXP, "a parenthetical expression"),
            new MNoAssign(HOST_EXP, "a host expression"),
            new MAction(true, IDENTIFIER) {
                @Override
                public void onMatched(final ParseHead h) {
                    final String idName = (String) h.popFromParseStack();
                    final ParseHead.Position pos = getNotedPosition();
                    h.pushOnParseStack(new LHSAccess() {
                                @Override
                                public Expression toValue() {
                                    Symbol idSym = nameToSymbol(idName, pos);
                                    return new VariableNameExpression(
                                            loc(pos), idSym);
                                }

                                @Override
                                public Statement toAssignment(
                                        Expression value) {
                                    Symbol idSym = nameToSymbol(idName, pos);
                                    
                                    if (idSym.isBaked()) {
                                        throw InternalStaticCodeException
                                                .bakedModifyForbidden(loc(h));
                                    }
                                    
                                    return new TopLevelVariableAssignmentStatement(
                                            loc(pos), idSym, value);
                                }
                            });
                }
            });
    
    private final Matcher LHS =
            new MSequence(BOUNDED_EXP, new MRepeated(new MAlternatives(
                    new MAction(true, new MSequence(new MLiteral("."), 
                            new MAlternatives(NAKED_STRING_IDENTIFIER,
                                    PARENTHETICAL_EXP))) {
                        @Override
                        public void onMatched(final ParseHead h) {
                            final Expression key =
                                    (Expression) h.popFromParseStack();
                            final Expression dict =
                                    ((LHSAccess) h.popFromParseStack())
                                            .toValue();
                            final ParseHead.Position pos = getNotedPosition();
                            
                            h.pushOnParseStack(
                                    new LHSAccess() {
                                        @Override
                                        public Expression toValue() {
                                            return new BinaryExpression(
                                                    loc(pos), dict,
                                                    BinaryExpression.Operator
                                                            .LOOK_UP_KEY,
                                                    key);
                                        }

                                        @Override
                                        public Statement toAssignment(
                                                Expression value) {
                                            if (dict.yeildsBakedLValues()) {
                                                throw InternalStaticCodeException
                                                        .bakedModifyForbidden(
                                                                loc(h));
                                            }
                                            
                                            return new FieldAssignmentStatement(
                                                    loc(pos), dict, key, value);
                                        }
                                    });
                        }
                    },
                    new MAction(true, new MSequence(
                            new MLiteral("["), EXP, new MLiteral("]"))) {
                        @Override
                        public void onMatched(final ParseHead h) {
                            final Expression index =
                                    (Expression) h.popFromParseStack();
                            final Expression seq =
                                    ((LHSAccess) h.popFromParseStack())
                                            .toValue();
                            final ParseHead.Position pos = getNotedPosition();
                            
                            h.pushOnParseStack(
                                    new LHSAccess() {
                                        @Override
                                        public Expression toValue() {
                                            return new BinaryExpression(
                                                    loc(pos), seq,
                                                    BinaryExpression.Operator
                                                            .INDEX_SEQ,
                                                    index);
                                        }

                                        @Override
                                        public Statement toAssignment(
                                                Expression value) {
                                            if (seq.yeildsBakedLValues()) {
                                                throw InternalStaticCodeException
                                                        .bakedModifyForbidden(
                                                                loc(h));
                                            }
                                            
                                            return new SequenceAssignmentStatement(
                                                    loc(pos), seq, index,
                                                    value);
                                        }
                                    });
                        }
                    },
                    new MAction(true, new MSequence(
                            new MLiteral("("), PARAM_LIST_INNARDS,
                            new MLiteral(")"))) {
                        @Override
                        public void onMatched(final ParseHead h) {
                            final List<Expression> args =
                                    (List) h.popFromParseStack();
                            final Expression f =
                                    ((LHSAccess) h.popFromParseStack())
                                            .toValue();
                            final ParseHead.Position startP =
                                    getNotedPosition();
                            final ParseHead.Position endP = h.getPosition();
                            
                            h.pushOnParseStack(new LHSAccess() {
                                        @Override
                                        public Expression toValue() {
                                            return new FunctionCallExpression(
                                                    loc(startP), f, args);
                                        }

                                        @Override
                                        public Statement toAssignment(
                                                Expression value) {
                                            throw InternalStaticCodeException
                                                    .expectedEndOfStatement(
                                                            loc(endP));
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
                    new MRepeated(new MAction(true, new MLiteral("not")) {
                                @Override
                                public void onMatched(ParseHead h) throws WellFormednessException {
                                    h.pushOnParseStack(getNotedPosition());
                                    h.pushOnParseStack(UnaryExpression.Operator
                                            .BOOLEAN_NEGATE);
                                }
                            }),
                    LHS_EXP,
                    new MDo() {
                        @Override
                        public void run(ParseHead h) {
                            Expression baseExp =
                                    (Expression) h.popFromParseStack();
                            while (!h.isParseStackEmpty()
                                    && (h.peekFromParseStack() instanceof
                                        UnaryExpression.Operator)) {
                                UnaryExpression.Operator op =
                                        (UnaryExpression.Operator)
                                                h.popFromParseStack();
                                baseExp = new UnaryExpression(
                                        loc((ParseHead.Position)
                                                h.popFromParseStack()),
                                        op, baseExp);
                            }
                            
                            h.pushOnParseStack(baseExp);
                        }
                    });
    
    private final Matcher POW_PRECEDENCE_EXP =
            new MSequence(PREFIX_EXP, new MRepeated(
                    new MAction(true,
                            new MSequence(new MLiteral("^"), PREFIX_EXP)) {
                        @Override
                        public void onMatched(ParseHead h) {
                            Expression right =
                                    (Expression) h.popFromParseStack();
                            Expression left =
                                    (Expression) h.popFromParseStack();
                            
                            h.pushOnParseStack(new BinaryExpression(
                                    loc(getNotedPosition()), left,
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
            new MOptional(
                    new MAction(true,
                            new MSequence(IDENTIFIER, new MLiteral(":"))) {
                        @Override
                        public void onMatched(ParseHead h) {
                            Symbol exSym = introduceSymbol(
                                    (String) h.popFromParseStack(), false,
                                    getNotedPosition());
                            
                            h.pushOnParseStack(exSym);
                        }
                    }) {
                @Override
                public void onOmitted(ParseHead h) {
                    Symbol exSym = introduceSymbol("@", false, h.getPosition());
                    
                    h.pushOnParseStack(exSym);
                }
            },
            new MAlternatives(
                    new MAction(BOOLEAN_LEVEL_EXPRESSION) {
                        @Override
                        public void onMatched(ParseHead h) {
                            Expression sourceCollection =
                                    (Expression) h.popFromParseStack();
                            Symbol exemplar = (Symbol) h.popFromParseStack();
                            
                            h.pushOnParseStack(new IntermediatePickExpression(
                                    exemplar, sourceCollection));
                        }
                    },
                    new MSequence(
                            new MAction(true, new MLiteral("{")) {
                                @Override
                                public void onMatched(ParseHead h) {
                                    h.pushOnParseStack(getNotedPosition());
                                    h.pushOnParseStack(new LinkedList());
                                }
                            },
                            new MOptional(POOL_ELEMENT), 
                                    new MRepeated(
                                            new MLiteral(","), POOL_ELEMENT),
                            new MAction(new MLiteral("}")) {
                                @Override
                                public void onMatched(ParseHead h) {
                                    List<Expression> weighter =
                                            new LinkedList<>();
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
                                                throw InternalStaticCodeException
                                                        .pickCollectionAllNeedWeights(
                                                                loc(h));
                                            }
                                            else if (!explicitWeight
                                                    && elObj.getWeight()
                                                        != null) {
                                                throw InternalStaticCodeException
                                                        .pickCollectionNoWeights(
                                                                loc(h));
                                            }
                                        }
                                        
                                        if (elObj.getWeight() != null) {
                                            weighter.add(elObj.getWeight());
                                        }
                                    }
                                    
                                    ParseLocation loc = loc((ParseHead.Position)
                                            h.popFromParseStack());
                                    
                                    Symbol exemplar =
                                            (Symbol) h.popFromParseStack();
                                    
                                    IntermediatePickExpression ipe =
                                            new IntermediatePickExpression(
                                                    exemplar,
                                                    new SequenceExpression(
                                                            loc, seq));
                                    if (explicitWeight) {
                                        ipe.fillInWeighter(
                                                new SequenceExpression(
                                                        loc, weighter),
                                                h);
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
    
    private final Matcher REQUIRED_PICK_EXP = new MAction(true, new MSequence(
                    new MLiteral("pick"), PICK_COUNT_SPECIFIER,
                    new MLiteral("from"),
                    new MDo() {
                        @Override
                        public void run(ParseHead h) {
                            myLexicalScope =
                                    new InnerLexicalScope(myLexicalScope);
                        }
                    },
                    REQUIRED_SINGLE_PICK_TAIL_EXP,
                    new MDo() {
                        @Override
                        public void run(ParseHead h) {
                            myLexicalScope = myLexicalScope.getParent();
                        }
                    })) {
                @Override
                public void onMatched(ParseHead h) {
                    Expression where = (Expression) h.popFromParseStack();
                    IntermediatePickExpression ipe =
                            (IntermediatePickExpression) h.popFromParseStack();
                    Symbol exemplar = ipe.getExemplar();
                    Expression unique = (Expression) h.popFromParseStack();
                    Expression count = (Expression) h.popFromParseStack();
                    
                    h.pushOnParseStack(new PickExpression(
                            loc(getNotedPosition()), exemplar, ipe.getPool(),
                            count, unique, ipe.getWeighter(), where));
                }
            };
    
    // Explicit pick has to come first so we don't gobble up "pick" as an
    // identifier.
    private final Matcher PICK_EXP =
            new MAlternatives(REQUIRED_PICK_EXP, BOOLEAN_LEVEL_EXPRESSION);
    
    private final Matcher OTHERWISE_EXP =
            new MSequence(PICK_EXP, new MRepeated(
                    new MBinaryExpression(
                            new MPush(new MLiteral("otherwise"),
                                    BinaryExpression.Operator.OTHERWISE),
                            PICK_EXP)));
    
    {
        EXP.fillIn(OTHERWISE_EXP);
    }
    
    private final MPlaceholder STATEMENT = new MPlaceholder();
    
    private final Matcher CODE = new MSequence(
            new MDo() {
                @Override
                public void run(ParseHead h) {
                    h.pushOnParseStack(loc(h));
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
                    h.pushOnParseStack(new MultiplexingStatement(
                            (ParseLocation) h.popFromParseStack(), stmts));
                }
            });
    
    {
        CODE_BLOCK.fillIn(new MLiteral("{"), CODE, new MLiteral("}"));
        REQUIRED_INNER_LEXICAL_SCOPE_BLOCK.fillIn(new MSequence(
                new MRequire(new MLiteral("{"),
                        "Expected opening of code block with '{'."),
                new MDo() {
                    @Override
                    public void run(ParseHead h) {
                        myLexicalScope = new InnerLexicalScope(myLexicalScope);
                    }
                },
                CODE,
                new MForbid(MEndOfInput.INSTANCE, "Reached end of input before "
                        + "close of code block."),
                new MDo() {
                    @Override
                    public void run(ParseHead h) {
                        myLexicalScope = myLexicalScope.getParent();
                    }
                },
                new MLiteral("}")));
        REQUIRED_NO_SCOPE_BLOCK.fillIn(new MSequence(
                new MRequire(new MLiteral("{"),
                        "Expected opening of code block with '{'."),
                CODE,
                new MForbid(MEndOfInput.INSTANCE, "Reached end of input before "
                        + "close of code block."),
                new MLiteral("}")));
    }
    
    private final Matcher ASSIGN_OR_CALL_STMT = new MSequence(
            LHS,
            new MRequireAhead(
                    new MAlternatives(new MLiteral(";"), new MLiteral("=")),
                    "Expected ';' or '='."),
            new MAlternatives(
                    new MAction(true, new MLiteral(";")) {
                        @Override
                        public void onMatched(ParseHead h) {
                            Expression eval =
                                    ((LHSAccess) h.popFromParseStack())
                                            .toValue();
                            
                            if (!(eval instanceof FunctionCallExpression)
                                    && !(eval instanceof HostExpression)) {
                                throw InternalStaticCodeException
                                        .expectedEndOfStatement(
                                                loc(getNotedPosition()));
                            }
                            
                            h.pushOnParseStack(new EvaluateStatement(
                                    eval.getParseLocation(), eval));
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
            new MAction(true, new MSequence(new MLiteral("for"),
                    new MLiteral("each"),
                    new MDo() {
                        @Override
                        public void run(ParseHead h) {
                            myLexicalScope =
                                    new InnerLexicalScope(myLexicalScope);
                        }
                    },
                    REQUIRED_SINGLE_PICK_TAIL_EXP, REQUIRED_NO_SCOPE_BLOCK,
                    new MDo() {
                        @Override
                        public void run(ParseHead h) {
                            myLexicalScope = myLexicalScope.getParent();
                        }
                    })) {
                @Override
                public void onMatched(ParseHead h) {
                    Statement codeBlock = (Statement) h.popFromParseStack();
                    Expression where = (Expression) h.popFromParseStack();
                    IntermediatePickExpression ipe =
                            (IntermediatePickExpression) h.popFromParseStack();
                    Symbol exemplar = ipe.getExemplar();
                    
                    if (ipe.getWeighter() != null) {
                        throw InternalStaticCodeException
                                .weightsDisallowedInForEach(loc(h));
                    }
                    
                    h.pushOnParseStack(new ForEachStatement(
                            loc(getNotedPosition()), exemplar, ipe.getPool(),
                            where, codeBlock));
                }
            };
    
    private final Matcher IF_STMT = new MSequence(
            new MAction(true,
                    new MLiteral("if")) {
                        @Override
                        public void onMatched(ParseHead h) {
                            h.pushOnParseStack(loc(getNotedPosition()));
                        }
                    },
            new MRequire(EXP, "Expected condition expression."),
            REQUIRED_INNER_LEXICAL_SCOPE_BLOCK,
            new MDo() {
                @Override
                public void run(ParseHead h) {
                    Statement codeBlock = (Statement) h.popFromParseStack();
                    Expression exp = (Expression) h.popFromParseStack();
                    ParseLocation loc = (ParseLocation) h.popFromParseStack();
                    
                    h.pushOnParseStack(new LinkedList());
                    
                    ((Deque) h.peekFromParseStack()).push(loc);
                    ((Deque) h.peekFromParseStack()).push(exp);
                    ((Deque) h.peekFromParseStack()).push(codeBlock);
                }
            },
            new MRepeated(
                    new MAction(true, new MSequence(new MLiteral("else"),
                            new MLiteral("if"))) {
                        @Override
                        public void onMatched(ParseHead h) {
                            ((Deque) h.peekFromParseStack()).push(
                                    loc(getNotedPosition()));
                        }
                    }, EXP,
                    REQUIRED_INNER_LEXICAL_SCOPE_BLOCK,
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
            new MOptional(
                    new MAction(true, new MLiteral("else")) {
                        @Override
                        public void onMatched(ParseHead h) {
                            ((Deque) h.peekFromParseStack()).push(
                                    loc(getNotedPosition()));
                        }
                    },
                    REQUIRED_INNER_LEXICAL_SCOPE_BLOCK,
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
                        ParseLocation loc = (ParseLocation) ifComponents.pop();
                        
                        curIf = new IfStatement(loc, condition, onTrue, curIf);
                    } while (!ifComponents.isEmpty());
                    
                    h.pushOnParseStack(curIf);
                }
            });
    
    private final Matcher RETURN_STMT =
            new MAction(true, new MSequence(
                    new MLiteral("return"), EXP, new MLiteral(";"))) {
                @Override
                public void onMatched(ParseHead h) {
                    h.pushOnParseStack(new ReturnStatement(
                            loc(getNotedPosition()),
                            (Expression) h.popFromParseStack()));
                }
            };
    
    private final Matcher INTRO_IDENTIFIER =
            new MAction(true,
                    new MRequire(IDENTIFIER, "Expected new variable name.")) {
                @Override
                public void onMatched(ParseHead h) {
                    Symbol idSym = introduceSymbol(
                            (String) h.popFromParseStack(),
                            (boolean) h.popFromParseStack(),
                            getNotedPosition());
                    
                    h.pushOnParseStack(idSym);
                }
            };
    
    private final Matcher INTRO_STMT =
            new MSequence(
                    new MAction(true, new MAlternatives(
                            new MPush(new MLiteral("intro"), false),
                            new MPush(new MLiteral("bake"), true))) {
                                @Override
                                public void onMatched(ParseHead h) {
                                    boolean bake =
                                            (Boolean) h.popFromParseStack();
                                    h.pushOnParseStack(loc(getNotedPosition()));
                                    h.pushOnParseStack(bake);
                                }
                            },
                    INTRO_IDENTIFIER,
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
                    new MRequire(new MLiteral(";"), "Expected ';'."),
                    new MDo() {
                        @Override
                        public void run(ParseHead h) {
                            Expression initialValue =
                                    (Expression) h.popFromParseStack();
                            Symbol sym = (Symbol) h.popFromParseStack();
                            
                            h.pushOnParseStack(
                                    new VariableIntroductionStatement(
                                            (ParseLocation)
                                                    h.popFromParseStack(),
                                            sym, initialValue));
                        }
                    });
    
    private final Matcher DECIDE_STMT =
            new MAction(true, new MSequence(new MLiteral("decide"), EXP,
                    REQUIRED_INNER_LEXICAL_SCOPE_BLOCK)) {
                        @Override
                        public void onMatched(ParseHead h) {
                            MultiplexingStatement code = (MultiplexingStatement)
                                    h.popFromParseStack();
                            Expression decider =
                                    (Expression) h.popFromParseStack();
                            
                            h.pushOnParseStack(new DecideStatement(
                                    loc(getNotedPosition()), decider, code));
                        }
                    };
    
    {
        STATEMENT.fillIn(new MSequence(
                new MForbid(new MLiteral("pick"), "Pick expression cannot "
                        + "begin a statement.  Try surrounding it in "
                        + "parentheses."),
                new MAlternatives(INTRO_STMT, RETURN_STMT, FOR_EACH_STMT,
                        IF_STMT, DECIDE_STMT, ASSIGN_OR_CALL_STMT)));
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
    private RootLexicalScope myLexicalScope =
            new InnerLexicalScope(new RootLexicalScope());
    
    private String mySourceDescription;
    
    public Scedel(HostEnvironment e) {
        this(e, buildRandomDecider());
    }
    
    public Scedel(HostEnvironment e, Decider d) {
        myHostEnvironment = e;
        myDecider = d;
    }
    
    public void printParseTree(String input) throws StaticCodeException {
        ParseHead h = parse(input);

        StringBuilder b = new StringBuilder();
        ((Statement) h.popFromParseStack()).prettyRender(4, 0, b);
        System.out.println(b);
    }
    
    public void run(String input)
            throws StaticCodeException, ExecutionException {
        StackTraceElement e = new RuntimeException().getStackTrace()[1];
        String sourceDesc = "string from " + e.getFileName() + ", line "
                + e.getLineNumber();
        
        run(sourceDesc, input);
    }
    
    public void run(String sourceDescription, String input)
            throws StaticCodeException, ExecutionException {
        mySourceDescription = sourceDescription;
        ParseHead h = parse(input);
        
        ScriptEnvironment s = new ScriptEnvironment(myDecider);
        
        try {
            ((MultiplexingStatement) h.popFromParseStack())
                    .execute(myHostEnvironment, s);
        }
        catch (InternalExecutionException iee) {
            throw iee.getExecutionException();
        }
        
        if (!h.getParseStackCopy().isEmpty()) {
            throw new RuntimeException();
        }
    }
    
    private ParseHead parse(String input) throws StaticCodeException { 
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
            
            if (h.hasNextChar()) {
                throw new StaticCodeException(
                        StaticCodeException.ErrorType.GENERIC_SYNTAX_ERROR,
                        "Don't understand: " + h.remainingText().split("\n")[0],
                        loc(h));
            }
        }
        catch (InternalStaticCodeException isce) {
            throw isce.getStaticCodeException().copy();
        }
        catch (WellFormednessException wfe) {
            throw new StaticCodeException(
                    StaticCodeException.ErrorType.GENERIC_SYNTAX_ERROR,
                    wfe.getMessage(), loc(wfe));
        }
        catch (NoMatchException nme) {
            throw new StaticCodeException(
                    StaticCodeException.ErrorType.GENERIC_SYNTAX_ERROR,
                    nme.getMessage(), loc(h));
        }
        
        return h;
    }
    
    private Symbol nameToSymbol(String name, ParseHead.Position pos) {
        Symbol idSym;
        try {
            idSym = myLexicalScope.getSymbol(name);
        }
        catch (NoSuchSymbolException nsse) {
            throw InternalStaticCodeException.noSuchSymbol(loc(pos));
        }
        catch (SymbolInaccessibleException sie) {
            throw InternalStaticCodeException.inaccessibleSymbol(
                    loc(pos), sie.getDefinitionLocation());
        }
        
        return idSym;
    }
    
    private Symbol introduceSymbol(
            String name, boolean baked, ParseHead.Position pos) {
        Symbol result;
        try {
            result = myLexicalScope.introduceSymbol(name, baked, pos);
        }
        catch (DuplicateSymbolException dse) {
            throw InternalStaticCodeException.duplicateSymbol(
                    loc(pos), dse.getPreviousDefinitionLocation());
        }
        
        return result;
    }
    
    private ParseLocation loc(WellFormednessException e) {
        return new ParseLocation(mySourceDescription, e.getLineNumber(),
                e.getColNumber(), e.getAlignmentPrefix(), e.getLineContents());
    }
    
    private ParseLocation loc(ParseHead h) {
        return loc(h.getPosition());
    }
    
    private ParseLocation loc(ParseHead.Position p) {
        return new ParseLocation(mySourceDescription, p.getLineNumber(),
                p.getColumn(), p.getAlignmentPrefix(), p.getLineContents());
    }
    
    private static String renderPosition(ParseHead.Position p) {
        String result = "Line " + p.getLineNumber() + ":\n";
        result += p.getLineContents() + "\n";
        result += p.getAlignmentPrefix() + "^";
        
        return result;
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
         * @throws com.shieldsbetter.scedel.Scedel.HostEnvironmentException If
         *               for any reason the given host expression cannot be
         *               evaluated into a {@link Value}.
         */
        public Value evaluate(String name, List<Value> parameters)
                throws HostEnvironmentException;
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
            super(true, base);
            
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

            h.pushOnParseStack(new BinaryExpression(
                    loc(getNotedPosition()), left, op, right));
            
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
    
    private class IntermediatePickExpression {
        private final Symbol myExemplar;
        private Expression myCollection;
        private Expression myWeighter;
        
        public IntermediatePickExpression(
                Symbol exemplar, Expression collection) {
            myExemplar = exemplar;
            myCollection = collection;
        }
        
        public Symbol getExemplar() {
            return myExemplar;
        }
        
        public Expression getPool() {
            return myCollection;
        }
        
        public Expression getWeighter() {
            return myWeighter;
        }
        
        public void fillInWeighter(Expression w, ParseHead h) {
            if (myWeighter != null) {
                throw InternalStaticCodeException
                        .explicitWeightsDisallowWeightedByClause(loc(h));
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
                        public Statement toAssignment(Expression value) {
                            throw InternalStaticCodeException.illegalLValue(
                                    myDescription, loc(h));
                        }
                    });
        }
    }
    
    private static interface LHSAccess {
        public Expression toValue();
        public Statement toAssignment(Expression value)
                throws WellFormednessException;
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
    
    public static final class Symbol {
        private final String myName;
        private final boolean myBakedFlag;
        private final ParseLocation myPosition;
        
        public Symbol(String name, boolean baked, ParseLocation p) {
            myName = name;
            myBakedFlag = baked;
            myPosition = p;
        }
        
        public String getName() {
            return myName;
        }
        
        public int getLineNumber() {
            return myPosition.getLineNumber();
        }
        
        public int getColumn() {
            return myPosition.getColumnNumber();
        }
        
        public String getLineContents() {
            return myPosition.getLineContents();
        }
        
        public String getIndent() {
            return myPosition.getAlignmentPrefix();
        }
        
        public ParseLocation getPosition() {
            return myPosition;
        }
        
        public boolean isBaked() {
            return myBakedFlag;
        }
        
        @Override
        public String toString() {
            return myName + " (" + myPosition.getLineNumber() + ":"
                    + myPosition.getColumnNumber() + ") "
                    + (myBakedFlag ? "bake" : "intro");
        }
    }
    
    private static class RootLexicalScope {
        protected Symbol getOwnSymbol(String name) {
            return null;
        }
        
        protected Symbol getParentSymbol(String name)
                throws NoSuchSymbolException, SymbolInaccessibleException {
            throw NoSuchSymbolException.INSTANCE;
        }
        
        public RootLexicalScope getParent() {
            throw new IllegalStateException();
        }
        
        public Symbol introduceSymbol(String name, boolean baked,
                ParseHead.Position p) throws DuplicateSymbolException {
            throw new IllegalStateException();
        }
        
        public Symbol getSymbol(String name)
                throws NoSuchSymbolException, SymbolInaccessibleException {
            throw NoSuchSymbolException.INSTANCE;
        }
    }
    
    private class InnerLexicalScope extends RootLexicalScope {
        private final RootLexicalScope myParent;
        
        private final Map<String, Symbol> mySymbols = new HashMap<>();
        
        public InnerLexicalScope(RootLexicalScope parent) {
            myParent = parent;
        }
        
        @Override
        public RootLexicalScope getParent() {
            return myParent;
        }
        
        @Override
        public Symbol introduceSymbol(String name, boolean baked,
                ParseHead.Position p) throws DuplicateSymbolException {
            Symbol def = mySymbols.get(name);
            if (def != null) {
                throw new DuplicateSymbolException(def.getPosition());
            }
            
            def = new Symbol(name, baked, loc(p));
            
            mySymbols.put(name, def);
            
            return def;
        }
        
        @Override
        protected Symbol getOwnSymbol(String name) {
            return mySymbols.get(name);
        }
        
        @Override
        protected Symbol getParentSymbol(String name)
                throws NoSuchSymbolException, SymbolInaccessibleException {
            return myParent.getSymbol(name);
        }
        
        @Override
        public Symbol getSymbol(String name)
                throws NoSuchSymbolException, SymbolInaccessibleException {
            Symbol result = mySymbols.get(name);
            
            if (result == null) {
                if (myParent == null) {
                    throw NoSuchSymbolException.INSTANCE;
                }
                else {
                    result = myParent.getSymbol(name);
                }
            }
            
            return result;
        }
    }
    
    private class ClosingLexicalScope extends InnerLexicalScope {
        public ClosingLexicalScope(RootLexicalScope parent) {
            super(parent);
        }

        @Override
        public Symbol getSymbol(String name)
                throws NoSuchSymbolException, SymbolInaccessibleException {
            Symbol def = getOwnSymbol(name);
            
            if (def == null) {
                def = getParentSymbol(name);
                
                if (!def.isBaked()) {
                    throw new SymbolInaccessibleException(def.getPosition());
                }
            }
            
            return def;
        }
    }
    
    private static final class NoSuchSymbolException extends Exception {
        public static final NoSuchSymbolException INSTANCE =
                new NoSuchSymbolException();
        
        private NoSuchSymbolException() {}

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }
    
    private static final class SymbolInaccessibleException
            extends Exception {
        private final ParseLocation myLocation;
        
        public SymbolInaccessibleException(ParseLocation location) {
            myLocation = location;
        }
        
        public ParseLocation getDefinitionLocation() {
            return myLocation;
        }
    }
    
    private static final class DuplicateSymbolException
            extends Exception {
        private final ParseLocation myPreviousDefinitionLocation;
        
        public DuplicateSymbolException(ParseLocation l) {
            myPreviousDefinitionLocation = l;
        }
        
        public ParseLocation getPreviousDefinitionLocation() {
            return myPreviousDefinitionLocation;
        }
    }
    
    public static final class ParseException extends Exception {
        private final int myLineNumber;
        private final int myColumn;
        
        private final String myLineContents;
        private final String myAlignmentPrefix;
        
        public ParseException(int lineNumber, int column, String lineContents,
                String alignmentPrefix, String message) {
            super(message);
            
            myLineNumber = lineNumber;
            myColumn = column;
            myLineContents = lineContents;
            myAlignmentPrefix = alignmentPrefix;
        }
        
        public int getLineNumber() {
            return myLineNumber;
        }
        
        public int getColumn() {
            return myColumn;
        }
        
        public String getLineContents() {
            return myLineContents;
        }
        
        public String getAlignmentPrefix() {
            return myAlignmentPrefix;
        }
        
        public void print(PrintWriter w) {
            w.println(getMessage());
            w.println();
            w.println("Line " + myLineNumber + ":");
            w.println(myLineContents);
            w.println(myAlignmentPrefix + "^");
        }
    }
    
    public static interface Decider {
        public boolean decide(double chance);
    }
}
