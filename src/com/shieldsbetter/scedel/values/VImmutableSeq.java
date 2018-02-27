package com.shieldsbetter.scedel.values;

public class VImmutableSeq extends VSeq {
    public VImmutableSeq(Value... elements) {
        super(elements);
    }
    
    @Override
    public Value dequeue() {
        throw new UnsupportedOperationException("Immutable sequence.");
    }

    @Override
    public void enqueue(Value v) {
        throw new UnsupportedOperationException("Immutable sequence.");
    }

    @Override
    public Value pop() {
        throw new UnsupportedOperationException("Immutable sequence.");
    }

    @Override
    public void push(Value v) {
        throw new UnsupportedOperationException("Immutable sequence.");
    }

    @Override
    public void set(int i, Value v) {
        throw new UnsupportedOperationException("Immutable sequence.");
    }
}
