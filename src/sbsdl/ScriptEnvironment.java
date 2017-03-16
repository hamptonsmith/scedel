package sbsdl;

import java.util.HashMap;
import java.util.Map;
import sbsdl.values.VUnavailable;
import sbsdl.values.Value;

public class ScriptEnvironment {
    private Scope myCurrentScope = new RootScope(null);
    
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
    
    public void putSymbol(String name, Value v) {
        myCurrentScope.putSymbol(name, v);
    }
    
    public void assignValue(String name, Value v) {
        myCurrentScope.assignValue(name, v);
    }
    
    public void popScope() {
        myCurrentScope = myCurrentScope.getParentScope();
    }
    
    public Value lookupVariable(String name) {
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
        
        private final Map<String, Value> myVariables = new HashMap<>();
        
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
        
        public void putSymbol(String name, Value v) {
            if (v == null) {
                throw new IllegalArgumentException();
            }
            
            if (myVariables.containsKey(name)) {
                throw new Sbsdl.ExecutionException(
                        "Symbol with that name already exists.");
            }
            
            myVariables.put(name, v);
        }
        
        public void assignValue(String name, Value v) {
            if (v == null) {
                throw new IllegalArgumentException();
            }
            
            if (myVariables.containsKey(name)) {
                myVariables.put(name, v);
            }
            else {
                if (myParent == null) {
                    throw new Sbsdl.ExecutionException(
                            "Variable not found: " + name);
                }
                
                myParent.assignValue(name, v);
            }
        }
        
        public Scope getParentScope() {
            return myParent;
        }
        
        public Value lookupVariable(String name) {
            Value result;
            
            result = myVariables.get(name);
            if (result == null) {
                if (myParent == null) {
                    throw new Sbsdl.ExecutionException(
                            "Variable not found: " + name);
                }
                
                result = myParent.lookupVariable(name);
                // This can't be null because we'd have thrown an exception.
                
                if (isRoot()) {
                    throw new Sbsdl.ExecutionException("Cannot access symbol \'"
                            + name + "\' outside of function literal.");
                }
            }
            
            return result;
        }
    }
}
