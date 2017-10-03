/*
 * Copyright (c) 2017, University of California, Berkeley
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

import java.io.File;
import java.io.PrintStream;
import java.util.function.Consumer;

import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;

/**
 * A front-end that only generates random inputs.
 *
 * This class provides no guidance to quickcheck. It seeds random values from
 * <tt>/dev/urandom</tt>, making it effectively an unguided random test input
 * generator.
 */
public class NoGuidance implements Guidance {

    protected boolean keepGoing = true;
    protected long numTrials = 0;
    protected final long maxTrials;
    protected final PrintStream out;

    public NoGuidance(long maxTrials, PrintStream out) {
        if (maxTrials <= 0) {
            throw new IllegalArgumentException("maxTrials must be greater than 0");
        }
        this.maxTrials = maxTrials;
        this.out = out;
    }

    @Override
    public File getInputFile() {
        return new File("/dev/urandom");
    }

    @Override
    public boolean hasInput() {
        return keepGoing;
    }

    @Override
    public void handleResult(Result result, Throwable error) {
        numTrials++;
        if (error != null) {
            if (out != null) {
                error.printStackTrace(out);
            }
            this.keepGoing = false;
        }

        if (numTrials >= maxTrials) {
            this.keepGoing = false;
        }
    }

    @Override
    public Consumer<TraceEvent> generateCallBack(String threadName) {
        return (e) -> {};
    }
}
