package com.shieldsbetter.scedel.visitors;

import com.shieldsbetter.scedel.ExecutionException;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScedelTest;
import com.shieldsbetter.scedel.StaticCodeException;
import com.shieldsbetter.scedel.values.Value;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Scanner;
import org.junit.Test;

public class SerializerTest {
    @Test
    public void constructableInScedelTest()
            throws StaticCodeException, ExecutionException, IOException {
        String code;
        try (Scanner s = new Scanner(
                ScedelTest.class.getResourceAsStream("tests.scedel"))) {
            code = s.useDelimiter("\\A").hasNext() ? s.next() : "";
        }
        
        Scedel s = new Scedel(new ScedelTest.TestEnvironment(),
                new ScedelTest.TestDecider());
        Value v1 = s.evaluate(code);
        
        StringWriter w = new StringWriter();
        Serializer.serialize(v1, new PrintWriter(w));
        
        for (String ss : w.getBuffer().toString().split("\\$\\$")) {
            System.out.println(ss + "$$");
        }
        
        Value v2 = Serializer.deserialize(new StringReader(w.toString()));
    }
}
