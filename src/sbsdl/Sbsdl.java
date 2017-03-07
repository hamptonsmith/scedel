package sbsdl;

import com.shieldsbetter.flexcompilator.NoMatchException;
import com.shieldsbetter.flexcompilator.ParseHead;
import com.shieldsbetter.flexcompilator.WellFormednessException;
import com.shieldsbetter.flexcompilator.matchers.COneOf;
import com.shieldsbetter.flexcompilator.matchers.CSet;
import com.shieldsbetter.flexcompilator.matchers.CSubtract;
import com.shieldsbetter.flexcompilator.matchers.MAlternatives;
import com.shieldsbetter.flexcompilator.matchers.MCapture;
import com.shieldsbetter.flexcompilator.matchers.MEndOfInput;
import com.shieldsbetter.flexcompilator.matchers.MError;
import com.shieldsbetter.flexcompilator.matchers.MLiteral;
import com.shieldsbetter.flexcompilator.matchers.MOptional;
import com.shieldsbetter.flexcompilator.matchers.MPlaceholder;
import com.shieldsbetter.flexcompilator.matchers.MSequence;
import com.shieldsbetter.flexcompilator.matchers.MRepeated;
import com.shieldsbetter.flexcompilator.matchers.MWithSkipper;
import com.shieldsbetter.flexcompilator.matchers.Matcher;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Sbsdl {
    private final ExtendedPredicateMatcher myExtendedPredicateMatcher =
            new ExtendedPredicateMatcher();
    
    private final MPlaceholder EXP = new MPlaceholder();
    
    private final Matcher STRING_LITERAL =
            new MWithSkipper(new MSequence(
                    new MLiteral("'"),
                    new MCapture(new MRepeated(new MAlternatives(
                            new MError(new MLiteral("\n"), "Encountered end of "
                                    + "line before close of string literal."),
                            new CSubtract(
                                    CSet.ANY, new COneOf('\'', '\\')),
                            new MSequence(new MLiteral("\\"),
                                    new MAlternatives(
                                            new MLiteral("n"),
                                            new MLiteral("r"),
                                            new MLiteral("'"),
                                            new MError(CSet.ANY,
                                                    "Unrecognized control "
                                                            + "character in "
                                                            + "string."))),
                            new MError(MEndOfInput.INSTANCE, "Encountered end "
                                    + "of input before string closed.")
                    ))),
                    new MLiteral("'")),
                null);
    
    private final Matcher NUMERIC_LITERAL =
            new MWithSkipper(new MAlternatives(
                    new MSequence(
                            new MRepeated(CSet.ISO_LATIN_DIGIT, 0, 10),
                            new MLiteral("."),
                            new MRepeated(CSet.ISO_LATIN_DIGIT, 1, 10)),
                    new MRepeated(CSet.ISO_LATIN_DIGIT, 1, 10)),
                null);
    
    private final Matcher IDENTIFIER = new MWithSkipper(new MAlternatives(
            new MSequence(CSet.LETTER,
                    new MRepeated(new MAlternatives(
                            CSet.LETTER_OR_DIGIT, new MLiteral("_")))),
            new MLiteral("@")),
        null);
    
    private final Matcher ENV_VALUE =
            new MWithSkipper(new MSequence(new MLiteral("#"), CSet.LETTER,
                    new MRepeated(new MAlternatives(
                            CSet.LETTER_OR_DIGIT, new MLiteral("_")))),
                null);
    
    private final Matcher PARENTHETICAL_EXP =
            new MSequence(new MLiteral("("), EXP, new MLiteral(")"));
    
    private final Matcher META_LOOKUP =
            new MWithSkipper(
                    new MSequence(IDENTIFIER, new MLiteral(":"), IDENTIFIER),
                    null);
    
    private final Matcher BOUNDED_EXP = new MAlternatives(META_LOOKUP,
            STRING_LITERAL, NUMERIC_LITERAL, PARENTHETICAL_EXP, IDENTIFIER,
            ENV_VALUE);
    
    private final Matcher PARAM_LIST_INNARDS = new MOptional(
            new MSequence(
                    EXP,
                    new MRepeated(new MLiteral(","), EXP)));
    
    private final Matcher ACCESS_EXP =
            new MSequence(BOUNDED_EXP, new MRepeated(new MAlternatives(
                    new MSequence(new MLiteral("."), IDENTIFIER),
                    new MSequence(new MLiteral("#"), IDENTIFIER),
                    new MSequence(new MLiteral("["), EXP, new MLiteral("]")),
                    new MSequence(
                            new MLiteral("("),
                            PARAM_LIST_INNARDS,
                            new MLiteral(")")))));
    
    private final Matcher PREFIX_EXP =
            new MSequence(new MRepeated(new MLiteral("!")), ACCESS_EXP);
    
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
    
    private final Matcher TRINARY_TAIL = new MOptional(new MLiteral("between"),
            NUMERIC_EXP, new MLiteral("and"), NUMERIC_EXP);
    
    private final Matcher TRINARY_EXP =
            new MSequence(NUMERIC_EXP, TRINARY_TAIL);
    
    private final Matcher AND_PRECEDENCE_EXP =
            new MSequence(TRINARY_EXP, new MRepeated(
                    new MSequence(new MLiteral("and"), NUMERIC_EXP)));
    
    private final Matcher OR_PRECEDENCE_EXP =
            new MSequence(AND_PRECEDENCE_EXP, new MRepeated(
                    new MSequence(new MLiteral("or"), AND_PRECEDENCE_EXP)));
    
    private final Matcher POOL_ELEMENT =
            new MSequence(EXP, new MOptional(
                    new MSequence(new MLiteral("{"), EXP, new MLiteral("}"))));
    
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
            new MSequence(new MLiteral("where"), 
                    new MAlternatives(
                            myExtendedPredicateMatcher,
                            OR_PRECEDENCE_EXP));
    
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
            return myPrecompiledExtendedPredicates.match(h);
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
"                where distance from this_station;");/*\n" +
"                      between #hopdist / 2 and #hopdist / 2 * 3;");*/
    }
}
