package sbsdl.statements;

import sbsdl.ParseLocation;

public abstract class SkeletonStatement implements Statement {
    private final ParseLocation myParseLocation;
    
    public SkeletonStatement(ParseLocation l) {
        myParseLocation = l;
    }
    
    public ParseLocation getParseLocation() {
        return myParseLocation;
    }
}
