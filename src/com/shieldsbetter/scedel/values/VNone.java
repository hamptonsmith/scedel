package com.shieldsbetter.scedel.values;

public class VNone extends ImmutableValue<VNone> {
    public static VNone INSTANCE = new VNone();
    
    private VNone() {}

    @Override
    public void accept(Visitor v) {
        v.visitVNone(this);
    }

    @Override
    public String getValueString() {
        return "none";
    }
}
