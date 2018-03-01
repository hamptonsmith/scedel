package com.shieldsbetter.scedel;

import com.shieldsbetter.scedel.expressions.Expression;
import com.shieldsbetter.scedel.expressions.FunctionCallExpression;
import com.shieldsbetter.scedel.statements.ReturnStatement;
import java.io.PrintWriter;
import java.util.List;
import com.shieldsbetter.scedel.statements.Statement;
import com.shieldsbetter.scedel.values.VFunction;
import com.shieldsbetter.scedel.values.Value;
import java.util.ArrayList;

public class Scedel {
    public static Decider buildRandomDecider() {
        return new Decider() {
                    @Override
                    public boolean decide(double chance) {
                        return Math.random() < chance;
                    }
                };
    }
    
    private final HostEnvironment myHostEnvironment;
    
    private final Decider myDecider;
    
    public Scedel(HostEnvironment e) {
        this(e, buildRandomDecider());
    }
    
    public Scedel(HostEnvironment e, Decider d) {
        myHostEnvironment = e;
        myDecider = d;
    }
    
    public void run(CompiledCode c) throws ExecutionException {
        runWithReturn(c);
    }
    
    private Value runWithReturn(CompiledCode c) throws ExecutionException {
        Statement parsedInput = c.myStatement;
        
        ScriptEnvironment s = new ScriptEnvironment(myDecider);
        
        try {
            parsedInput.execute(myHostEnvironment, s);
        }
        catch (InternalExecutionException iee) {
            throw iee.getExecutionException();
        }
        
        return s.getReturn();
    }
    
    public void run(String input)
            throws StaticCodeException, ExecutionException {
        run(parseCode(input));
    }
    
    public void run(String sourceDescription, String input)
            throws StaticCodeException, ExecutionException {
        run(parseCode(sourceDescription, input));
    }
    
    public Value run(VFunction f, List<Value> params)
            throws ExecutionException {
        List<Expression> copiedParams = new ArrayList<>(params.size());
        for (Value v : params) {
            copiedParams.add(v.copy(false));
        }
        
        return runWithReturn(new CompiledCode(
                new ReturnStatement(f.getParseLocation(),
                        new FunctionCallExpression(
                                f.getParseLocation(), f, copiedParams))));
    }
    
    public Value evaluate(String expression)
            throws StaticCodeException, ExecutionException {
        StackTraceElement e = new RuntimeException().getStackTrace()[1];
        String sourceDesc = "string from " + e.getFileName() + ", line "
                + e.getLineNumber();
        
        return evaluate(sourceDesc, expression);
    }
    
    public Value evaluate(String sourceDescription, String expression)
            throws StaticCodeException, ExecutionException {
        Expression parsedExpression =
                Compiler.parseExpression(sourceDescription, expression);
        
        return evaluate(parsedExpression);
    }
    
    public Value evaluate(Expression e) throws ExecutionException {
        ScriptEnvironment s = new ScriptEnvironment(myDecider);
        
        Value result;
        try {
            result = e.evaluate(myHostEnvironment, s);
        }
        catch (InternalExecutionException iee) {
            throw iee.getExecutionException();
        }
        
        return result;
    }
    
    public static CompiledCode parseCode(String input)
            throws StaticCodeException {
        StackTraceElement e = new RuntimeException().getStackTrace()[1];
        String sourceDesc = "string from " + e.getFileName() + ", line "
                + e.getLineNumber();
        
        return parseCode(sourceDesc, input);
    }
    
    public static CompiledCode parseCode(
            String sourceDescription, String input) throws StaticCodeException {
        return Compiler.parseCode(sourceDescription, input);
    }
    
    public static Expression parseExpression(String input)
            throws StaticCodeException {
        StackTraceElement e = new RuntimeException().getStackTrace()[1];
        String sourceDesc = "string from " + e.getFileName() + ", line "
                + e.getLineNumber();
        
        return parseExpression(sourceDesc, input);
    }
    
    public static Expression parseExpression(
            String sourceDescription, String input) throws StaticCodeException {
        return Compiler.parseExpression(sourceDescription, input);
    }
    
    public static String createFunctionCode(
            String innerCode, String ... argNames) {
        String argString = "(";
        boolean comma = false;
        for (String arg : argNames) {
            if (comma) {
                argString += ", ";
            }
            else {
                comma = true;
            }
            
            argString += arg;
        }
        argString += ")";
        
        return "fn" + argString + "{ " + innerCode + "}";
    }
    
