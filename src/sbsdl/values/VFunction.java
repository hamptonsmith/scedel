package sbsdl.values;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.statements.MultiplexingStatement;

public class VFunction extends SkeletonValue {
    private final List<String> myArgumentNames;
    private final MultiplexingStatement myCode;
    
    public VFunction(List<String> argumentNames, MultiplexingStatement code) {
        myArgumentNames = new ArrayList<>(argumentNames);
        myCode = code;
    }
    
    public Value call(Sbsdl.HostEnvironment h, ScriptEnvironment s,
            List<Value> parameters) {
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
        
        myCode.execute(h, s);
        
        Value returnValue = s.getReturn();
        if (returnValue == null) {
            returnValue = VUnavailable.INSTANCE;
        }
        
        s.popScope();
        
        return returnValue;
    }
    
    @Override
    public VFunction assertIsFunction() {
        return this;
    }
}
