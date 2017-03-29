package com.shieldsbetter.scedel.visitors;

import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.expressions.BinaryExpression;
import com.shieldsbetter.scedel.expressions.ClosureBuildingExpression;
import com.shieldsbetter.scedel.expressions.DictionaryExpression;
import com.shieldsbetter.scedel.expressions.Expression;
import com.shieldsbetter.scedel.expressions.FunctionCallExpression;
import com.shieldsbetter.scedel.expressions.HostExpression;
import com.shieldsbetter.scedel.expressions.LiteralExpression;
import com.shieldsbetter.scedel.expressions.PickExpression;
import com.shieldsbetter.scedel.expressions.SequenceExpression;
import com.shieldsbetter.scedel.expressions.UnaryExpression;
import com.shieldsbetter.scedel.expressions.VariableNameExpression;
import com.shieldsbetter.scedel.statements.EvaluateStatement;
import com.shieldsbetter.scedel.statements.FieldAssignmentStatement;
import com.shieldsbetter.scedel.statements.ForEachStatement;
import com.shieldsbetter.scedel.statements.IfStatement;
import com.shieldsbetter.scedel.statements.MultiplexingStatement;
import com.shieldsbetter.scedel.statements.ReturnStatement;
import com.shieldsbetter.scedel.statements.SequenceAssignmentStatement;
import com.shieldsbetter.scedel.statements.Statement;
import com.shieldsbetter.scedel.statements.TopLevelVariableAssignmentStatement;
import com.shieldsbetter.scedel.statements.VariableIntroductionStatement;
import com.shieldsbetter.scedel.values.VBoolean;
import com.shieldsbetter.scedel.values.VDict;
import com.shieldsbetter.scedel.values.VFunction;
import com.shieldsbetter.scedel.values.VNone;
import com.shieldsbetter.scedel.values.VNumber;
import com.shieldsbetter.scedel.values.VProxy;
import com.shieldsbetter.scedel.values.VSeq;
import com.shieldsbetter.scedel.values.VString;
import com.shieldsbetter.scedel.values.VToken;
import com.shieldsbetter.scedel.values.VUnavailable;
import com.shieldsbetter.scedel.values.Value;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;

public class Serializer implements Statement.Visitor, Expression.Visitor {
    private static final Map<String, DeserializeAction> DESERIALIZE_ACTIONS =
            new HashMap<>();
    
    private static final Map<String, BinaryExpression.Operator> BIN_OP_KEYS =
            new HashMap<>();
    
    private static final Map<String, UnaryExpression.Operator> UN_OP_KEYS =
            new HashMap<>();
    
