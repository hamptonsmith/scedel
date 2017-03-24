package com.shieldsbetter.scedel;

import java.io.PrintStream;

public class StaticCodeException extends Exception {
    public static enum ErrorType {
        BAKED_MODIFICATION_FORBIDDEN,
        DUPLICATE_SYMBOL,
        EXPECTED_END_OF_STATEMENT,
        EXPLICIT_WEIGHTS_DISALLOW_WEIGHTED_BY_CLAUSE,
        ILLEGAL_LVALUE,
        INACCESSIBLE_SYMBOL,
        GENERIC_SYNTAX_ERROR,
        NO_SUCH_SYMBOL,
        PICK_COLLECTION_ALL_NEED_WEIGHTS,
        PICK_COLLECTION_NO_WEIGHTS,
        WEIGHTS_DISALLOWED_IN_FOR_EACH
    }
    
    protected final ErrorType myErrorType;
    protected final String myPrefixMessage;
    protected final ParseLocation myDetectionLocation;
    
    public StaticCodeException(ErrorType e, String msg, ParseLocation loc) {
        super("Static code exception " + e);
        myErrorType = e;
        myPrefixMessage = msg;
        myDetectionLocation = loc;
    }
    
    public ErrorType getErrorType() {
        return myErrorType;
    }
    
    public ParseLocation getDetectionLocation() {
        return myDetectionLocation;
    }
    
    public void print(PrintStream w) {
        w.println(myPrefixMessage);
        w.println();
        myDetectionLocation.print(w);
    }
    
    public StaticCodeException copy() {
        return new StaticCodeException(
                myErrorType, myPrefixMessage, myDetectionLocation);
    }
    
    public static class ReferentialStaticCodeException
            extends StaticCodeException {
        private final String myMiddleMessage;
        private final String myPostMessage;
        
        private final ParseLocation myReferenceLocation;
        private final boolean myDetectionOutputFirstFlag;
        
        public ReferentialStaticCodeException(ErrorType e, String prefixMsg,
                String middleMsg, String postMsg,
                ParseLocation detectionLocation,
                ParseLocation referenceLocation, boolean detectionOutputFirst) {
            super(e, prefixMsg, detectionLocation);
            
            myMiddleMessage = middleMsg;
            myPostMessage = postMsg;
            
            myReferenceLocation = referenceLocation;
            myDetectionOutputFirstFlag = detectionOutputFirst;
        }
        
        public ParseLocation getReferenceLocation() {
            return myReferenceLocation;
        }

        @Override
        public void print(PrintStream w) {
            w.println(myPrefixMessage);
            w.println();
            
            if (myDetectionOutputFirstFlag) {
                myDetectionLocation.print(w);
            }
            else {
                myReferenceLocation.print(w);
            }
            w.println();
            
            if (myMiddleMessage != null) {
                w.println(myMiddleMessage);
                w.println();
            }
            
            if (myDetectionOutputFirstFlag) {
                myReferenceLocation.print(w);
            }
            else {
                myDetectionLocation.print(w);
            }
            w.println();
            
            if (myPostMessage != null) {
                w.println(myPostMessage);
            }
        }

        @Override
        public ReferentialStaticCodeException copy() {
            return new ReferentialStaticCodeException(
                    myErrorType, myPrefixMessage, myMiddleMessage,
                    myPostMessage, myDetectionLocation, myReferenceLocation,
                    myDetectionOutputFirstFlag);
        }
    }
}
