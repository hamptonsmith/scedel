package com.shieldsbetter.scedel.values;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import com.shieldsbetter.scedel.ExecutionException;
import com.shieldsbetter.scedel.ParseLocation;

public class VSeq extends ContainerValue<VSeq> {
    public final LinkedList<Value> myValue = new LinkedList<>();
    
    public VSeq(ExecutionException onProxy, List<Value> vs) {
        super(onProxy != null);
        
        for (Value v : vs) {
            myValue.add(v.copy(onProxy));
        }
    }
    
    public VSeq(Value ... vs) {
        this(null, Arrays.asList(vs));
    }
    
    public Iterable<Value> elements() {
        return myValue;
    }
    
    public void enqueue(Value v) {
        myValue.addLast(v);
    }
    
    public Value dequeue() {
        return myValue.removeFirst();
    }
    
    public void push(Value v) {
        myValue.push(v);
    }
    
    public Value pop() {
        return myValue.pop();
    }
    
    public Value get(int i) {
        if (i < 0) {
            throw new RuntimeException();
        }
        
        if (i >= myValue.size()) {
            throw new RuntimeException();
        }
        
        return myValue.get(i);
    }
    
    public void set(int i, Value v) {
        myValue.set(i, v);
    }

    @Override
    public VSeq assertIsSeq(ParseLocation at) {
        return this;
    }
    
    @Override
    public String toString() {
        return myValue.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        
        if (!(o instanceof VSeq)) {
            return false;
        }
        
        VSeq oAsSeq = (VSeq) o;
        
        return myValue.equals(oAsSeq.myValue);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(VSeq.class, myValue);
    }

    @Override
    public VSeq copy(ExecutionException onProxy) {
        return new VSeq(onProxy, myValue);
    }

    @Override
    public void accept(Visitor v) {
        v.visitVSeq(this);
    }
}
