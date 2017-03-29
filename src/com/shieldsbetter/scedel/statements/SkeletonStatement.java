package com.shieldsbetter.scedel.statements;

import com.shieldsbetter.scedel.ParseLocation;

public abstract class SkeletonStatement implements Statement {
    private final ParseLocation myParseLocation;
    
    public SkeletonStatement(ParseLocation l) {
        myParseLocation = l;
    }
    
    @Override
    public ParseLocation getParseLocation() {
        return myParseLocation;
    }
}
