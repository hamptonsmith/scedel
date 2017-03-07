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
import com.shieldsbetter.flexcompilator.matchers.MEndOfInput;
import com.shieldsbetter.flexcompilator.matchers.MForbid;
import com.shieldsbetter.flexcompilator.matchers.MLiteral;
import com.shieldsbetter.flexcompilator.matchers.MOptional;
import com.shieldsbetter.flexcompilator.matchers.MPlaceholder;
import com.shieldsbetter.flexcompilator.matchers.MSequence;
import com.shieldsbetter.flexcompilator.matchers.MRepeated;
import com.shieldsbetter.flexcompilator.matchers.MWithSkipper;
import com.shieldsbetter.flexcompilator.matchers.Matcher;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import sbsdl.values.VDict;
import sbsdl.values.VNumber;
import sbsdl.values.VSeq;
import sbsdl.values.VString;
import sbsdl.values.Value;

public class Sbsdl {
    private final ExtendedPredicateMatcher myExtendedPredicateMatcher =
            new ExtendedPredicateMatcher();
    
    private final MPlaceholder EXP = new MPlaceholder();
    
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
                    VNumber n = new VNumber(integralPart, 1);
                    
                    long pow = 1;
                    for (int i = 0; i < fractionalPartText.length(); i++) {
                        pow *= 10;
                    }
                    n.multiply(pow);
                    n.add(Long.parseLong(fractionalPartText));
                    n.divide(pow);
                    
