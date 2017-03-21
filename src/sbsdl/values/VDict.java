package sbsdl.values;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class VDict extends ContainerValue<VDict> {
    private final Map<Value, Value> myValue = new HashMap<>();
    
    public VDict() {
        this(null, Collections.EMPTY_MAP);
    }
    
    public VDict(String proxiesForbiddedError, Map<Value, Value> value) {
        super(proxiesForbiddedError != null);
        
        for (Map.Entry<Value, Value> entry : value.entrySet()) {
            myValue.put(entry.getKey().copy(proxiesForbiddedError),
                    entry.getValue().copy(proxiesForbiddedError));
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

    public int referenceHashCode() {
        return super.hashCode();
    }
    
    @Override
    public VDict assertIsDict() {
        return this;
    }

    @Override
    public VDict copy(String proxiesForbiddedError) {
        return new VDict(proxiesForbiddedError, myValue);
    }
}
