package com.shieldsbetter.scedel.values;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.statements.MultiplexingStatement;

public class VFunction extends ImmutableValue<VFunction> {
    public static VFunction buildConstantFunction(
            final int argCt, final Value result) {
        return new VFunction() {
            @Override
            public Value call(ParseLocation l, Scedel.HostEnvironment h,
                    ScriptEnvironment s, List<Value> parameters) {
                return result;
            }

            @Override
            public int getArgumentCount() {
                return argCt;
            }
        };
    }
    
    private final List<Scedel.Symbol> myArgumentNames;
    private final MultiplexingStatement myCode;
    private final Map<Scedel.Symbol, Value> myBakedValues;
    
    public VFunction(List<Scedel.Symbol> argumentNames,
            MultiplexingStatement code, ScriptEnvironment environment) {
        this(argumentNames, code, environment.getBakedValues());
    }
    
    public VFunction(List<Scedel.Symbol> argumentNames,
            MultiplexingStatement code, Map<Scedel.Symbol, Value> bakedVals) {
        myArgumentNames = new ArrayList<>(argumentNames);
        myCode = code;
        myBakedValues = new HashMap<>(bakedVals);
    }
    
    private VFunction() {
        myArgumentNames = null;
        myCode = null;
        myBakedValues = new HashMap<>();
    }
    
    public List<Scedel.Symbol> getArgumentNames() {
        return myArgumentNames;
    }
    
    public MultiplexingStatement getCode() {
        return myCode;
    }
    
    public int getBakedValueCount() {
        return myBakedValues.size();
    }
    
    public Iterable<Map.Entry<Scedel.Symbol, Value>> getBakedValues() {
        return myBakedValues.entrySet();
    }
    
    public int getArgumentCount() {
        return myArgumentNames.size();
    }
    
    public Value call(ParseLocation at, Scedel.HostEnvironment h,
            ScriptEnvironment s, List<Value> parameters) {
        s.pushScope(false);
        for (Map.Entry<Scedel.Symbol, Value> bakedEntry
                : myBakedValues.entrySet()) {
            s.introduceSymbol(bakedEntry.getKey(), bakedEntry.getValue());
        }
        
        s.pushScope(true);
        if (myArgumentNames.size() != parameters.size()) {
            throw new RuntimeException();
        }
        
        Iterator<Value> paramIter = parameters.iterator();
        for (Scedel.Symbol arg : myArgumentNames) {
            s.introduceSymbol(arg, paramIter.next());
        }
        
        myCode.execute(h, s);
        
        Value returnValue = s.getReturn();
        if (returnValue == null) {
            returnValue = VUnavailable.INSTANCE;
        }
        
        s.popScope();
        s.popScope();
        
        return returnValue;
    }
    
    @Override
    public VFunction assertIsFunction(ParseLocation at) {
        return this;
    }

    @Override
    public void accept(Visitor v) {
        v.visitVFunction(this);
    }
}
