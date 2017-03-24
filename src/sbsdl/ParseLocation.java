package sbsdl;

import java.io.PrintStream;

public class ParseLocation {
    public static final ParseLocation INTERNAL =
            new ParseLocation(0, 0, "", "internally-generated-location");
    
    private final int myLineNumber;
    private final int myColumnNumber;
    
    private final String myLineContents;
    private final String myAlignmentPrefix;
    
    public ParseLocation(int lineNumber, int columnNumber,
            String alignmentPrefix, String lineContents) {
        myLineNumber = lineNumber;
        myColumnNumber = columnNumber;
        myLineContents = lineContents;
        myAlignmentPrefix = alignmentPrefix;
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
        w.println("Line " + myLineNumber + ":");
        w.println(myLineContents);
        w.println(myAlignmentPrefix + "^");
    }
}
