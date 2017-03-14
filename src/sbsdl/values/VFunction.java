package sbsdl.values;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.statements.Statement;

public class VFunction extends SkeletonValue {
    private final List<String> myArgumentNames;
    private final Statement myCode;
    
    public VFunction(List<String> argumentNames, Statement code) {
        myArgumentNames = new ArrayList<>(argumentNames);
        myCode = code;
    }
    
    public void call(Sbsdl.HostEnvironment h, ScriptEnvironment s,
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
        
        s.popScope();
    }
    
    @Override
    public VFunction assertIsFunction() {
        return this;
    }
}
