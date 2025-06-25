package edu.berkeley.cs.jqf.fuzz.ei;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.instrument.tracing.events.BranchEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEventVisitor;

public class ZestGuidanceTest {
    @Rule
    public TestName testName = new TestName();
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void shouldPrintEndCycleStatistics() throws IOException {
        // public ZestGuidance(String testName, Duration duration, Long trials, File outputDirectory, Random
        // sourceOfRandomness) throws IOException {
        final File output = temp.newFolder("output");
        ZestGuidance zg = new ZestGuidance(
            testName.getMethodName(), Duration.ofMillis(1), 1L, output, new Random()
        );
        for (int i = 0; i < 1000 + 1; i++) {
            try (final InputStream input = zg.getInput()) {
                input.read();
                zg.generateCallBack(null).accept(new BranchEvent(1, null, 0, -1));
                zg.handleResult(Result.SUCCESS, null);
            }
        }
        zg.getInput();

        final Path logFile = output.toPath().resolve("fuzz.log");
        Assert.assertTrue(logFile.toFile().exists());

        final List<String> logLines = Files.readAllLines(logFile);
        Pattern p = Pattern.compile("# Cycle \\d completed at (\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+)");
        Assert.assertTrue(logLines.stream().anyMatch(s -> p.matcher(s).matches()));
    }
}
