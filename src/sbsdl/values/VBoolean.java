package sbsdl.values;

import sbsdl.ParseLocation;

public class VBoolean extends ImmutableValue<VBoolean> {
    public static final VBoolean TRUE = new VBoolean(true);
    public static final VBoolean FALSE = new VBoolean(false);
    
    public static VBoolean of(boolean value) {
        return value ? TRUE : FALSE;
    }
    
    private final boolean myValue;
    
    private VBoolean(boolean value) {
        myValue = value;
    }
    
    public boolean getValue() {
        return myValue;
    }
    
    public VBoolean and(VBoolean o) {
        return VBoolean.of(myValue && o.getValue());
    }
    
    public VBoolean or(VBoolean o) {
        return VBoolean.of(myValue || o.getValue());
    }
    
    public VBoolean not() {
        return VBoolean.of(!myValue);
    }
    
    @Override
    public VBoolean assertIsBoolean(ParseLocation at) {
        return this;
    }

    @Override
    public String toString() {
        return "VB" + ("" + myValue).toUpperCase();
    }
}
