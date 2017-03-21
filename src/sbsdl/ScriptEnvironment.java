package sbsdl;

import java.util.HashMap;
import java.util.Map;
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
    
    public void introduceSymbol(Sbsdl.Symbol name, Value v) {
        myCurrentScope.introduceSymbol(name, v);
    }
    
    public void assignValue(Sbsdl.Symbol name, Value v) {
        myCurrentScope.assignValue(name, v);
    }
    
    public void popScope() {
        myCurrentScope = myCurrentScope.getParentScope();
    }
    
    public Value lookupVariable(Sbsdl.Symbol name) {
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
        
        private final Map<Sbsdl.Symbol, Value> myVariables = new HashMap<>();
        
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
        
        public void introduceSymbol(Sbsdl.Symbol name, Value v) {
            if (myVariables.containsKey(name)) {
                throw new RuntimeException();
            }
            
            myVariables.put(name, v);
        }
        
        public void assignValue(Sbsdl.Symbol name, Value v) {
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
        
        public Value lookupVariable(Sbsdl.Symbol name) {
            Value result;
            
            result = myVariables.get(name);
            if (result == null) {
                if (myParent == null) {
                    throw new Sbsdl.ExecutionException(
                            "Variable not found: " + name);
                }
                
                result = myParent.lookupVariable(name);
            }
            
            return result;
        }
    }
}
