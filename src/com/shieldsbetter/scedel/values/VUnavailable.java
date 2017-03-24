package com.shieldsbetter.scedel.values;

public class VUnavailable extends ImmutableValue<VUnavailable> {
    public static VUnavailable INSTANCE = new VUnavailable();
    
    private VUnavailable() { }

    @Override
    public void accept(Visitor v) {
        v.visitVUnavailable(this);
    }
}
