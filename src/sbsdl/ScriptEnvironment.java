package sbsdl;

import java.util.HashMap;
import java.util.Map;
import sbsdl.values.Value;

public class ScriptEnvironment {
    private Scope myCurrentScope = new Scope(null, false);
    
    public void pushScope(boolean blocking) {
        myCurrentScope = new Scope(myCurrentScope, blocking);
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
        throw new UnsupportedOperationException();
    }
    
    private static class Scope {
        private final Scope myRootParent;
        private final Scope myParent;
        
        private final boolean myRootFlag;
        
        private final Map<String, Value> myVariables = new HashMap<>();
        
        public Scope(Scope parent, boolean root) {
            myParent = parent;
            myRootFlag = root;
            
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
        
        public boolean isRoot() {
            return myRootFlag;
        }
        
        public Scope getRootParent() {
            return myRootParent;
        }
        
        public void putSymbol(String name, Value v) {
            if (v == null) {
                throw new IllegalArgumentException();
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
                
                if (myRootFlag) {
                    throw new Sbsdl.ExecutionException("Cannot access symbol \'"
                            + name + "\' outside of function literal.");
                }
            }
            
            return result;
        }
    }
}
