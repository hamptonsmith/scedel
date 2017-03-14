package sbsdl;

import com.shieldsbetter.flexcompilator.WellFormednessException;
import java.util.Arrays;
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
                    VNumber.of(123, 1))
        });
        
        Sbsdl.HostEnvironment h = Mockito.mock(Sbsdl.HostEnvironment.class);
        Sbsdl s = new Sbsdl(h);
        
        for (EvaluationTest t : tests) {
            System.out.println("\n\n\n");
            System.out.println("=============================================");
            System.out.println("Starting test: " + t.myName + "\n\n");
            
            Mockito.reset(h);
            Mockito.when(h.evaluate(Mockito.anyString(), Mockito.anyList()))
                .thenReturn(VUnavailable.INSTANCE);
            
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
    
    private static class EvaluationTest {
        private String myName;
        private String myExpression;
        private Value myExpectedResult;
        
        public EvaluationTest(String name, String expression, Value expected) {
            myName = name;
            myExpression = expression;
            myExpectedResult = expected;
        }
    }
}
