package com.shieldsbetter.scedel;

public class InternalStaticCodeException extends RuntimeException {
    private final StaticCodeException myStaticCodeException;
    
    public InternalStaticCodeException(StaticCodeException p) {
        myStaticCodeException = p;
    }
    
    public StaticCodeException getStaticCodeException() {
        return myStaticCodeException;
    }
    
    public static InternalStaticCodeException inaccessibleSymbol(
            ParseLocation detected, ParseLocation reference) {
        return new InternalStaticCodeException(
                new StaticCodeException.ReferentialStaticCodeException(
                        StaticCodeException.ErrorType.INACCESSIBLE_SYMBOL,
                        "This identifier:", "Seems to reference this symbol:",
                        "But that symbol would need to be baked to be "
                                + "accessible.", detected, reference, true));
    }
    
    public static InternalStaticCodeException noSuchSymbol(
            ParseLocation detected) {
        return new InternalStaticCodeException(
                new StaticCodeException(
                        StaticCodeException.ErrorType.NO_SUCH_SYMBOL,
                        "No such symbol.", detected));
    }
    
    public static InternalStaticCodeException duplicateSymbol(
            ParseLocation detected, ParseLocation reference) {
        return new InternalStaticCodeException(
                new StaticCodeException.ReferentialStaticCodeException(
                        StaticCodeException.ErrorType.DUPLICATE_SYMBOL,
                        "Symbol already exists.", "Previous definition here:",
                        null, detected, reference, true));
    }
    
    public static InternalStaticCodeException bakedModifyForbidden(
            ParseLocation detected) {
        return new InternalStaticCodeException(
                new StaticCodeException(
                        StaticCodeException.ErrorType
                                .BAKED_MODIFICATION_FORBIDDEN,
                        "Cannot modify the value of a baked variable.",
                        detected));
    }
    
    public static InternalStaticCodeException expectedEndOfStatement(
            ParseLocation detected) {
        return new InternalStaticCodeException(
                new StaticCodeException(
                        StaticCodeException.ErrorType.EXPECTED_END_OF_STATEMENT,
                        "Expected end of statement", detected));
    }
    
    public static InternalStaticCodeException pickCollectionAllNeedWeights(
            ParseLocation detected) {
        return new InternalStaticCodeException(
                new StaticCodeException(
                        StaticCodeException.ErrorType
                                .PICK_COLLECTION_ALL_NEED_WEIGHTS,
                        "If the first element of a pick collection has an "
                                + "explicit weight, all subsequence elements "
                                + "must also have an explicit weight.",
                        detected));
    }
    
    public static InternalStaticCodeException pickCollectionNoWeights(
            ParseLocation detected) {
        return new InternalStaticCodeException(
                new StaticCodeException(
                        StaticCodeException.ErrorType
                                .PICK_COLLECTION_NO_WEIGHTS,
                        "If the first element of a pick collection does not "
                                + "have an explicit weight, all subsequence "
                                + "elements must also not have an explicit "
                                + "weight.",
                        detected));
    }
    
    public static InternalStaticCodeException
            explicitWeightsDisallowWeightedByClause(
                    ParseLocation detected) {
        return new InternalStaticCodeException(
                new StaticCodeException(
                        StaticCodeException.ErrorType
                                .EXPLICIT_WEIGHTS_DISALLOW_WEIGHTED_BY_CLAUSE,
                        "Pick collections with explicit weightings cannot also "
                                + "have a 'weighted by' clause.",
                        detected));
    }
            
    public static InternalStaticCodeException weightsDisallowedInForEach(
            ParseLocation detected) {
        return new InternalStaticCodeException(
                new StaticCodeException(
                        StaticCodeException.ErrorType
                                .WEIGHTS_DISALLOWED_IN_FOR_EACH,
                        "Explicitly weighted collections are not allowed in "
                                + "for-each collections.",
                        detected));
    }
    
    public static InternalStaticCodeException illegalLValue(
            String typeDesc, ParseLocation detected) {
        return new InternalStaticCodeException(
                new StaticCodeException(
                        StaticCodeException.ErrorType.ILLEGAL_LVALUE,
                        "Cannot assign values to " + typeDesc + ".",
                        detected));
    }
}
