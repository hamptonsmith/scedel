package com.shieldsbetter.scedel.values;

import java.util.Objects;

public final class VToken extends SkeletonValue<VToken> {
    private final String myBackingKey;
    
    public VToken(String backingKey) {
        myBackingKey = backingKey;
    }
    
    public String getBackingKey() {
        return myBackingKey;
    }
    
    @Override
    public VToken copy(boolean errorOnVProy) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        
        if (!(o instanceof VToken)) {
            return false;
        }
        
        VToken oAsVt = (VToken) o;
        
        return myBackingKey.equals(oAsVt.getBackingKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(myBackingKey, getClass());
    }

    @Override
    public void accept(Visitor v) {
        v.visitVToken(this);
    }

    @Override
    public String getValueString() {
        return "(* token *) 1 / 0";
    }

    @Override
    public String toString() {
        return "VToken[" + myBackingKey + "]";
    }
}
