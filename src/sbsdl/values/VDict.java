package sbsdl.values;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class VDict extends SkeletonValue<VDict> {
    private final Map<Value, Value> myValue = new HashMap<>();
    
    public VDict() { }
    public VDict(Map<Value, Value> value) {
        for (Map.Entry<Value, Value> entry : value.entrySet()) {
            myValue.put(entry.getKey().copy(), entry.getValue().copy());
        }
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

    @Override
    public VDict assertIsDict() {
        return this;
    }

    @Override
    public VDict copy() {
        return new VDict(myValue);
    }
}
