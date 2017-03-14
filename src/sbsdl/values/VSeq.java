package sbsdl.values;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;

public class VSeq extends SkeletonValue {
    public final LinkedList<Value> myValue = new LinkedList<>();
    
    public VSeq(Value ... vs) {
        myValue.addAll(Arrays.asList(vs));
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
        return myValue.get(i);
    }
    
    public void set(int i, Value v) {
        myValue.set(i, v);
    }

    @Override
    public VSeq assertIsSeq() {
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
}
