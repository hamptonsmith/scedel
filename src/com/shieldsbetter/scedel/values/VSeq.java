package com.shieldsbetter.scedel.values;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import com.shieldsbetter.scedel.ExecutionException;
import com.shieldsbetter.scedel.ParseLocation;
import java.util.Iterator;

// Note as changes are made here that mutator methods must be overridden in
// VImmutableSeq.
public class VSeq extends ContainerValue<VSeq> implements Iterable<Value> {
    public final LinkedList<Value> myValue = new LinkedList<>();
    
    public VSeq(boolean forbidsProxies, List<Value> vs) {
        super(forbidsProxies);
        
        for (Value v : vs) {
            myValue.add(v.copy(forbidsProxies));
        }
    }
    
    public VSeq(Value ... vs) {
        this(false, Arrays.asList(vs));
    }
    
    @Override
    public Iterator<Value> iterator() {
        return new Iterator<Value>() {
            private final Iterator<Value> myBaseIterator = myValue.iterator();
            
            @Override
            public boolean hasNext() {
                return myBaseIterator.hasNext();
            }

            @Override
            public Value next() {
                return myBaseIterator.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException(
                        "Unmodifiable view iterator.");
            }
        };
    }
    
    public int length() {
        return myValue.size();
    }
    
    public int getElementCount() {
        return myValue.size();
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
    public VSeq copy(boolean errorOnVProxy) {
        return new VSeq(errorOnVProxy, myValue);
    }

    @Override
    public void accept(Visitor v) {
        v.visitVSeq(this);
    }

    @Override
    public String getValueString() {
        StringBuilder result = new StringBuilder();
        result.append("[");
        
        boolean needComma = false;
        for (Value v : myValue) {
            if (needComma) {
                result.append(",");
            }
            else {
                needComma = true;
            }
            
            result.append(" ");
            result.append(v);
        }
        
        result.append(" ]");
        return result.toString();
    }
}
