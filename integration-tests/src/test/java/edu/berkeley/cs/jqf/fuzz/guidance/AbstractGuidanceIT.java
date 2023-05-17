package edu.berkeley.cs.jqf.fuzz.guidance;

import edu.berkeley.cs.jqf.instrument.InstrumentingClassLoader;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractGuidanceIT {

    // Temp directory to store fuzz results
    protected static File resultsDir;

    // Class loader to instrument test
    protected ClassLoader classLoader;

    @Before
    public void initTempDir() throws IOException {
        resultsDir = Files.createTempDirectory("fuzz-results").toFile();
    }

    @Before
    public void initClassLoader() throws IOException {
        // Walk dependency tree of jqf-examples
        List<String> paths = Files.walk(Paths.get("../examples/target/dependency"))
                .map(Path::toString).collect(Collectors.toList());
        paths.add("../examples/target/classes/"); // add sources from jqf-examples
        paths.add("../examples/target/test-classes/"); // also add fuzz drivers in jqf-examples

        // Create coverage-instrumenting class loader
        classLoader = new InstrumentingClassLoader(paths.stream().toArray(String[]::new),
                getClass().getClassLoader());
    }

    @After
    public void clearTempDir() {
        resultsDir.delete();
    }

}
