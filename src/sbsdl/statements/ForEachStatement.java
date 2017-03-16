package sbsdl.statements;

import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;
import sbsdl.values.VSeq;
import sbsdl.values.Value;

public class ForEachStatement implements Statement {
    private final String myExemplar;
    private final Expression myCollection;
    private final Expression myWhere;
    private final Statement myCode;
    
    public ForEachStatement(String exemplar, Expression collection,
            Expression where, Statement code) {
        myExemplar = exemplar;
        myCollection = collection;
        myWhere = where;
        myCode = code;
    }
    
    @Override
    public void execute(Sbsdl.HostEnvironment h, ScriptEnvironment s) {
        VSeq pool = myCollection.evaluate(h, s).assertIsSeq();
        for (Value v : pool.elements()) {
            s.pushScope(false);
            s.putSymbol(myExemplar, v);
            
            boolean include =
                    myWhere.evaluate(h, s).assertIsBoolean().getValue();
            if (include) {
                myCode.execute(h, s);
            }
        }
        
        s.popScope();
    }
}