    static {
        for (BinaryExpression.Operator binop
                : BinaryExpression.Operator.values()) {
            BIN_OP_KEYS.put(binop.getKey(), binop);
        }
        
        for (UnaryExpression.Operator unop
                : UnaryExpression.Operator.values()) {
            UN_OP_KEYS.put(unop.getKey(), unop);
        }
        
        DESERIALIZE_ACTIONS.put("$", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i)
                            throws IOException {
                        String sourceDesc = readSourceDescription(i);
                        int lineNum = readLineNumber(i);
                        
                        s.setSourceDescription(sourceDesc);
                        s.setLineNumber(lineNum);
                    }
                });
        DESERIALIZE_ACTIONS.put("$$", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i)
                            throws IOException {
                        int lineNum = readLineNumber(i);
                        s.setLineNumber(lineNum);
                    }
                });
    }
    
    public static void serialize(Value v, PrintWriter out) throws IOException {
        v.accept(new Serializer(out));
    }
    
    public static Value deserialize(Reader r) throws IOException {
        Input i = new Input(r);
        String magicHeader = i.nextToken();
        
        if (!magicHeader.equals("scedelv1")) {
            throw new IOException("Not a valid serialization.");
        }
        
        Value result;
        try {
            result = deserialize(new Input(r),
                    new DeserializeState(Scedel.buildRandomDecider()));
        }
        catch (Throwable t) {
            throw new IOException("Error deserializing.", t);
        }
        
        return result;
    }
            
    private static Value deserialize(Input i, DeserializeState s)
            throws IOException {
        while (i.hasNextToken()) {
            String actionId = i.nextToken();
            DESERIALIZE_ACTIONS.get(actionId).execute(s, i);
        }
        
        if (s.getStackDepth() != 1) {
            throw new IOException("Not a valid serialization.");
        }
        
        return (Value) s.pop();
    }
    
    private final PrintWriter myOut;
    
    private String myCurrentSource;
    private int myCurrentLine;
    
    public Serializer(PrintWriter w) {
        myOut = w;
    }
    
    private void outToken(String tkn) {
        myOut.println(tkn + " ");
    }
    
    private void visitSymbol(Scedel.Symbol s) {
        assertLocation(s.getPosition());
        outToken("sym");
        outToken(s.getName());
    }
    
    static {
        DESERIALIZE_ACTIONS.put("sym", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i)
                            throws IOException {
                        String name = i.nextToken();
                        s.push(new Scedel.Symbol(
                                name, false, s.getParseLocation()));
                    }
                });
    }
    
    private void assertLocation(Statement s) {
        assertLocation(s.getParseLocation());
    }
    
    private void assertLocation(Expression e) {
        assertLocation(e.getParseLocation());
    }
    
    private void assertLocation(ParseLocation l) {
        if (!l.getSourceDescription().equals(myCurrentSource)) {
            myOut.print("$ ");
            escapeSourceDescription(l.getSourceDescription());
            myOut.print("$ ");
            myOut.print(l.getLineNumber());
            myOut.print("$ ");
        }
        else if (l.getLineNumber() != myCurrentLine) {
            myOut.print("$$ ");
            myOut.print(l.getLineNumber());
            myOut.print("$ ");
        }
        
        myCurrentSource = l.getSourceDescription();
        myCurrentLine = l.getLineNumber();
    }
    
    private static int readLineNumber(Input i) throws IOException {
        StringBuilder result = new StringBuilder();
        boolean done = false;
        do {
            char nextChar = i.nextChar();
            
            if (nextChar == '$') {
                done = true;
            }
            else {
                result.append(nextChar);
            }
        } while (!done);
        
        return Integer.parseInt(result.toString());
    }
    
    private static String readSourceDescription(Input i) throws IOException {
        StringBuilder result = new StringBuilder();
        boolean done = false;
        do {
            char nextChar = i.nextChar();
            
            if (nextChar == '\\') {
                if (!i.hasMoreChars()) {
                    throw new IOException("Escaped end-of-input.");
                }
                
                nextChar = i.nextChar();
                if (nextChar != '$' && nextChar != '\\') {
                    throw new IOException(
                            "Illegal escape character: " + nextChar);
                }
                
                result.append(nextChar);
            }
            else if (nextChar == '$') {
                done = true;
            }
            else {
                result.append(nextChar);
            }
        } while (!done);
        
        return result.toString().trim();
    }
    
    private void escapeSourceDescription(String s) {
        for (char c : s.toCharArray()) {
            if (c == '$') {
                myOut.print("\\$");
            }
            else {
                myOut.print(c);
            }
        }
    }
    
    @Override
    public void visitEvaluateStatement(EvaluateStatement s) {
        s.getExpression().accept(this);
        
        assertLocation(s);
        outToken("evalstmt");
    }
    
    static {
        DESERIALIZE_ACTIONS.put("evalstmt", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i) {
                        s.push(new EvaluateStatement(
                                s.getParseLocation(), (Expression )s.pop()));
                    }
                });
    }

    @Override
    public void visitFieldAssignmentStatement(FieldAssignmentStatement s) {
        s.getBase().accept(this);
        s.getField().accept(this);
        s.getAssigned().accept(this);
        
        assertLocation(s);
        outToken("assgnfield");
    }
    
    static {
        DESERIALIZE_ACTIONS.put("assgnfield", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i) {
                        Expression assigned = (Expression) s.pop();
                        Expression field = (Expression) s.pop();
                        Expression base = (Expression) s.pop();
                        
                        s.push(new FieldAssignmentStatement(
                                s.getParseLocation(), base, field, assigned));
                    }
                });
    }

    @Override
    public void visitForEachStatement(ForEachStatement s) {
        visitSymbol(s.getExemplar());
        s.getCollection().accept(this);
        s.getWhere().accept(this);
        s.getCode().accept(this);
        
        assertLocation(s);
        outToken("foreach");
    }
    
    static {
        DESERIALIZE_ACTIONS.put("foreach", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i) {
                        Statement code = (Statement) s.pop();
                        Expression where = (Expression) s.pop();
                        Expression collection = (Expression) s.pop();
                        Scedel.Symbol exemplar = (Scedel.Symbol) s.pop();
                        
                        s.push(new ForEachStatement(s.getParseLocation(),
                                exemplar, collection, where, code));
                    }
                });
    }

    @Override
    public void visitIfStatement(IfStatement s) {
        s.getCondition().accept(this);
        s.getOnTrue().accept(this);
        s.getOnFalse().accept(this);
        
        assertLocation(s);
        outToken("ifelse");
    }
    
    static {
        DESERIALIZE_ACTIONS.put("ifelse", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i) {
                        Statement onFalse = (Statement) s.pop();
                        Statement onTrue = (Statement) s.pop();
                        Expression condition = (Expression) s.pop();
                        
                        s.push(new IfStatement(s.getParseLocation(),
                                condition, onTrue, onFalse));
                    }
                });
    }

    @Override
    public void visitMultiplexingStatement(MultiplexingStatement s) {
        for (Statement sub : s.getSubStatements()) {
            sub.accept(this);
        }
        
        assertLocation(s);
        outToken("mux");
        outToken("" + s.getSubStatements().size());
    }
    
    static {
        DESERIALIZE_ACTIONS.put("mux", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i)
                            throws IOException {
                        int stmtCt = Integer.parseInt(i.nextToken());
                        
                        LinkedList<Statement> subStatements = new LinkedList();
                        for (int j = 0; j < stmtCt; j++) {
                            subStatements.addFirst((Statement) s.pop());
                        }
                        
                        s.push(new MultiplexingStatement(
                                s.getParseLocation(), subStatements));
                    }
               });
    }

    @Override
    public void visitReturnStatement(ReturnStatement s) {
        s.getExpression().accept(this);
        
        assertLocation(s);
        outToken("return");
    }
    
    static {
        DESERIALIZE_ACTIONS.put("return", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i) {
                        s.push(new ReturnStatement(
                                s.getParseLocation(), (Expression) s.pop()));
                    }
                });
    }

    @Override
    public void visitSequenceAssignmentStatement(
            SequenceAssignmentStatement s) {
        s.getBase().accept(this);
        s.getIndex().accept(this);
        s.getAssigned().accept(this);
        
        assertLocation(s);
        outToken("assgnindex");
    }
    
    static {
        DESERIALIZE_ACTIONS.put("assgnindex", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i) {
                        Expression assigned = (Expression) s.pop();
                        Expression index = (Expression) s.pop();
                        Expression base = (Expression) s.pop();
                        
                        s.push(new SequenceAssignmentStatement(
                                s.getParseLocation(), base, index, assigned));
                    }
                });
    }

    @Override
    public void visitNoOpStatement(Statement s) {
        assertLocation(s);
        outToken("noop");
    }
    
    static {
        DESERIALIZE_ACTIONS.put("noop", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i) {
                        s.push(Statement.NO_OP);
                    }
                });
    }

    @Override
    public void visitTopLevelVariableAssignmentStatement(
            TopLevelVariableAssignmentStatement s) {
        visitSymbol(s.getSymbol());
        s.getAssigned().accept(this);
        
        assertLocation(s);
        outToken("assgnvar");
    }
    
    static {
        DESERIALIZE_ACTIONS.put("assgnvar", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i) {
                        Expression assigned = (Expression) s.pop();
                        Scedel.Symbol symbol = (Scedel.Symbol) s.pop();
                        
                        s.push(new TopLevelVariableAssignmentStatement(
                                s.getParseLocation(), symbol, assigned));
                    }
                });
    }

    @Override
    public void visitVariableIntroductionStatement(
            VariableIntroductionStatement s) {
        visitSymbol(s.getSymbol());
        s.getInitialValue().accept(this);
        
        assertLocation(s);
        outToken("var");
    }
    
    static {
        DESERIALIZE_ACTIONS.put("var", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i) {
                        Expression initial = (Expression) s.pop();
                        Scedel.Symbol symbol = (Scedel.Symbol) s.pop();
                        
                        s.push(new VariableIntroductionStatement(
                                s.getParseLocation(), symbol, initial));
                    }
                });
    }

    @Override
    public void visitBinaryExpression(BinaryExpression e) {
        e.getOperand1().accept(this);
        e.getOperand2().accept(this);
        
        assertLocation(e);
        outToken("binop");
        outToken(e.getOperator().getKey());
    }
    
    static {
        DESERIALIZE_ACTIONS.put("binop", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i)
                            throws IOException {
                        BinaryExpression.Operator op =
                                BIN_OP_KEYS.get(i.nextToken());
                        
                        Expression operand2 = (Expression) s.pop();
                        Expression operand1 = (Expression) s.pop();
                        
                        s.push(new BinaryExpression(s.getParseLocation(),
                                operand1, op, operand2));
                    }
                });
    }

    @Override
    public void visitClosureBindingExpression(ClosureBuildingExpression e) {
        for (Scedel.Symbol arg : e.getArgumentNames()) {
            visitSymbol(arg);
        }
        e.getCode().accept(this);
        
        assertLocation(e);
        outToken("fn");
        outToken("" + e.getArgumentNames().size());
    }

    static {
        DESERIALIZE_ACTIONS.put("fn", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i)
                            throws IOException {
                        int argCt = Integer.parseInt(i.nextToken());
                        
                        MultiplexingStatement code =
                                (MultiplexingStatement) s.pop();
                        
                        LinkedList<Scedel.Symbol> args = new LinkedList<>();
                        for (int j = 0; j < argCt; j++) {
                            args.addFirst((Scedel.Symbol) s.pop());
                        }
                        
                        s.push(new ClosureBuildingExpression(
                                s.getParseLocation(), args, code));
                    }
                });
    }
    
    @Override
    public void visitDictionaryExpression(DictionaryExpression e) {
        for (Map.Entry<Expression, Expression> entry : e.entries()) {
            entry.getKey().accept(this);
            entry.getValue().accept(this);
        }
        
        assertLocation(e);
        outToken("dictexp");
        outToken("" + e.size());
    }
    
    static {
        DESERIALIZE_ACTIONS.put("dictexp", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i)
                            throws IOException {
                        int entryCt = Integer.parseInt(i.nextToken());
                        
                        Map<Expression, Expression> entries =
                                new HashMap<>();
                        for (int j = 0; j < entryCt; j++) {
                            Expression value = (Expression) s.pop();
                            Expression key = (Expression) s.pop();
                            
                            entries.put(key, value);
                        }
                        
                        s.push(new DictionaryExpression(s.getParseLocation(),
                                entries));
                    }
                });
    }

    @Override
    public void visitFunctionCallExpression(FunctionCallExpression e) {
        e.getFunction().accept(this);
        for (Expression p : e.getParameters()) {
            p.accept(this);
        }
        
        assertLocation(e);
        outToken("fncall");
        outToken("" + e.getParameters().size());
    }

    static {
        DESERIALIZE_ACTIONS.put("fncall", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i)
                            throws IOException {
                        int paramCt = Integer.parseInt(i.nextToken());
                        
                        LinkedList<Expression> params = new LinkedList<>();
                        for (int j = 0; j < paramCt; j++) {
                            params.addFirst((Expression) s.pop());
                        }
                        
                        Expression fun = (Expression) s.pop();
                        
                        s.push(new FunctionCallExpression(
                                s.getParseLocation(), fun, params));
                    }
                });
    }
    
    @Override
    public void visitHostExpression(HostExpression e) {
        if (e.getParameters() != null) {
            for (Expression p : e.getParameters()) {
                p.accept(this);
            }
        }
        
        assertLocation(e);
        outToken("hostexp");
        outToken(e.getId());
        if (e.getParameters() == null) {
            outToken("noparams");
        }
        else {
            outToken("" + e.getParameters().size());
        }
    }
    
    static {
        DESERIALIZE_ACTIONS.put("hostexp", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i)
                            throws IOException {
                        String id = i.nextToken();
                        String tokenCtToken = i.nextToken();
                        
                        LinkedList<Expression> params;
                        if (tokenCtToken.equals("noparams")) {
                            params = null;
                        }
                        else {
                            params = new LinkedList<>();
                            int tokenCt = Integer.parseInt(tokenCtToken);
                            for (int j = 0; j < tokenCt; j++) {
                                params.addFirst((Expression) s.pop());
                            }
                        }
                        
                        s.push(new HostExpression(
                                s.getParseLocation(), id, params));
                    }
                });
    }

    @Override
    public void visitLiteralExpression(LiteralExpression e) {
        e.getValue().accept(this);
        
        assertLocation(e);
        outToken("lit");
    }
    
    static {
        DESERIALIZE_ACTIONS.put("lit", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i) {
                        s.push(new LiteralExpression(
                                s.getParseLocation(), (Value) s.pop()));
                    }
                });
    }

    @Override
    public void visitPickExpression(PickExpression e) {
        visitSymbol(e.getExamplar());
        e.getCount().accept(this);
        e.getUnique().accept(this);
        e.getCollection().accept(this);
        e.getWhere().accept(this);
        e.getWeighter().accept(this);
        
        assertLocation(e);
        outToken("pick");
    }
    
    static {
        DESERIALIZE_ACTIONS.put("pick", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i) {
                        Expression weighter = (Expression) s.pop();
                        Expression where = (Expression) s.pop();
                        Expression collection = (Expression) s.pop();
                        Expression unique = (Expression) s.pop();
                        Expression count = (Expression) s.pop();
                        Scedel.Symbol exemplar = (Scedel.Symbol) s.pop();
                        
                        s.push(new PickExpression(s.getParseLocation(),
                                exemplar, collection, count, unique, weighter,
                                where, s.getDecider()));
                    }
                });
    }           

    @Override
    public void visitSequenceExpression(SequenceExpression e) {
        for (Expression el : e.getElements()) {
            el.accept(this);
        }
        
        assertLocation(e);
        outToken("seqexp");
        outToken("" + e.getElements().size());
    }
    
    static {
        DESERIALIZE_ACTIONS.put("seqexp", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i)
                            throws IOException {
                        int elCt = Integer.parseInt(i.nextToken());
                        
                        LinkedList<Expression> elements = new LinkedList<>();
                        for (int j = 0; j < elCt; j++) {
                            elements.addFirst((Expression) s.pop());
                        }
                        
                        s.push(new SequenceExpression(
                                s.getParseLocation(), elements));
                    }
                });
    }

    @Override
    public void visitUnaryExpression(UnaryExpression e) {
        e.getOperand().accept(this);
        
        assertLocation(e);
        outToken("unop");
        outToken(e.getOperator().getKey());
    }
    
    static {
        DESERIALIZE_ACTIONS.put("unop", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i)
                            throws IOException {
                        UnaryExpression.Operator op =
                                UN_OP_KEYS.get(i.nextToken());
                        
                        s.push(new UnaryExpression(s.getParseLocation(),
                                op, (Expression) s.pop()));
                    }
                });
    }

    @Override
    public void visitVariableNameExpression(VariableNameExpression e) {
        assertLocation(e);
        outToken("lookup");
        outToken(e.getSymbol().getName());
    }
    
    static {
        DESERIALIZE_ACTIONS.put("lookup", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i) {
                        Scedel.Symbol var = (Scedel.Symbol) s.pop();
                        
                        s.push(new VariableNameExpression(
                                s.getParseLocation(), var));
                    }
                });
    }

    @Override
    public void visitVBoolean(VBoolean v) {
        outToken("" + v.getValue());
    }
    
    static {
        DESERIALIZE_ACTIONS.put("true", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i) {
                        s.push(VBoolean.TRUE);
                    }
                });
        
        DESERIALIZE_ACTIONS.put("false", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i) {
                        s.push(VBoolean.FALSE);
                    }
                });
    }

    @Override
    public void visitVDict(VDict v) {
        for (Map.Entry<Value, Value> e : v.entries()) {
            e.getKey().accept(this);
            e.getValue().accept(this);
        }
        outToken("dict");
        outToken("" + v.size());
    }
    
    static {
        DESERIALIZE_ACTIONS.put("dict", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i)
                            throws IOException {
                        int entryCt = Integer.parseInt(i.nextToken());
                        VDict d = new VDict();
                        for (int j = 0; j < entryCt; j++) {
                            Value value = (Value) s.pop();
                            Value key = (Value) s.pop();
                            
                            d.put(key, value);
                        }
                        
                        s.push(d);
                    }
                });
    }
    

    @Override
    public void visitVFunction(VFunction v) {
        for (Map.Entry<Scedel.Symbol, Value> baked : v.getBakedValues()) {
            visitSymbol(baked.getKey());
            baked.getValue().accept(this);
        }
        
        for (Scedel.Symbol a : v.getArgumentNames()) {    
            visitSymbol(a);
        }
        
        v.getCode().accept(this);
        
        outToken("closure");
        outToken("" + v.getBakedValueCount());
        outToken("" + v.getArgumentNames());
    }
    
    static {
        DESERIALIZE_ACTIONS.put("closure", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i)
                            throws IOException {
                        int bakedValCt = Integer.parseInt(i.nextToken());
                        int argCt = Integer.parseInt(i.nextToken());
                        
                        MultiplexingStatement code =
                                (MultiplexingStatement) s.pop();
                        
                        LinkedList<Scedel.Symbol> args = new LinkedList<>();
                        for (int j = 0; j < argCt; j++) {
                            args.addFirst((Scedel.Symbol) s.pop());
                        }
                        
                        Map<Scedel.Symbol, Value> baked = new HashMap<>();
                        for (int j = 0; j < bakedValCt; j++) {
                            Value value = (Value) s.pop();
                            Scedel.Symbol sym = (Scedel.Symbol) s.pop();
                            
                            baked.put(sym, value);
                        }
                        
                        s.push(new VFunction(args, code, baked));
                    }
                });
    }

    @Override
    public void visitVNone(VNone v) {
        outToken("none");
    }
    
    static {
        DESERIALIZE_ACTIONS.put("none", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i)
                            throws IOException {
                        s.push(VNone.INSTANCE);
                    }
                });
    }

    @Override
    public void visitVNumber(VNumber v) {
        outToken("num");
        outBI(v.getNumerator());
        outBI(v.getDenominator());
    }
    
    static {
        DESERIALIZE_ACTIONS.put("num", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i)
                            throws IOException {
                        long n = Long.parseLong(i.nextToken());
                        long d = Long.parseLong(i.nextToken());
                        
                        s.push(VNumber.of(n, d));
                    }
        });
    }
    
    private void outBI(BigInteger i) {
        if (i.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
            throw new RuntimeException(
                    "Can't serialize a number this large.");
        }
        
        if (i.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0) {
            throw new RuntimeException(
                    "Can't serialize a number this small.");
        }
        
        long val = i.longValue();
        
        outToken("" + val);
    }
    
    @Override
    public void visitVProxy(VProxy v) {
        throw new IllegalArgumentException("Cannot serialize proxy object.");
    }

    @Override
    public void visitVSeq(VSeq v) {
        for (Value e : v.elements()) {
            e.accept(this);
        }
        
        outToken("seq");
        outToken("" + v.getElementCount());
    }
    
    static {
        DESERIALIZE_ACTIONS.put("seq", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i)
                            throws IOException {
                        int elCt = Integer.parseInt(i.nextToken());
                        
                        LinkedList<Value> elements = new LinkedList<>();
                        for (int j = 0; j < elCt; j++) {
                            elements.addFirst((Value) s.pop());
                        }
                        
                        s.push(new VSeq(null, elements));
                    }
                });
    }

    @Override
    public void visitVString(VString v) {
        outToken("str");
        outToken("'" + escapeForStringLiteral(v.getValue()) + "'");
    }
    
    static {
        DESERIALIZE_ACTIONS.put("str", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i)
                            throws IOException {
                        String contents = readStringLiteral(i);
                        
                        s.push(new VString(contents));
                    }
                });
    }
    
    @Override
    public void visitVToken(VToken v) {
        outToken("tkn");
        outToken("'" + escapeForStringLiteral(v.getBackingKey()) + "'");
    }
    
    static {
        DESERIALIZE_ACTIONS.put("tkn", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i)
                            throws IOException {
                        String contents = readStringLiteral(i);
                        
                        s.push(new VToken(contents));
                    }
                });
    }

    @Override
    public void visitVUnavailable(VUnavailable v) {
        outToken("unavailable");
    }
    
    static {
        DESERIALIZE_ACTIONS.put("str", new DeserializeAction() {
                    @Override
                    public void execute(DeserializeState s, Input i) {
                        s.push(VUnavailable.INSTANCE);
                    }
                });
    }
    
    private static String escapeForStringLiteral(String s) {
        StringBuilder result = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '\'') {
                result.append("\\'");
            }
            else if (c == '\\') {
                result.append("\\\\");
            }
            else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    private static String readStringLiteral(Input i) throws IOException {
        while (i.peekNextChar() == ' ') {
            i.nextChar();
        }
        
        if (i.peekNextChar() != '\'') {
            throw new RuntimeException();
        }
        
        i.nextChar();
        
        StringBuilder result = new StringBuilder();
        boolean working = true;
        do {
            char nc = i.nextChar();
            
            if (nc == '\\') {
                nc = i.nextChar();
                
                if (nc != '\'' && nc != '\\') {
                    throw new RuntimeException("Illegal excape char: " + nc);
                }
                
                result.append(nc);
            }
            else if (nc == '\'') {
                working = false;
            }
            else {
                result.append(nc);
            }
        } while (working);
        
        return result.toString();
    }
    
    private static BufferedReader toBufferedReader(Reader r) {
        BufferedReader result;
        if (r instanceof BufferedReader) {
            result = (BufferedReader) r;
        }
        else {
            result = new BufferedReader(r);
        }
        
        return result;
    }
    
    private static interface DeserializeAction {
        public void execute(DeserializeState s, Input i)
                throws IOException;
    }
    
    private static final class DeserializeState {
        private final Scedel.Decider myDecider;
        private final Deque myStack = new ArrayDeque<>();
        private String mySourceDesription;
        private int myLineNumber;
        
        public DeserializeState(Scedel.Decider d) {
            myDecider = d;
        }
        
        public Scedel.Decider getDecider() {
            return myDecider;
        }
        
        public int getStackDepth() {
            return myStack.size();
        }
        
        public void push(Object o) {
            myStack.push(o);
        }
        
        public Object pop() {
            return myStack.pop();
        }
        
        public void setSourceDescription(String d) {
            mySourceDesription = d;
        }
        
        public void setLineNumber(int l) {
            myLineNumber = l;
        }
        
        public ParseLocation getParseLocation() {
            return new ParseLocation(mySourceDesription, myLineNumber);
        }
    }
    
    /**
     * <p>{@code Reader} can't find a delimeter for us and {@code Scanner}
     * can't read a next char, and the two can't be used together due to an
     * unsatisfactorily vague specification of {@code Scanner}'s interaction
     * with its underlying {@code Reader} (specifically: (1) is {@code Scanner}
     * robust against interleaved usage of its underlying {@code Reader}?,
     * (2) in what state is the underlying {@code Reader} left with regard to
     * delimeters, i.e. are they consumed or not? and (3) in what state is it
     * left by {@code hasNext()}?)</p>
     * 
     * <p>In light of this and our straightforward needs, we just roll our
     * own delimeter-finder on top of {@code Reader}, which no doubt we will
     * live to regret.</p>
     */
    private static final class Input {
        private final BufferedReader myReader;
        private int myNextChar;
        
        public Input(Reader r) {
            myReader = toBufferedReader(r);
        }
        
        public boolean hasNextToken() throws IOException {
            queueNextToken();
            return myNextChar != -1;
        }
        
        public boolean hasMoreChars() {
            return myNextChar != -1;
        }
        
        public char peekNextChar() throws IOException {
            if (myNextChar == -1) {
                throw new NoSuchElementException();
            }
            
            return (char) myNextChar;
        }
        
        public char nextChar() throws IOException {
            char result = peekNextChar();
            myNextChar = myReader.read();
            return result;
        }
        
        public String nextToken() throws IOException {
            queueNextToken();
            if (myNextChar == -1) {
                throw new NoSuchElementException();
            }
            
            StringBuilder result = new StringBuilder();
            do {
                result.append((char) myNextChar);
                myNextChar = myReader.read();
            } while (myNextChar != -1 && myNextChar != ' ');
            
            return result.toString();
        }
        
        private void queueNextToken() throws IOException {
            while (myNextChar != -1 && myNextChar == ' ') {
                myNextChar = myReader.read();
            }
        }
    }
}
