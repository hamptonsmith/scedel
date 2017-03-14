package sbsdl;

import com.shieldsbetter.flexcompilator.WellFormednessException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.mockito.Mockito;
import sbsdl.values.VNumber;
import sbsdl.values.VUnavailable;
import sbsdl.values.Value;

public class SbsdlTest {
    @Test
    public void basicTest1()
            throws WellFormednessException, Sbsdl.HostEnvironmentException {
        Sbsdl.HostEnvironment h = Mockito.mock(Sbsdl.HostEnvironment.class);
        Mockito.when(h.evaluate(Mockito.anyString(), Mockito.anyList()))
                .thenReturn(VUnavailable.INSTANCE);
        
        Sbsdl s = new Sbsdl(h);
        
        s.run("#out(2 + 3);");
        
        Mockito.verify(h).evaluate("out", list((Value) VNumber.of(5, 1)));
    }
    
    private static <T> List<T> list(final T ... ts) {
        return Arrays.asList(ts);
    }
}
