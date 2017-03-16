package sbsdl;

import com.shieldsbetter.flexcompilator.WellFormednessException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import sbsdl.values.VBoolean;
import sbsdl.values.VDict;
import sbsdl.values.VNone;
import sbsdl.values.VNumber;
import sbsdl.values.VSeq;
import sbsdl.values.VString;
import sbsdl.values.VUnavailable;
import sbsdl.values.Value;

public class SbsdlTest {
    @Test
    public void evaluationTests()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        List<EvaluationTest> tests = Arrays.asList(new EvaluationTest[] {
            new EvaluationTest(
                    "unavailable", "unavailable", VUnavailable.INSTANCE),
            new EvaluationTest("none", "none", VNone.INSTANCE),
            new EvaluationTest("basic true", "true", VBoolean.TRUE),
            new EvaluationTest("basic false", "false", VBoolean.FALSE),
            new EvaluationTest("and1", "true and true", VBoolean.TRUE),
            new EvaluationTest("and2", "true and false", VBoolean.FALSE),
            new EvaluationTest("and3", "false and true", VBoolean.FALSE),
            new EvaluationTest("and4", "false and false", VBoolean.FALSE),
            new EvaluationTest("or1", "true or true", VBoolean.TRUE),
            new EvaluationTest("or2", "true or false", VBoolean.TRUE),
            new EvaluationTest("or3", "false or true", VBoolean.TRUE),
            new EvaluationTest("or4", "false or false", VBoolean.FALSE),
            new EvaluationTest("not1", "not true", VBoolean.FALSE),
            new EvaluationTest("not2", "not false", VBoolean.TRUE),
            new EvaluationTest(
                    "basic number", "123.457", VNumber.of(123457, 1000)),
            new EvaluationTest("basic string", "'hello \\'quoted'",
                    new VString("hello 'quoted")),
            new EvaluationTest("basic sequence", "[3.1, 'foo', []]",
                    new VSeq(VNumber.of(31, 10), new VString("foo"),
                            new VSeq())),
            new EvaluationTest("empty sequence", "[]", new VSeq()),
            new EvaluationTest(
                    "singleton sequence", "[1]", new VSeq(VNumber.of(1, 1))),
            new EvaluationTest("basic function", "fn(x) { return x; }(123)",
                    VNumber.of(123, 1)),
            new EvaluationTest("basic pick", "pick 2 unique from "
                    + "{ 'foo' {1}, 'bar' {5}, 'bazz' {5} }",
                    new VSeq(new VString("foo"), new VString("bazz")),
                    true, false, false, false, true),
            new EvaluationTest("basic pick implicit weight",
                    "pick 2 unique from { 'foo', 'bar', 'bazz' }",
                    new VSeq(new VString("foo"), new VString("bazz")),
                    true, false, false, false, true),
            new EvaluationTest("basic sequence pick",
                    "pick 2 unique from [ 'foo', 'bar', 'bazz' ]",
                    new VSeq(new VString("foo"), new VString("bazz")),
                    true, false, false, false, true),
            new EvaluationTest("basic dictionary", "{foo: 5, bar: 2 + 4}",
                    new VDict().put(new VString("foo"), VNumber.of(5, 1))
                            .put(new VString("bar"), VNumber.of(6, 1))),
            new EvaluationTest("empty disctionary", "{}", new VDict()),
            new EvaluationTest("singleton dictionary", "{foo: 5}",
                    new VDict().put(new VString("foo"), VNumber.of(5, 1))),
            new EvaluationTest("sequence indexing", "[3, 4, 5][1]",
                    VNumber.of(4, 1)),
            new EvaluationTest("dictionary access",
                    "{foo: 5, bar: 6, bazz: 9}.bar", VNumber.of(6, 1)),
            new EvaluationTest("dictionary access expression",
                    "{foo: 5, bar: 6, bazz: 9}.('bar')", VNumber.of(6, 1)),
            new EvaluationTest("order of operations",
                    "1 + 3 * 5 * 2 ^ 4 * (6 + 7)", VNumber.of(3121, 1)),
            new EvaluationTest("simple string concat", "'foo' + 'bar'",
                    new VString("foobar")),
            new EvaluationTest("string concat w/ number", "'foo' + 4.1",
                    new VString("foo(41 / 10)")),
            new EvaluationTest("nested structures",
                    "{"
                        + "foo: ["
                            + "[1, 2, {}],"
                            + "{"
                                + "tom: [],"
                                + "dick: ['a'],"
                                +" harry: [4, 5, 6]"
                            + "}],"
                        + "bar: [[[[[]]]]]"
                    + "}",
                    new VDict()
                        .put(new VString("foo"),
                                new VSeq(
                                        new VSeq(VNumber.of(1, 1),
                                                VNumber.of(2, 1),
                                                new VDict()),
                                        new VDict()
                                            .put(new VString("tom"),
                                                    new VSeq())
                                            .put(new VString("dick"),
                                                    new VSeq(new VString("a")))
                                            .put(new VString("harry"),
                                                    new VSeq(VNumber.of(4, 1),
                                                            VNumber.of(5, 1),
                                                            VNumber.of(6, 1)))))
                        .put(new VString("bar"),
                                new VSeq(new VSeq(new VSeq(new VSeq(
                                        new VSeq()))))))
        });
        
