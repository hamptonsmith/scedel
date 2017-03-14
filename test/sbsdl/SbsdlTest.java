package sbsdl;

import com.shieldsbetter.flexcompilator.WellFormednessException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;
import org.mockito.Mockito;
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
                    "basic number", "123.457", VNumber.of(123457, 1000)),
            new EvaluationTest("basic string", "'hello \\'quoted'",
                    new VString("hello 'quoted")),
            new EvaluationTest("basic sequence", "[3.1, 'foo', []]",
                    new VSeq(VNumber.of(31, 10), new VString("foo"),
                            new VSeq())),
            new EvaluationTest("basic function", "fn(x) { return x; }(123)",
                    VNumber.of(123, 1)),
            new EvaluationTest("basic pick", "pick 2 unique from "
                    + "{ 'foo' {1}, 'bar' {5}, 'bazz' {5} }",
                    new VSeq(new VString("foo"), new VString("bazz")),
                    true, false, false, false, true)
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
    
    private static class EvaluationTest {
        private String myName;
        private String myExpression;
        private Value myExpectedResult;
        private boolean[] myDeciderResults;
        
        public EvaluationTest(String name, String expression, Value expected,
                boolean ... deciderResults) {
            myName = name;
            myExpression = expression;
            myExpectedResult = expected;
            myDeciderResults = deciderResults;
        }
    }
}