                    myParseStack.push(n);
                }
            };
    
    private final Matcher INTEGRAL_LITERAL =
            new MAction(new MRepeated(CSet.ISO_LATIN_DIGIT, 1, 10)) {
                @Override
                public void onMatched(ParseHead h) {
                    String integerText = h.nextCapture();
                    VNumber n = new VNumber(Long.parseLong(integerText), 1);
                    
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
                    Value expValue = (Value) myParseStack.pop();
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

                    try {
                        Value v = myHostEnvironment.evaluate(
                                hostId, parameterList);
                        
                        if (v == null) {
                            throw new RuntimeException("Host environment "
                                    + "evaluated host expression #" + hostId
                                    + (parameterList == null ? ""
                                            : parameterList.toString()) + " to "
                                    + "null.");
                        }
                    }
                    catch (HostEnvironmentException hee) {
                        throw new ExecutionException(hee.getMessage());
                    }
                }
            };
    
    private final Matcher PARENTHETICAL_EXP =
            new MSequence(new MLiteral("("), EXP, new MLiteral(")"));
    
    private final Matcher LITERAL_META_LOOKUP =
            new MWithSkipper(
                    new MSequence(IDENTIFIER, new MLiteral(":"), IDENTIFIER),
                    null);
    
    private final Matcher EXPRESSION_META_LOOKUP =
            new MAction(new MSequence(
                    new MWithSkipper(
                            new MSequence(IDENTIFIER, new MLiteral(":")), null),
                    PARENTHETICAL_EXP)) {
                @Override
                public void onMatched(ParseHead h) {
                    if (!(myParseStack.peek() instanceof VString)) {
                        throw new ExecutionException("Host lookup key did not "
                                + "resolve to a string: "
                                + myParseStack.peek());
                    }
                    
                    myParseStack.push(
                            ((VString) myParseStack.pop()).getValue());
                }
            };
    
    private final Matcher META_LOOKUP =
            new MAction(new MAlternatives(
                        LITERAL_META_LOOKUP, EXPRESSION_META_LOOKUP)) {
                @Override
                public void onMatched(ParseHead h) {
                    String key = (String) myParseStack.pop();
                    String type = (String) myParseStack.pop();
                    
                    try {
                        Value v = myHostEnvironment.lookup(key, type);
                        
                        if (v == null) {
                            throw new RuntimeException("Host environment "
                                    + "evaluated host lookup expression "
                                    + type + ":" + key + " to null.");
                        }
                        
                        myParseStack.push(v);
                    }
                    catch (HostEnvironmentException hee) {
                        throw new ExecutionException(hee.getMessage());
                    }
                }
            };
    
    private final Matcher BOUNDED_EXP = new MAlternatives(META_LOOKUP,
            STRING_LITERAL, NUMERIC_LITERAL, PARENTHETICAL_EXP, IDENTIFIER,
            HOST_EXP);
    
    private final Matcher ACCESS_EXP =
            new MSequence(BOUNDED_EXP, new MRepeated(new MAlternatives(
                    new MAction(new MSequence(new MLiteral("."), IDENTIFIER)) {
                        @Override
                        public void onMatched(ParseHead h) {
                            if (!(myParseStack.peek() instanceof VDict)) {
                                throw new ExecutionException("Expression is "
                                        + "not a dictionary: "
                                        + myParseStack.peek());
                            }
                            
                            myParseStack.push(((VDict) myParseStack.pop()).get(
                                    new VString(h.nextCapture())));
                        }
                    },
                    new MAction(new MSequence(
                            new MLiteral("["), EXP, new MLiteral("]"))) {
                        @Override
                        public void onMatched(ParseHead h) {
                            Value index = (Value) myParseStack.pop();
                            Value referent = (Value) myParseStack.pop();
                            
                            if (referent instanceof VDict) {
                                myParseStack.push(
                                        ((VDict) referent).get(index));
                            }
                            else if (referent instanceof VSeq) {
                                VSeq referentSeq = (VSeq) referent;
                                
                                if (!(index instanceof VNumber)) {
                                    throw new ExecutionException("Index is "
                                            + "non-numeric but referent is a "
                                            + "sequence.  Index: " + index);
                                }
                                
                                VNumber indexNumber = (VNumber) index;
                                
                                if (!indexNumber.getDenominator().equals(
                                        BigInteger.ONE)) {
                                    throw new ExecutionException("Index is "
                                            + "non-integral but referent is "
                                            + "a sequence.  Index: " + index);
                                }
                                
                                BigInteger indexInt =
                                        indexNumber.getNumerator();
                                
                                if (indexInt.compareTo(
                                        BigInteger.valueOf(
                                                Integer.MAX_VALUE)) > 0) {
                                    throw new ExecutionException("Index too "
                                            + "large to index sequence.  "
                                            + "Index: " + index);
                                }
                                else if (indexInt.compareTo(BigInteger.ZERO)
                                        < 0) {
                                    throw new ExecutionException("Negative "
                                            + "index.  Index: " + index);
                                }
                                
                                myParseStack.push(
                                        referentSeq.get(indexInt.intValue()));
                            }
                            else {
                                throw new ExecutionException("Expression is not"
                                        + " a dictionary or sequence.");
                            }
                        }
                    })));
    
    private final Matcher PREFIX_EXP =
            new MSequence(
                    new MAlternatives(
                            myExtendedPredicateMatcher,
                            new MRepeated(new MLiteral("!"))),
                    ACCESS_EXP);
    
    private final Matcher POW_PRECEDENCE_EXP =
            new MSequence(PREFIX_EXP, new MRepeated(
                    new MSequence(new MLiteral("^"), PREFIX_EXP)));
    
    private final Matcher MULT_PRECEDENCE_EXP =
            new MSequence(POW_PRECEDENCE_EXP, new MRepeated(
                    new MSequence(
                            new MAlternatives(
                                    new MLiteral("*"), new MLiteral("/")),
                            POW_PRECEDENCE_EXP)));
    
    private final Matcher ADD_PRECEDENCE_EXP =
            new MSequence(MULT_PRECEDENCE_EXP, new MRepeated(
                    new MSequence(
                            new MAlternatives(
                                    new MLiteral("+"), new MLiteral("-")),
                            MULT_PRECEDENCE_EXP)));
    
    private final Matcher NUMERIC_EXP = ADD_PRECEDENCE_EXP;
    
    private final Matcher TRINARY_EXP =
            new MSequence(NUMERIC_EXP, new MLiteral("between"),
                    NUMERIC_EXP, new MLiteral("and"), NUMERIC_EXP);
    
    private final Matcher BINARY_COMPARISON_EXP =
            new MSequence(NUMERIC_EXP,
                    new MRepeated(
                            new MAlternatives(
                                    // Order important!  Prefixes can't come
                                    // first!
                                    new MLiteral("<="),
                                    new MLiteral("<"),
                                    new MLiteral("="),
                                    new MLiteral(">="),
                                    new MLiteral(">")),
                            NUMERIC_EXP));
    
    // Trinary needs to come first, otherwise the initial expression of a
    // trinary comparison will get gobbled up by the binary comparison
    // expression's "no operator actually present" version.
    private final Matcher COMPARISON_EXP =
            new MAlternatives(TRINARY_EXP, BINARY_COMPARISON_EXP);
    
    private final Matcher AND_PRECEDENCE_EXP =
            new MSequence(COMPARISON_EXP,
                    new MRepeated(new MLiteral("and"), COMPARISON_EXP));
    
    private final Matcher OR_PRECEDENCE_EXP =
            new MSequence(AND_PRECEDENCE_EXP,
                    new MRepeated(new MLiteral("or"), AND_PRECEDENCE_EXP));
    
    private final Matcher POOL_ELEMENT =
            new MSequence(EXP,
                    new MOptional(new MLiteral("{"), EXP, new MLiteral("}")));
    
    private final Matcher FROM_POOL = new MSequence(
            new MOptional(IDENTIFIER, new MLiteral(":")),
            new MAlternatives(
                    EXP,
                    new MSequence(
                            new MLiteral("{"),
                            new MOptional(POOL_ELEMENT), 
                                    new MRepeated(
                                            new MLiteral(","), POOL_ELEMENT),
                            new MLiteral("}"))));
    
    private final Matcher WHERE_CLAUSE =
            new MSequence(new MLiteral("where"), OR_PRECEDENCE_EXP);
    
    private final Matcher EXPLICIT_SINGLE_PICK_TAIL_EXP =
            new MSequence(FROM_POOL, new MOptional(WHERE_CLAUSE));
    
    private final Matcher EXPLICIT_PICK_TAIL_EXP = new MSequence(
            EXPLICIT_SINGLE_PICK_TAIL_EXP,
            new MRepeated(
                    new MLiteral("otherwise"), EXPLICIT_SINGLE_PICK_TAIL_EXP));
    
    private final Matcher EXPLICIT_PICK_EXP = new MSequence(
            new MLiteral("pick"), new MLiteral("from"), EXPLICIT_PICK_TAIL_EXP);
    
    // Explicit pick has to come first so we don't gobble up "pick" as an
    // identifier.
    private final Matcher PICK_EXP =
            new MAlternatives(EXPLICIT_PICK_EXP, OR_PRECEDENCE_EXP);
    
    {
        EXP.fillIn(PICK_EXP);
    }
    
    private final MPlaceholder STATEMENT = new MPlaceholder();
    
    private final Matcher CODE = new MRepeated(STATEMENT);
    
    private final Matcher CODE_BLOCK = new MSequence(
            new MLiteral("{"), CODE, new MLiteral("}"));
    
    private final Matcher ASSIGN_STMT = new MSequence(
            ACCESS_EXP, new MLiteral("="), EXP, new MLiteral(";"));
    
    private final Matcher FOR_EACH_STMT = new MSequence(new MLiteral("for"),
            new MLiteral("each"), EXPLICIT_PICK_TAIL_EXP, CODE_BLOCK);
    
    private final Matcher IF_STMT = new MSequence(new MLiteral("if"), EXP,
            CODE_BLOCK,
            new MRepeated(new MLiteral("else"), new MLiteral("if"), EXP,
                    CODE_BLOCK),
            new MOptional(new MLiteral("else"), CODE_BLOCK));
    
    private final Matcher INSTRUCTION_STMT =
            new MSequence(IDENTIFIER, PARAM_LIST_INNARDS, new MLiteral(";"));
    
    {
        STATEMENT.fillIn(new MAlternatives(
                ASSIGN_STMT, FOR_EACH_STMT, IF_STMT, INSTRUCTION_STMT));
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
    
    private final List<Matcher> myExtendedPredicates = new LinkedList<>();
    private MAlternatives myPrecompiledExtendedPredicates = new MAlternatives();
    
    private static enum ParseMode { EVALUATE, CONSTRUCT }
    private ParseMode myParseMode = ParseMode.EVALUATE;
    
    private final Deque myParseStack = new LinkedList();
    private final HostEnvironment myHostEnvironment;
    
    public Sbsdl(HostEnvironment e) {
        myHostEnvironment = e;
    }
    
    public void addPickPredicate(boolean hasArg, String ... keywords) {
        List<Matcher> literalList = new ArrayList<>(keywords.length);
        for (String literalDescription : keywords) {
            literalList.add(new MLiteral(literalDescription));
        }
        
        if (hasArg) {
            literalList.add(NUMERIC_EXP);
        }
        myExtendedPredicates.add(
                new MSequence(literalList.toArray(new Matcher[0])));
        
        myPrecompiledExtendedPredicates = new MAlternatives(
                myExtendedPredicates.toArray(new Matcher[0]));
    }
    
    public void parse(String input)
            throws NoMatchException, WellFormednessException {
        ParseHead h = new ParseHead(input);
        h.setSkip(DEFAULT_SKIPPER);
        h.advanceOver(CODE);
        
        System.out.println("REMAINING TEXT: " + h.remainingText());
    }
    
    private final class ExtendedPredicateMatcher implements Matcher {
        @Override
        public int match(ParseHead h)
                throws NoMatchException, WellFormednessException {
            int charCt = h.advanceOver(myPrecompiledExtendedPredicates);
            
            if (!myParseMode.equals(ParseMode.CONSTRUCT)) {
                throw new WellFormednessException("Can't use host predicate "
                        + "outside of 'pick' or 'for each' selector.");
            }
            
            return charCt;
        }
    }
    
    public static void main(String[] args) throws NoMatchException, WellFormednessException {
        Sbsdl p = new Sbsdl();
        p.addPickPredicate(true, "distance", "from");
        
        p.parse("conversation.reward =\n" +
"                pick from {\n" +
"                    module:AS  {1},\n" +
"                    module:LFT {1},\n" +
"                    module:AHG {1}\n" +
"                };\n" +
"	conversation.destination =\n" +
"                pick from #stations\n" +
"                where distance from this_station\n" +
"                      between #hopdist / 2 and #hopdist / 2 * 3\n" +
"                      and x <= 0.5;");
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
        
        /**
         * <p>Called to resolve host lookup expressions like {@code foo:bar}
         * or {@code foo:('b' + 'ar')} into host object proxies.  In
         * general, this mechanism is meant to provide scripts the ability to
         * identify host objects by some unique identifier like a name.</p>
         * 
         * <p>{@code type} will contain the identifier before the colon.  If the
         * colon is followed by an identifier, {@code key} will contain that
         * identifier verbatim.  Otherwise, if the colon is followed by a
         * parenthetical expression, that expression is asserted to be
         * string-valued and {@code key} will contain the result of evaluating
         * that expression.</p>
         * 
         * <p>In cases where the host lookup expression is understood but does
         * not correspond to a host object (e.g., the type and key are clearly
         * intend to refer to a player and that player <i>could</i> exist, but
         * <i>does not</i> exist at the moment), the host environment should
         * return
         * {@link sbsdl.values.VUnavailable#INSTANCE VUnavailable.INSTANCE}
         * rather than throw an exception.</p>
         * 
         * @param type The identifier before the colon.
         * @param key The identifier after the colon, or the string-valued
         *              result of evaluating the parenthetical expression after
         *              the colon.
         * @return A {@code VProxy} corresponding to the identified host object,
         *         or {@code VUnavailable.INSTANCE} if no such host object
         *         exists.  May not return {@code null}.
         * 
         * @throws sbsdl.Sbsdl.HostEnvironmentException If the lookup is not
         *              understood.
         */
        public Value lookup(String type, String key)
                throws HostEnvironmentException;
        
        /**
         * <p>Called when a script issues a command like
         * {@code foo 1 ('t' + 'wo') [3];} .  {@code command} will be
         * the text of the command identifier, while {@code parameters} will
         * contain the evaluated value of each of the provided parameters in
         * order (or be the empty list in the case where no parameters are
         * provided.)</p>
         * 
         * @param command The text of the command identifier.
         * @param parameters The list of evaluated parameters, or the empty list
         *              in the case that no parameters are provided.
         * 
         * @throws sbsdl.Sbsdl.HostEnvironmentException If the command is not
         *              understood, or for any reason can not be obeyed.
         */
        public void doCommand(String command, List<Value> parameters)
                throws HostEnvironmentException;
    }
    
    private static class ExecutionException extends RuntimeException {
        public ExecutionException(String msg) {
            super(msg);
        }
    }
}