    public static VFunction createFunction(
            String innerCode, String ... argNames)
            throws StaticCodeException {
        Scedel s = new Scedel(new HostEnvironment() {
                    @Override
                    public Value evaluate(String name, List<Value> parameters)
                            throws HostEnvironmentException {
                        throw new UnsupportedOperationException();
                    }
                });
        
        VFunction fnVal;
        try {
            fnVal = (VFunction) s.evaluate(
                    parseExpression(createFunctionCode(innerCode, argNames)));
        }
        catch (ExecutionException ee) {
            throw new RuntimeException(ee);
        }
        
        return fnVal;
    }
    
    public static class HostEnvironmentException extends Exception {
        public HostEnvironmentException(String msg) {
            super(msg);
        }
    }
    
    public static interface HostEnvironment {
        /**
         * <p>Called to evaluate host expressions like {@code #foo} or
         * {@code #bar(5)}.</p>
         * 
         * <p>{@code name} will contain the identifier after the
         * hash mark ({@code foo} or {@code bar} in the previous examples).</p>
         * 
         * <p>If the host expression has a parameter list (including the case
         * where it has an empty parameter list), then {@code parameters} will
         * contain the evaluated value of each of those parameters in order (or
         * be the empty list in the case where the parameter list is empty.)  If
         * the host expression does not have a parameter list,
         * {@code parameters} will be {@code null}.  {@code #bazz} can thus be
         * distinguished from {@code #bazz()} by whether or not
         * {@code parameters} is {@code null}.</p>
         * 
         * @param name The text of the identifier following the hash mark.
         * @param parameters The list of evaluated parameters provided in the
         *              parameter list, or {@code null} if no parameter list is
         *              present.
         * @return The evaluated value of the host expression.  May not return
         *         {@code null}.
         * 
         * @throws com.shieldsbetter.scedel.Scedel.HostEnvironmentException If
         *               for any reason the given host expression cannot be
         *               evaluated into a {@link Value}.
         */
        public Value evaluate(String name, List<Value> parameters)
                throws HostEnvironmentException;
    }
    
    public static final class Symbol {
        private final String myName;
        private final boolean myBakedFlag;
        private final ParseLocation myPosition;
        
        public Symbol(String name, boolean baked, ParseLocation p) {
            myName = name;
            myBakedFlag = baked;
            myPosition = p;
        }
        
        public Symbol(String name) {
            this(name, false, ParseLocation.INTERNAL);
        }
        
        public boolean isEquivalentTo(Symbol other) {
            return myName.equals(other.getName())
                    && myBakedFlag == other.isBaked();
        }
        
        public String getName() {
            return myName;
        }
        
        public int getLineNumber() {
            return myPosition.getLineNumber();
        }
        
        public int getColumn() {
            return myPosition.getColumnNumber();
        }
        
        public String getLineContents() {
            return myPosition.getLineContents();
        }
        
        public String getIndent() {
            return myPosition.getAlignmentPrefix();
        }
        
        public ParseLocation getPosition() {
            return myPosition;
        }
        
        public boolean isBaked() {
            return myBakedFlag;
        }
        
        @Override
        public String toString() {
            String result =  myName + " (" + myPosition.getLineNumber() + ":";
            
            if (myPosition.hasColumnNumber()) {
                result += myPosition.getColumnNumber();
            }
            
            result += ") " + (myBakedFlag ? "bake" : "intro");
            
            return result;
        }
    }
    
    public static final class ParseException extends Exception {
        private final int myLineNumber;
        private final int myColumn;
        
        private final String myLineContents;
        private final String myAlignmentPrefix;
        
        public ParseException(int lineNumber, int column, String lineContents,
                String alignmentPrefix, String message) {
            super(message);
            
            myLineNumber = lineNumber;
            myColumn = column;
            myLineContents = lineContents;
            myAlignmentPrefix = alignmentPrefix;
        }
        
        public int getLineNumber() {
            return myLineNumber;
        }
        
        public int getColumn() {
            return myColumn;
        }
        
        public String getLineContents() {
            return myLineContents;
        }
        
        public String getAlignmentPrefix() {
            return myAlignmentPrefix;
        }
        
        public void print(PrintWriter w) {
            w.println(getMessage());
            w.println();
            w.println("Line " + myLineNumber + ":");
            w.println(myLineContents);
            w.println(myAlignmentPrefix + "^");
        }
    }
    
    public static interface Decider {
        public boolean decide(double chance);
    }
    
    public static final class CompiledCode {
        final Statement myStatement;
        
        CompiledCode(Statement s) {
            myStatement = s;
        }
    }
}
