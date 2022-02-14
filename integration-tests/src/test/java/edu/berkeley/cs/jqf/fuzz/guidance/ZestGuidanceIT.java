package edu.berkeley.cs.jqf.fuzz.guidance;

import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ZestGuidanceIT extends AbstractGuidanceIT {

    private static class ProbedZestGuidance extends ZestGuidance {
        List<Integer> inputHashes = new ArrayList<>();

        ProbedZestGuidance(String name, long trials, Random rnd) throws IOException {
            super(name, null, trials, resultsDir, rnd);
        }

        @Override
        public void observeGeneratedArgs(Object[] args) {
            this.inputHashes.add(Arrays.hashCode(args));
        }

        int hashInputHashes() {
            return inputHashes.hashCode();
        }

        int corpusCount() {
            return savedInputs.size();
        }

        int hashTotalCoverage() {
            return totalCoverage.hashCode();
        }

        int hashValidCoverage() {
            return validCoverage.hashCode();
        }
    }

    @Test
    public void testPatriciaTrieFuzzer() throws Exception {
        // Set up test params
        String clazz = "edu.berkeley.cs.jqf.examples.commons.PatriciaTrieTest";
        String method = "testCopyAscii";
        long trials = 5000;
        Random rnd = new Random(42);
        // Create guidance
        ProbedZestGuidance zest = new ProbedZestGuidance("PatriciaTrieTest", trials, rnd);
        // Fuzz
        GuidedFuzzing.run(clazz, method, classLoader, zest, null);

        // Validate result
        Assert.assertEquals(29, zest.corpusCount());
        Assert.assertEquals(-941815396, zest.hashInputHashes());
        Assert.assertEquals(-684278400, zest.hashTotalCoverage());
        Assert.assertEquals(-1096184368, zest.hashValidCoverage());
    }


    // This function tests if the instrumentation framework of Zest handles
    // branch instructions in the target program properly.
    @Test
    public void testSimpleTestCoverage() throws Exception {
        String clazz = "edu.berkeley.cs.jqf.examples.simple.SimpleClassTest";
        String method = "testWithGenerator";

        long trials = 5000;
        Random rnd = new Random(42);

        ProbedZestGuidance zest = new ProbedZestGuidance("SimpleClassTest", trials, rnd);
        GuidedFuzzing.run(clazz, method, classLoader, zest, null);

        // Validate result
        Assert.assertEquals(7, zest.getTotalCoverage().getNonZeroCount());

    }
}
