package sbsdl.statements;

import sbsdl.Sbsdl;
import sbsdl.ScriptEnvironment;
import sbsdl.expressions.Expression;
import sbsdl.values.VSeq;
import sbsdl.values.Value;

public class ForEachStatement implements Statement {
    private final Sbsdl.Symbol myExemplar;
    private final Expression myCollection;
    private final Expression myWhere;
    private final Statement myCode;
    
    public ForEachStatement(Sbsdl.Symbol exemplar, Expression collection,
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
            s.introduceSymbol(myExemplar, v.copy(null));
            
            boolean include =
                    myWhere.evaluate(h, s).assertIsBoolean().getValue();
            if (include) {
                myCode.execute(h, s);
            }
            
            s.popScope();
        }
    }

    @Override
    public void prettyRender(int indentUnit, int indentLevels, StringBuilder b) {
        b.append("STMT FOR EACH\n");
        Util.indent(indentUnit, indentLevels + 1, b);
        b.append("exemplar: ");
        b.append(myExemplar);
        b.append("\n");
        
        Util.labeledChild(indentUnit, indentLevels, "collection expression:",
                myCollection, b);
        Util.labeledChild(
                indentUnit, indentLevels, "where expression:", myWhere, b);
        Util.labeledChild(indentUnit, indentLevels, "code:", myCode, b);
    }
}
