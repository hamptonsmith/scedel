package com.shieldsbetter.scedel;

import java.util.HashMap;
import java.util.Map;
import com.shieldsbetter.scedel.values.Value;
import java.util.Deque;
import java.util.LinkedList;

public class ScriptEnvironment {
    private final Deque<Picker> myPickers = new LinkedList<>();
    private Scope myCurrentScope = new RootScope(null);
    
    public ScriptEnvironment(Scedel.Decider d) {
        myPickers.push(Picker.Util.buildStandardPicker(d));
    }
    
    public void pushPicker(Picker p) {
        myPickers.push(p);
    }
    
    public void popPicker() {
        if (myPickers.size() == 1) {
            throw new RuntimeException();
        }
        
        myPickers.pop();
    }
    
    public Picker getPicker() {
        return myPickers.peek();
    }
    
    public Map<Scedel.Symbol, Value> getBakedValues() {
        Map<Scedel.Symbol, Value> result = new HashMap<>();
        myCurrentScope.buildBakedValues(result);
        return result;
    }
    
    public void pushScope(boolean root) {
        if (root) {
            myCurrentScope= new RootScope(myCurrentScope);
        }
        else {
            myCurrentScope = new Scope(myCurrentScope);
        }
    }
    
    public void setReturn(Value v) {
        myCurrentScope.setReturn(v);
    }
    
    public Value getReturn() {
        return myCurrentScope.getReturn();
    }
    
    public void introduceSymbol(Scedel.Symbol name, Value v) {
        myCurrentScope.introduceSymbol(name, v);
    }
    
    public void assignValue(Scedel.Symbol name, Value v) {
        myCurrentScope.assignValue(name, v);
    }
    
    public void popScope() {
        myCurrentScope = myCurrentScope.getParentScope();
        
        if (myCurrentScope == null) {
            throw new RuntimeException();
        }
    }
    
    public Value lookupVariable(Scedel.Symbol name) {
        return myCurrentScope.lookupVariable(name);
    }
    
    private static class RootScope extends Scope {
        private Value myReturnValue;
        
        public RootScope(Scope parent) {
            super(parent);
        }
        
        @Override
        public void setReturn(Value v) {
            myReturnValue = v;
        }
        
        @Override
        public Value getReturn() {
            return myReturnValue;
        }

        @Override
        public boolean isRoot() {
            return true;
        }
    }
    
    private static class Scope {
        private final Scope myRootParent;
        private final Scope myParent;
        
        private final Map<Scedel.Symbol, Value> myVariables = new HashMap<>();
        
        public Scope(Scope parent) {
            myParent = parent;
            
            if (parent == null) {
                myRootParent = null;
            }
            else if (parent.isRoot()) {
                myRootParent = parent;
            }
            else {
                myRootParent = parent.getRootParent();
            }
        }
        
        public void buildBakedValues(Map<Scedel.Symbol, Value> accum) {
            if (myParent != null) {
                myParent.buildBakedValues(accum);
            }
            
            for (Map.Entry<Scedel.Symbol, Value> entry
                    : myVariables.entrySet()) {
                if (entry.getKey().isBaked()) {
                    // No need for a value copy.  It's been baked.
                    accum.put(entry.getKey(), entry.getValue());
                }
            }
        }
        
        public void setReturn(Value v) {
            myRootParent.setReturn(v);
        }
        
        public Value getReturn() {
            return myRootParent.getReturn();
        }
        
        public boolean isRoot() {
            return false;
        }
        
        public Scope getRootParent() {
            return myRootParent;
        }
        
        public void introduceSymbol(Scedel.Symbol name, Value v) {
            if (myVariables.containsKey(name)) {
                throw new RuntimeException();
            }
            
            myVariables.put(name, v);
        }
        
        public void assignValue(Scedel.Symbol name, Value v) {
            if (v == null) {
                throw new IllegalArgumentException();
            }
            
            if (myVariables.containsKey(name)) {
                myVariables.put(name, v);
            }
            else {
                myParent.assignValue(name, v);
            }
        }
        
        public Scope getParentScope() {
            return myParent;
        }
        
        public Value lookupVariable(Scedel.Symbol name) {
            Value result;
            
            result = myVariables.get(name);
            if (result == null) {
                if (myParent == null) {
                    throw new RuntimeException();
                }
                
                result = myParent.lookupVariable(name);
            }
            
            return result;
        }
    }
}
