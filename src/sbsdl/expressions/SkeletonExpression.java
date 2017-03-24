package sbsdl.expressions;

import sbsdl.ParseLocation;

public abstract class SkeletonExpression implements Expression {
    private final ParseLocation myParseLocation;
    
    public SkeletonExpression(ParseLocation l) {
        myParseLocation = l;
    }
    
    @Override
    public ParseLocation getParseLocation() {
        return myParseLocation;
    }
}
