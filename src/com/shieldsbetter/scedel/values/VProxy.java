package com.shieldsbetter.scedel.values;

public class VProxy extends VDict {
    public VProxy() {
        super(true);
    }

    @Override
    public final boolean equals(Object o) {
        return o == this;
    }

    @Override
    public final int hashCode() {
        return referenceHashCode();
    }
    
    @Override
    public final VDict copy(boolean errorOnVProxy) {
        if (errorOnVProxy) {
            throw new CannotCopyVProxyException();
        }
        
        return this;
    }

    @Override
    public void accept(Visitor v) {
        v.visitVProxy(this);
    }

    @Override
    public String getValueString() {
        return "(* proxy *) 1 / 0";
    }
}
