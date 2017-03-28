package com.shieldsbetter.scedel;

import java.io.PrintStream;

public class ParseLocation {
    public static final ParseLocation INTERNAL =
            new ParseLocation("hardcoded-in-scedel", 0, 0, "",
                    "internally-generated-location");
    
    private final String mySourceDescription;
    private final int myLineNumber;
    
    private final Integer myColumnNumber;
    private final String myLineContents;
    private final String myAlignmentPrefix;
    
    public ParseLocation(String sourceDescription, int lineNumber) {
        mySourceDescription = sourceDescription;
        myLineNumber = lineNumber;
        
        myColumnNumber = null;
        myLineContents = null;
        myAlignmentPrefix = null;
    }
    
    public ParseLocation(String sourceDescription, int lineNumber,
            int columnNumber, String alignmentPrefix, String lineContents) {
        mySourceDescription = sourceDescription;
        myLineNumber = lineNumber;
        myColumnNumber = columnNumber;
        myLineContents = lineContents;
        myAlignmentPrefix = alignmentPrefix;
    }
    
    public String getSourceDescription() {
        return mySourceDescription;
    }
    
    public int getLineNumber() {
        return myLineNumber;
    }
    
    public int getColumnNumber() {
        return myColumnNumber;
    }
    
    public String getLineContents() {
        return myLineContents;
    }
    
    public String getAlignmentPrefix() {
        return myAlignmentPrefix;
    }
    
    public void print(PrintStream w) {
        w.print(mySourceDescription);
        
        if (myLineContents != null) {
            w.println();
            w.println("Line " + myLineNumber + ":");
            w.println(myLineContents);
            
            if (myAlignmentPrefix != null) {
                w.println(myAlignmentPrefix + "^");
            }
        }
        else {
            w.print(", line " + myLineNumber);
            if (myColumnNumber != null) {
                w.print(", col " + myColumnNumber);
            }
            w.println();
        }
    }
}
