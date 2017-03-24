package com.shieldsbetter.scedel.values;

import java.util.Objects;
import com.shieldsbetter.scedel.ExecutionException;

public final class VToken extends SkeletonValue<VToken> {
    private final String myBackingKey;
    
    public VToken(String backingKey) {
        myBackingKey = backingKey;
    }
    
    public String getBackingKey() {
        return myBackingKey;
    }
    
    @Override
    public VToken copy(ExecutionException onProxy) {
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
}
