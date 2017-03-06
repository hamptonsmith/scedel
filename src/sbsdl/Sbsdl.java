package sbsdl;

import com.shieldsbetter.flexcompilator.matchers.CAny;
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
import com.shieldsbetter.flexcompilator.matchers.Matcher;

public class Sbsdl {
    private final MPlaceholder EXP = new MPlaceholder();
    
    private final Matcher STRING_LITERAL =
            new MSequence(
                    new MLiteral("'"),
                    new MCapture(new MRepeated(new MAlternatives(
                            new MError(new MLiteral("\n"), "Encountered end of "
                                    + "line before close of string literal."),
                            new CSubtract(
                                    CAny.INSTANCE, new COneOf('\'', '\\')),
                            new MSequence(new MLiteral("\\"),
                                    new MAlternatives(
                                            new MLiteral("n"),
                                            new MLiteral("r"),
                                            new MLiteral("'"),
                                            new MError(CAny.INSTANCE,
                                                    "Unrecognized control "
                                                            + "character in "
                                                            + "string."))),
                            new MError(MEndOfInput.INSTANCE, "Encountered end "
                                    + "of input before string closed.")
                    ))),
                    new MLiteral("'"));
    
    private final Matcher NUMERIC_LITERAL =
            new MAlternatives(
                    new MSequence(
                            new MRepeated(CSet.ISO_LATIN_DIGIT, 0, 10),
                            new MLiteral("."),
                            new MRepeated(CSet.ISO_LATIN_DIGIT, 1, 10)),
                    new MRepeated(CSet.ISO_LATIN_DIGIT, 1, 10));
    
    private final Matcher IDENTIFIER = new MAlternatives(
            new MSequence(CSet.LETTER,
                    new MRepeated(new MAlternatives(
                            CSet.LETTER_OR_DIGIT, new MLiteral("_")))),
            new MLiteral("@"));
    
    private final Matcher ENV_VALUE =
            new MSequence(new MLiteral("#"), CSet.LETTER,
                    new MRepeated(new MAlternatives(
                            CSet.LETTER_OR_DIGIT, new MLiteral("_"))));
    
    private final Matcher PARENTHETICAL_EXP =
            new MSequence(new MLiteral("("), EXP, new MLiteral(")"));
    
    private final Matcher BOUNDED_EXP = new MAlternatives(
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
    
    private final Matcher TRINARY_EXP = new MSequence(NUMERIC_EXP,
            new MOptional(new MLiteral("between"), NUMERIC_EXP,
                    new MLiteral("and"), NUMERIC_EXP));
    
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
                            new MOptional(POOL_ELEMENT, 
                                    new MRepeated(
                                            new MLiteral(","), POOL_ELEMENT)),
                            new MLiteral("}"))));
    
    private final Matcher WHERE_CLAUSE =
            new MSequence(new MLiteral("where"), EXP);
    
    private final Matcher EXPLICIT_SINGLE_PICK_TAIL_EXP =
            new MSequence(FROM_POOL, new MOptional(WHERE_CLAUSE));
    
    private final Matcher EXPLICIT_PICK_TAIL_EXP = new MSequence(
            EXPLICIT_SINGLE_PICK_TAIL_EXP,
            new MRepeated(
                    new MLiteral("otherwise"), EXPLICIT_SINGLE_PICK_TAIL_EXP));
    
    private final Matcher EXPLICIT_PICK_EXP = new MSequence(
            new MLiteral("pick"), new MLiteral("from"), EXPLICIT_PICK_TAIL_EXP);
    
    private final Matcher PICK_EXP =
            new MAlternatives(OR_PRECEDENCE_EXP, EXPLICIT_PICK_EXP);
    
    {
        EXP.fillIn(PICK_EXP);
    }
    
    private final MPlaceholder STATEMENT = new MPlaceholder();
    
    private final Matcher CODE_BLOCK = new MSequence(
            new MLiteral("{"), new MRepeated(STATEMENT), new MLiteral("}"));
    
    private final Matcher ASSIGN_STMT = new MSequence(
            ACCESS_EXP, new MLiteral("="), EXP, new MLiteral(";"));
    
    private final Matcher FOR_EACH_STMT = new MSequence(new MLiteral("for"),
            new MLiteral("each"), EXPLICIT_PICK_TAIL_EXP, CODE_BLOCK);
    
    private final Matcher IF_STMT = new MSequence(new MLiteral("if"), EXP,
            CODE_BLOCK,
            new MRepeated(new MLiteral("else"), new MLiteral("if"), CODE_BLOCK),
            new MOptional(new MLiteral("else"), CODE_BLOCK));
    
    private final Matcher INSTRUCTION_STMT =
            new MSequence(IDENTIFIER, PARAM_LIST_INNARDS);
    
    {
        STATEMENT.fillIn(new MAlternatives(
                ASSIGN_STMT, FOR_EACH_STMT, IF_STMT, INSTRUCTION_STMT));
    }
}
