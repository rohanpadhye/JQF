/*
 * Copyright (c) 2026 Vladimir Sitnikov and JQF Contributors
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
package edu.berkeley.cs.jqf.fuzz.difffuzz;

import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.spi.TrialExecutor;
import edu.berkeley.cs.jqf.instrument.InstrumentationException;

/**
 * Wraps a {@link TrialExecutor} to capture each trial's outcome for differential fuzzing.
 *
 * <p>The delegate runs the trial under its test framework; this decorator records
 * the return value (or the thrown error) as an {@link Outcome} and hands it to the
 * {@link DiffFuzzGuidance}, which decides whether the trial passed, failed, or
 * differs from a reference run.
 */
public final class DiffTrialExecutor implements TrialExecutor {

    private final TrialExecutor delegate;
    private final DiffFuzzGuidance guidance;

    public DiffTrialExecutor(TrialExecutor delegate, DiffFuzzGuidance guidance) {
        this.delegate = delegate;
        this.guidance = guidance;
    }

    @Override
    public void runTrial(Object[] args) throws Throwable {
        Outcome outcome;
        try {
            delegate.runTrial(args);
            outcome = new Outcome(delegate.getLastOutput(), null);
        } catch (InstrumentationException e) {
            throw new GuidanceException(e);
        } catch (GuidanceException e) {
            throw e;
        } catch (Throwable e) {
            outcome = new Outcome(null, e);
        }
        guidance.acceptOutcome(outcome);
    }
}
