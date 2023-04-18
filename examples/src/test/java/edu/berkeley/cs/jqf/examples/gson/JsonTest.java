package edu.berkeley.cs.jqf.examples.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.examples.common.AsciiStringGenerator;
import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import org.junit.Assume;
import org.junit.runner.RunWith;

@RunWith(JQF.class)
public class JsonTest {

    private Gson gson = new Gson();

    @Fuzz
    public void fuzzJSONParser(@From(AsciiStringGenerator.class) String input) {
        try {
            gson.fromJson(input, Object.class);
        } catch (JsonSyntaxException e) {
            Assume.assumeNoException(e);
        } catch (JsonIOException e) {
            Assume.assumeNoException(e);
        }
    }
}
