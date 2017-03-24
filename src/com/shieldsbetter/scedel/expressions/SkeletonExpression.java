package com.shieldsbetter.scedel.expressions;

import com.shieldsbetter.scedel.ParseLocation;

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
