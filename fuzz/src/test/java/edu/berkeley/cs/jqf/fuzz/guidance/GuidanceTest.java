/*
 * Copyright (c) 2017-2018 The Regents of the University of California
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.berkeley.cs.jqf.fuzz.guidance;

import java.util.function.Consumer;

import edu.berkeley.cs.jqf.fuzz.random.NoGuidance;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GuidanceTest {

    @RunWith(JQF.class)
    public static class GuidanceTestFuzzer {
        @Fuzz
        public void success(int x) {
            Assert.assertTrue(true);
        }

        @Fuzz
        public void assumptionViolated(int x) {
            Assume.assumeTrue(false);
        }

        @Fuzz
        public void assertionFailure(int x) {
            Assert.assertTrue(false);
        }

        @Fuzz
        public void uncaughtException(int x) {
            throw new RuntimeException();
        }

        @Fuzz
        public void expectedException(int x) throws NullPointerException {
            throw new NullPointerException();
        }

        @Fuzz
        public void timeout(int x) {
            throw new TimeoutException();
        }
    }

    @Spy
    Guidance guidance = new NoGuidance(1, null);

    @Mock()
    Consumer<TraceEvent> eventHandler;


    @Test
    public void testSuccess() {
        GuidedFuzzing.run(GuidanceTestFuzzer.class, "success", guidance, null);
        Mockito.verify(guidance).handleResult(Result.SUCCESS, null);
    }

    @Test
    public void testAssumptionViolated() {
        GuidedFuzzing.run(GuidanceTestFuzzer.class, "assumptionViolated", guidance, null);
        Mockito.verify(guidance).handleResult(
                ArgumentMatchers.eq(Result.INVALID),
                ArgumentMatchers.isA(AssumptionViolatedException.class));
    }

    @Test
    public void testUncaughtException() {
        GuidedFuzzing.run(GuidanceTestFuzzer.class, "uncaughtException", guidance, null);
        Mockito.verify(guidance).handleResult(
                ArgumentMatchers.eq(Result.FAILURE),
                ArgumentMatchers.isA(RuntimeException.class));
    }


    @Test
    public void testAssertionFailure() {
        GuidedFuzzing.run(GuidanceTestFuzzer.class, "assertionFailure", guidance, null);
        Mockito.verify(guidance).handleResult(
                ArgumentMatchers.eq(Result.FAILURE),
                ArgumentMatchers.isA(AssertionError.class));
    }

    @Test
    public void testExpectedException() {
        GuidedFuzzing.run(GuidanceTestFuzzer.class, "expectedException", guidance, null);
        Mockito.verify(guidance).handleResult(Result.SUCCESS, null);
    }

    @Test
    public void testTimeout() {
        GuidedFuzzing.run(GuidanceTestFuzzer.class, "timeout", guidance, null);
        Mockito.verify(guidance).handleResult(
                ArgumentMatchers.eq(Result.TIMEOUT),
                ArgumentMatchers.isA(RuntimeException.class));
    }

}
