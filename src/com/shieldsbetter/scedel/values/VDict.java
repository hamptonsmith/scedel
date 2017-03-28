package com.shieldsbetter.scedel.values;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import com.shieldsbetter.scedel.ExecutionException;
import com.shieldsbetter.scedel.ParseLocation;

public class VDict extends ContainerValue<VDict> {
    private final Map<Value, Value> myValue = new HashMap<>();
    
    public VDict() {
        this(false);
    }
    
    public VDict(boolean forbidsProxies) {
        super(forbidsProxies);
    }
    
    public VDict(ExecutionException onProxy, Map<Value, Value> value) {
        super(onProxy != null);
        
        for (Map.Entry<Value, Value> entry : value.entrySet()) {
            myValue.put(entry.getKey().copy(onProxy),
                    entry.getValue().copy(onProxy));
        }
    }
    
    public int size() {
        return myValue.size();
    }
    
    public Iterable<Map.Entry<Value, Value>> entries() {
        return myValue.entrySet();
    }
    
    public VDict put(Value key, Value val) {
        if (val == VUnavailable.INSTANCE) {
            myValue.remove(key);
        }
        else {
            myValue.put(key, val);
        }
        
        return this;
    }
    
    public Value get(Value key) {
        Value result;
        
        if (myValue.containsKey(key)) {
            result = myValue.get(key);
        }
        else {
            result = VUnavailable.INSTANCE;
        }
        
        return result;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        
        if (!(o instanceof VDict)) {
            return false;
        }
        
        VDict oAsDict = (VDict) o;
        
        return myValue.equals(oAsDict.myValue);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(VDict.class, myValue);
    }

    public int referenceHashCode() {
        return super.hashCode();
    }
    
    @Override
    public VDict assertIsDict(ParseLocation at) {
        return this;
    }

    @Override
    public VDict copy(ExecutionException onProxy) {
        return new VDict(onProxy, myValue);
    }

    @Override
    public void accept(Visitor v) {
        v.visitVDict(this);
    }
}
