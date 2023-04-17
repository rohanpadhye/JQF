package edu.berkeley.cs.jqf.examples.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.examples.common.AsciiStringGenerator;
import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import org.junit.Assume;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

@RunWith(JQF.class)
public class JsonTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Fuzz
    public void fuzzJsonReadValue(@From(AsciiStringGenerator.class) String input) {
        Object output = null;
        try {
            objectMapper.readValue(input, Object.class);
        } catch (JsonProcessingException e) {
           Assume.assumeNoException(e);
        }
    }
}