        Sbsdl.HostEnvironment h = Mockito.mock(Sbsdl.HostEnvironment.class);
        TestDecider d = new TestDecider();
        
        Sbsdl s = new Sbsdl(h, d);
        
        for (EvaluationTest t : tests) {
            System.out.println("\n\n\n");
            System.out.println("=============================================");
            System.out.println("Starting test: " + t.myName + "\n\n");
            
            Mockito.reset(h);
            Mockito.when(h.evaluate(Mockito.anyString(), Mockito.anyList()))
                .thenReturn(VUnavailable.INSTANCE);
            
            d.setResults(t.myDeciderResults);
            
            try {
                s.run("#out(" + t.myExpression + ");");
            }
            catch (WellFormednessException wfe) {
                System.out.println(wfe);
                throw wfe;
            }
            
            Mockito.verify(h).evaluate("out", list(t.myExpectedResult));
        }
    }
    
    @Test
    public void executionTests()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        List<ExecutionTest> tests = Arrays.asList(new ExecutionTest[] {
            new ExecutionTest("simple intro stmt", "intro x;", "x",
                    VUnavailable.INSTANCE),
            new ExecutionTest("intro with initial value", "intro x = 5;", "x",
                    VNumber.of(5, 1)),
            new ExecutionTest("top level assign", "intro x; x = 5;", "x",
                    VNumber.of(5, 1)),
            new ExecutionTest("self assign", "intro x = 'a'; x = x + 'b';", "x",
                    new VString("ab")),
            new ExecutionTest("if1", "intro p = true; intro x; if p { x = 5; }",
                    "x", VNumber.of(5, 1)),
            new ExecutionTest("if2",
                    "intro p = false; intro x; if p { x = 5; }", "x",
                    VUnavailable.INSTANCE),
            new ExecutionTest("ifelse1",
                    "intro p = true; intro x; if p { x = 1; } else { x = 2; }",
                    "x", VNumber.of(1, 1)),
            new ExecutionTest("ifelse2",
                    "intro p = false; intro x; if p { x = 1; } else { x = 2; }",
                    "x", VNumber.of(2, 1)),
            new ExecutionTest("ifelseif1",
                    "intro y = 1;"
                  + "intro x;"
                  + "if y = 1 {"
                  + "    x = 'if';"
                  + "}"
                  + "else if y = 2 {"
                  + "    x = 'else if';"
                  + "}",
                    "x", new VString("if")),
            new ExecutionTest("ifelseif2",
                    "intro y = 2;"
                  + "intro x;"
                  + "if y = 1 {"
                  + "    x = 'if';"
                  + "}"
                  + "else if y = 2 {"
                  + "    x = 'else if';"
                  + "}",
                    "x", new VString("else if")),
            new ExecutionTest("ifelseif3",
                    "intro y = 3;"
                  + "intro x;"
                  + "if y = 1 {"
                  + "    x = 'if';"
                  + "}"
                  + "else if y = 2 {"
                  + "    x = 'else if';"
                  + "}",
                    "x", VUnavailable.INSTANCE),
            new ExecutionTest("fullif1",
                    "intro y = 1;"
                  + "intro x;"
                  + "if y = 1 {"
                  + "    x = 'if';"
                  + "}"
                  + "else if y = 2 {"
                  + "    x = 'else if 1';"
                  + "}"
                  + "else if y = 3 {"
                  + "    x = 'else if 2';"
                  + "}"
                  + "else {"
                  + "    x = 'else';"
                  + "}",
                    "x", new VString("if")),
            new ExecutionTest("fullif2",
                    "intro y = 2;"
                  + "intro x;"
                  + "if y = 1 {"
                  + "    x = 'if';"
                  + "}"
                  + "else if y = 2 {"
                  + "    x = 'else if 1';"
                  + "}"
                  + "else if y = 3 {"
                  + "    x = 'else if 2';"
                  + "}"
                  + "else {"
                  + "    x = 'else';"
                  + "}",
                    "x", new VString("else if 1")),
            new ExecutionTest("fullif3",
                    "intro y = 3;"
                  + "intro x;"
                  + "if y = 1 {"
                  + "    x = 'if';"
                  + "}"
                  + "else if y = 2 {"
                  + "    x = 'else if 1';"
                  + "}"
                  + "else if y = 3 {"
                  + "    x = 'else if 2';"
                  + "}"
                  + "else {"
                  + "    x = 'else';"
                  + "}",
                    "x", new VString("else if 2")),
            new ExecutionTest("fullif4",
                    "intro y = 4;"
                  + "intro x;"
                  + "if y = 1 {"
                  + "    x = 'if';"
                  + "}"
                  + "else if y = 2 {"
                  + "    x = 'else if 1';"
                  + "}"
                  + "else if y = 3 {"
                  + "    x = 'else if 2';"
                  + "}"
                  + "else {"
                  + "    x = 'else';"
                  + "}",
                    "x", new VString("else")),
            new ExecutionTest("foreach pick",
                    "intro x = '';"
                  + "for each {'abc', 'def', 'ghi'} {"
                  + "    x = x + @;"
                  + "}",
                    "x", new VString("abcdefghi")),
            new ExecutionTest("foreach pick w/ exemplar",
                    "intro x = '';"
                  + "for each s : {'abc', 'def', 'ghi'} {"
                  + "    x = x + s;"
                  + "}",
                    "x", new VString("abcdefghi")),
            new ExecutionTest("foreach seq",
                    "intro x = '';"
                  + "for each ['abc', 'def', 'ghi'] {"
                  + "    x = x + @;"
                  + "}",
                    "x", new VString("abcdefghi")),
            new ExecutionTest("nested for each",
                    "intro x = '';"
                  + "for each s : ['a', 'b', 'c'] {"
                  + "    for each {'d', 'e'} {"
                  + "        x = x + s + @;"
                  + "    }"
                  + "}",
                    "x", new VString("adaebdbecdce")),
            new ExecutionTest("for each where",
                    "intro x = '';"
                  + "for each {3, 1, 4, 1, 5, 9, 2} where @ > 4 {"
                  + "    x = x + @;"
                  + "}",
                    "x", new VString("59")),
            new ExecutionTest("for each where exemplar",
                    "intro x = '';"
                  + "for each s : {3, 1, 4, 1, 5, 9, 2} where s > 4 {"
                  + "    x = x + s;"
                  + "}",
                    "x", new VString("59")),
            new ExecutionTest("evaluation statement",
                    "intro x = fn (y) { #mem(y); };"
                  + "x(6);",
                    "#mem(unavailable)", VNumber.of(6, 1))
        });
        
        TestEnvironment h = new TestEnvironment();
        TestDecider d = new TestDecider();
        
        Sbsdl s = new Sbsdl(h, d);
        
        for (ExecutionTest t : tests) {
            System.out.println("\n\n\n");
            System.out.println("=============================================");
            System.out.println("Starting test: " + t.getName() + "\n\n");
            
            h.reset();
            
            d.setResults(t.getDeciderValues());
            
            try {
                s.run(t.getSetup() + "#out(" + t.getExpression() + ");");
            }
            catch (WellFormednessException wfe) {
                System.out.println(wfe);
                throw wfe;
            }
            
            Assert.assertEquals(t.getExpected(), h.myOut.get(0));
        }
    }
    
    private static <T> List<T> list(final T ... ts) {
        return Arrays.asList(ts);
    }
    
    private static class TestDecider implements Sbsdl.Decider {
        private Iterator<Boolean> myResults;
        
        public void setResults(boolean[] results) {
            List<Boolean> weirdness = new ArrayList<>(results.length);
            for (boolean b : results) {
                weirdness.add(b);
            }
            
            myResults = weirdness.iterator();
        }
        
        @Override
        public boolean randomize(double chance) {
            return myResults.next();
        }
    }
    
    private static class ExecutionTest extends EvaluationTest {
        private final String mySetup;
        
        public ExecutionTest(String name, String setup, String expression,
                Value expected, boolean... deciderResults) {
            super(name, expression, expected, deciderResults);
            
            mySetup = setup;
        }
        
        public String getSetup() {
            return mySetup;
        }
    }
    
    private static class EvaluationTest {
        private final String myName;
        private final String myExpression;
        private final Value myExpectedResult;
        private final boolean[] myDeciderResults;
        
        public EvaluationTest(String name, String expression, Value expected,
                boolean ... deciderResults) {
            myName = name;
            myExpression = expression;
            myExpectedResult = expected;
            myDeciderResults = deciderResults;
        }
        
        public String getName() {
            return myName;
        }
        
        public String getExpression() {
            return myExpression;
        }
        
        public Value getExpected() {
            return myExpectedResult;
        }
        
        public boolean[] getDeciderValues() {
            return myDeciderResults;
        }
    }
    
    private class TestEnvironment implements Sbsdl.HostEnvironment {
        private List<Value> myOut;
        private Value myMem = VUnavailable.INSTANCE;
        
        public void reset() {
            myOut = null;
            myMem = VUnavailable.INSTANCE;
        }
        
        @Override
        public Value evaluate(String name, List<Value> parameters)
                throws Sbsdl.HostEnvironmentException {
            Value result;
            
            if (name.equals("out")) {
                myOut = parameters;
                result = VUnavailable.INSTANCE;
            }
            else if (name.equals("mem")) {
                result = myMem;
                myMem = parameters.get(0);
            }
            else {
                throw new RuntimeException();
            }
            
            return result;
        }
    }
}
