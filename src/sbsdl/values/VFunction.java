package sbsdl.values;

import java.util.Iterator;
import java.util.List;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;

public class VFunction extends SkeletonValue {
    private final List<String> myArgumentNames;
    
    public VFunction(List<String> argumentNames) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void stackCallScope(ScriptEnvironment s, List<Value> parameters) {
        s.pushScope(true);
        if (myArgumentNames.size() != parameters.size()) {
            throw new Sbsdl.ExecutionException(
                    "Incorrect number of parameters.  Expected "
                            + myArgumentNames.size() + " got "
                            + parameters.size() + ".");
        }
        
        Iterator<Value> paramIter = parameters.iterator();
        for (String arg : myArgumentNames) {
            s.putSymbol(arg, paramIter.next());
        }
    }

    @Override
    public VFunction assertIsFunction() {
        return this;
    }
}
