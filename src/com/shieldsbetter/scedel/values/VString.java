package com.shieldsbetter.scedel.values;

import java.util.Objects;
import com.shieldsbetter.scedel.ParseLocation;

public class VString extends ImmutableValue<VString> {
    private final String myValue;
    
    public VString(String s) {
        myValue = s;
    }
    
    public String getValue() {
        return myValue;
    }

    @Override
    public VString assertIsString(ParseLocation at) {
        return this;
    }
    
    @Override
    public String toString() {
        return myValue;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        
        if (!(o instanceof VString)) {
            return false;
        }
        
        VString oAsStr = (VString) o;
        
        return myValue.equals(oAsStr.getValue());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(VString.class, myValue);
    }

    @Override
    public void accept(Visitor v) {
        v.visitVString(this);
    }
}
