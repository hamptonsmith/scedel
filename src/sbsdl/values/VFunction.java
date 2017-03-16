package sbsdl.values;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.statements.MultiplexingStatement;
import sbsdl.statements.ReturnStatement;
import sbsdl.statements.Statement;

public class VFunction extends SkeletonValue {
    public static VFunction buildConstantFunction(int argCount, Value result) {
        List<String> args = new ArrayList<>(argCount);
        for (int i = 0; i < argCount; i++) {
            args.add("a" + i);
        }
        
        List<Statement> code = new ArrayList<>(1);
        code.add(new ReturnStatement(result));
        
        return new VFunction(args, new MultiplexingStatement(code));
    }
    
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

    @Override
    public Value copy() {
        return this;
    }
}
