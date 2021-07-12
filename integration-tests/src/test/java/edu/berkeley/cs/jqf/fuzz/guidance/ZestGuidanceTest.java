package edu.berkeley.cs.jqf.fuzz.guidance;

import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.berkeley.cs.jqf.instrument.InstrumentingClassLoader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ZestGuidanceTest {

    // Temp directory to store fuzz results
    protected static File resultsDir;

    // Class loader to instrument test
    protected ClassLoader classLoader;

    @Before
    public void initTempDir() throws IOException {
        resultsDir = Files.createTempDirectory("fuzz-results").toFile();
    }

    @Before
    public void initClassLoader() throws IOException  {
        // Walk dependency tree of jqf-examples
        List<String> paths = Files.walk(Paths.get("../examples/target/dependency"))
                .map(Path::toString).collect(Collectors.toList());
        paths.add("../examples/target/test-classes/"); // also add fuzz drivers in jqf-examples

        // Create coverage-instrumenting class loader
        classLoader = new InstrumentingClassLoader(paths.stream().toArray(String[]::new),
                getClass().getClassLoader());
    }

    @Test
    public void testPatriciaTrieFuzzer() throws Exception {
        // Set up test params
        String clazz = "edu.berkeley.cs.jqf.examples.commons.PatriciaTrieTest";
        String method = "testCopy";
        long trials = 5000;
        Random rnd = new Random(42);
        // Create guidance
        ZestGuidance zest = new ZestGuidance("PatriciaTrieTest", null, trials, resultsDir, rnd);
        // Fuzz
        GuidedFuzzing.run(clazz, method, classLoader, zest, null);

        // Validate result
        Assert.assertEquals(381936627, zest.getTotalCoverage().hashCode());

    }
}
