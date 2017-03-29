package com.shieldsbetter.scedel;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import com.shieldsbetter.scedel.values.Value;

public class ExecutionException extends Exception {
    public static enum ErrorType {
        CANNOT_BAKE_PROXY,
        DECIDER_INDEXES_EXHAUSTED,
        DIVISION_BY_ZERO,
        HOST_ENVIRONMENT_EXCEPTION,
        ILLEGAL_PROXY_CONTAINMENT,
        INCORRECT_NUMBER_OF_PARAMETERS,
        INVALID_WEIGHTER,
        NEGATIVE,
        NON_INTEGRAL,
        NOT_A_BOOLEAN,
        NOT_A_DICTIONARY,
        NOT_A_FUNCTION,
        NOT_A_NUMBER,
        NOT_A_SEQUENCE,
        NOT_A_STRING,
        PLUS_FIRST_PARAMETER_MUST_BE_NUMBER_OR_STRING,
        TOO_LARGE,
    };
    
    private final ErrorType myErrorType;
    private final String myPrefixMessage;
    private final ParseLocation myLocation;
    private final Value myOffendingValue;
    private final List<ParseLocation> myStackTrace;
    
    public ExecutionException(ErrorType e, String message, ParseLocation l,
            Value offendingValue) {
        this(e, message, l, offendingValue, new LinkedList<ParseLocation>());
    }
    
    public ExecutionException(ErrorType e, String message, ParseLocation l,
            Value offendingValue, List<ParseLocation> stackTrace) {
        super("Execution Exception " + e);
        
        myErrorType = e;
        myPrefixMessage = message;
        myLocation = l;
        myOffendingValue = offendingValue;
        myStackTrace = new LinkedList<>(stackTrace);
    }
    
    public List<ParseLocation> getScriptStackTrace() {
        return new LinkedList<>(myStackTrace);
    }
    
    public ErrorType getErrorType() {
        return myErrorType;
    }
    
    public Value getOffendingValue() {
        return myOffendingValue;
    }
    
    public ExecutionException copy(List<ParseLocation> stackTrace) {
        return new ExecutionException(
                myErrorType, myPrefixMessage, myLocation, myOffendingValue,
                stackTrace);
    }
    
    public void print(PrintStream w) {
        w.println(myPrefixMessage);
        w.println();
        myLocation.print(w);
        
        for (ParseLocation s : myStackTrace) {
            w.println();
            w.print("via ");
            s.print(w);
        }
    }
}
