/*
 * Copyright (c) 2017-2018 The Regents of the University of California
 * Copyright (c) 2021 Rohan Padhye
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

package edu.berkeley.cs.jqf.instrument.tracing;

import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import janala.logger.AbstractLogger;
import janala.logger.inst.Instruction;

/**
 * A singleton class which manages per-thread tracers.
 *
 * This class is used both to log instrumented instructions
 * via {@link SingleSnoop}, as well as to provide programmatic
 * access to emit {@link TraceEvent}s.
 *
 * @author Rohan Padhye
 */
public class TraceLogger extends AbstractLogger {

    private static final TraceLogger singleton = new TraceLogger();

    // Each thread (except the first; see below) has a thread-local tracer object
    private final ThreadLocal<ThreadTracer> threadLocalTracer
            = ThreadLocal.withInitial(() -> ThreadTracer.spawn(Thread.currentThread()));

    // The first thread is often the main program or test thread
    private Thread firstThread = null;

    // For performance reasons (e.g. to optimize single-threaded fuzzing), we remember the tracer for this thread
    private ThreadTracer firstTracer;

    private TraceLogger() {
        // Singleton: Prevent outside construction
    }

    private ThreadTracer getTracer() {
        // The vast majority of fuzzing sessions are single-threaded; so, return the tracer quickly instead of
        // looking up the thread-local map. This provides about a 10-20% speedup.
        if (Thread.currentThread() == firstThread) {
            return firstTracer;
        } else if (firstThread == null) {
            // The first time this method is called, remember the "first" thread and its associated treacer
            assert firstTracer == null;
            firstThread = Thread.currentThread();
            firstTracer = ThreadTracer.spawn(firstThread);
            return firstTracer;
        } else {
            // In multi-threaded fuzzing mode, we have to use thread-local variables
            return threadLocalTracer.get();
        }
    }

    /**
     * Returns a handle to the singleton instance.
     *
     * @return a handle to the singleton instance
     */
    public static TraceLogger get() {
        return singleton;
    }

    /** Logs an instrumented byteode instruction for the current thread. */
    @Override
    protected void log(Instruction instruction) {
        getTracer().consume(instruction);
    }

    /**
     * Emits a trace event for the current thread.
     *
     * @param event the event to be emitted
     */
    public void emit(TraceEvent event) {
        getTracer().emit(event);
    }

    /**
     * Removes the trace logger for the current thread
     */
    public void remove() {
        // Make sure to remove the right tracer depending on whether its in the thread-local map or in the field
        if (Thread.currentThread() == firstThread) {
            firstTracer = null;
            firstThread = null;
        } else {
            threadLocalTracer.remove();
        }
    }

}
