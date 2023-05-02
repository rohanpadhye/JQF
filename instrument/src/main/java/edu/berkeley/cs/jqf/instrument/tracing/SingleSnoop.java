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

package edu.berkeley.cs.jqf.instrument.tracing;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import edu.berkeley.cs.jqf.instrument.util.DoublyLinkedList;


@SuppressWarnings("unused") // Dynamically loaded
public final class SingleSnoop {

    static DoublyLinkedList<Thread> threadsToUnblock = new DoublyLinkedList<>();

    private static ThreadLocal<Boolean> block = new ThreadLocal<Boolean>() {
        @Override
        public Boolean initialValue() {
        String threadName = Thread.currentThread().getName();
            if (threadsToUnblock.synchronizedRemove(Thread.currentThread())){
                return false; // Snoop on threads that were added to the queue explicitly
            } else {
                return true; // Block all other threads (e.g. JVM cleanup threads)
            }
        }
    };

    public static final Map<Thread, String> entryPoints = new WeakHashMap<>();


    /** A supplier of callbacks for each thread (does nothing by default). */
    static Function<Thread, Consumer<TraceEvent>> callbackGenerator = (t) -> (e) -> {};


    private static final TraceLogger intp = TraceLogger.get();

    private SingleSnoop() {}


    /**
     * Register a supplier of callbacks for each named thread, which will consume
     * {@link TraceEvent}s.
     *
     * @param callbackGenerator a supplier of thread-specific callbacks
     */
    public static void setCallbackGenerator(Function<Thread, Consumer<TraceEvent>> callbackGenerator) {
        SingleSnoop.callbackGenerator = callbackGenerator;
    }


    /** Start snooping for this thread, with the top-level call being
     * the {@code entryPoint}
     *
     * @param entryPoint the top-level method, formatted as
     *                   {@code CLASS#METHOD} (e.g.
     *                   {@code FooBar#main}).
     */
    public static void startSnooping(String entryPoint) {
        // Mark entry point
        entryPoints.put(Thread.currentThread(), entryPoint);
        // XXX: Offer a dummy instruction to warm-up
        // class-loaders of the logger, in order to avoid
        // deadlocks when tracing is triggered from
        // SnoopInstructionTransformer#transform()
        intp.SPECIAL(-1);
        // Unblock snooping for current thread
        unblock();
    }

    public static void unblock() {
        block.set(false);
    }

    public static void REGISTER_THREAD(Thread thread) {
        // Mark the Thread subclass's run() method as entry point for this Thread object
        String runMethod = thread.getClass().getName() + "#run";
        entryPoints.put(thread, runMethod);

        // XXX: The above logic will not work for objects of type java.lang.Thread
        // (that is, not a subclass with an overridden `run` method) which use a custom
        // `Runnable` object in their constructor. For such threads, the entry point
        // will simply be `java.lang.Thread#run`, which is never instrumented and so
        // never observed by JQF. However, entry points only matter when
        // MATCH_CALLEE_NAMES is turned on (e.g. for JDK fuzzing or for EI guidance).
        // In those cases, we simply do not support threads with custom Runnables,
        // which is fine because those use cases are mostly single-threaded anyway.
        // This will not affect the default mode of fuzzing with Zest via Maven.
        // Previous attempts at tracking the Runnable object inside java.lang.Thread
        // instances via reflection failed after the Java 9 modules system was
        // created; we cannot export the `java.lang` package from the `java.base` module
        // without having control of the JVM start-up initialization flags.

        // Mark thread for unblocking when we snoop its first instruction
        threadsToUnblock.synchronizedAddFirst(thread);
        // XXX: Could this cause a memory leak if threads are added but not removed?

    }

    public static void LDC(int iid, int mid, int c) {
        if (block.get()) return; else block.set(true);
        try { intp.LDC(iid, mid, c); } finally { block.set(false); }
    }

    public static void LDC(int iid, int mid, long c) {
        if (block.get()) return; else block.set(true);
        try { intp.LDC(iid, mid, c); } finally { block.set(false); }
    }

    public static void LDC(int iid, int mid, float c) {
        if (block.get()) return; else block.set(true);
        try { intp.LDC(iid, mid, c); } finally { block.set(false); }
    }

    public static void LDC(int iid, int mid, double c) {
        if (block.get()) return; else block.set(true);
        try { intp.LDC(iid, mid, c); } finally { block.set(false); }
    }

    public static void LDC(int iid, int mid, String c) {
        if (block.get()) return; else block.set(true);
        try { intp.LDC(iid, mid, c); } finally { block.set(false); }
    }

    public static void LDC(int iid, int mid, Object c) {
        if (block.get()) return; else block.set(true);
        try { intp.LDC(iid, mid, c); } finally { block.set(false); }
    }

    public static void IINC(int iid, int mid, int var, int increment) {
        if (block.get()) return; else block.set(true);
        try { intp.IINC(iid, mid, var, increment); } finally { block.set(false); }
    }

    public static void MULTIANEWARRAY(int iid, int mid, String desc, int dims) {
        if (block.get()) return; else block.set(true);
        try { intp.MULTIANEWARRAY(iid, mid, desc, dims); } finally { block.set(false); }
    }

    public static void LOOKUPSWITCH(int iid, int mid, int dflt, int[] keys, int[] labels) {
        if (block.get()) return; else block.set(true);
        try { intp.LOOKUPSWITCH(iid, mid, dflt, keys, labels); } finally { block.set(false); }
    }

    public static void TABLESWITCH(int iid, int mid, int min, int max, int dflt, int[] labels) {
        if (block.get()) return; else block.set(true);
        try { intp.TABLESWITCH(iid, mid, min, max, dflt, labels); } finally { block.set(false); }
    }

    public static void IFEQ(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        try { intp.IFEQ(iid, mid, label); } finally { block.set(false); }
    }

    public static void IFNE(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        try { intp.IFNE(iid, mid, label); } finally { block.set(false); }
    }

    public static void IFLT(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        try { intp.IFLT(iid, mid, label); } finally { block.set(false); }
    }

    public static void IFGE(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        try { intp.IFGE(iid, mid, label); } finally { block.set(false); }
    }

    public static void IFGT(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        try { intp.IFGT(iid, mid, label); } finally { block.set(false); }
    }

    public static void IFLE(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        try { intp.IFLE(iid, mid, label); } finally { block.set(false); }
    }

    public static void IF_ICMPEQ(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        try { intp.IF_ICMPEQ(iid, mid, label); } finally { block.set(false); }
    }

    public static void IF_ICMPNE(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        try { intp.IF_ICMPNE(iid, mid, label); } finally { block.set(false); }
    }

    public static void IF_ICMPLT(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        try { intp.IF_ICMPLT(iid, mid, label); } finally { block.set(false); }
    }

    public static void IF_ICMPGE(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        try { intp.IF_ICMPGE(iid, mid, label); } finally { block.set(false); }
    }

    public static void IF_ICMPGT(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        try { intp.IF_ICMPGT(iid, mid, label); } finally { block.set(false); }
    }

    public static void IF_ICMPLE(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        try { intp.IF_ICMPLE(iid, mid, label); } finally { block.set(false); }
    }

    public static void IF_ACMPEQ(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        try { intp.IF_ACMPEQ(iid, mid, label); } finally { block.set(false); }
    }

    public static void IF_ACMPNE(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        try { intp.IF_ACMPNE(iid, mid, label); } finally { block.set(false); }
    }

    public static void GOTO(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        try { intp.GOTO(iid, mid, label); } finally { block.set(false); }
    }

    public static void JSR(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        try { intp.JSR(iid, mid, label); } finally { block.set(false); }
    }

    public static void IFNULL(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        try { intp.IFNULL(iid, mid, label); } finally { block.set(false); }
    }

    public static void IFNONNULL(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        try { intp.IFNONNULL(iid, mid, label); } finally { block.set(false); }
    }

    public static void INVOKEVIRTUAL(int iid, int mid, String owner, String name, String desc) {
        if (block.get()) return; else block.set(true);
        try { intp.INVOKEVIRTUAL(iid, mid, owner, name, desc); } finally { block.set(false); }
    }

    public static void INVOKESPECIAL(int iid, int mid, String owner, String name, String desc) {
        if (block.get()) return; else block.set(true);
        try { intp.INVOKESPECIAL(iid, mid, owner, name, desc); } finally { block.set(false); }
    }

    public static void INVOKESTATIC(int iid, int mid, String owner, String name, String desc) {
        if (block.get()) return; else block.set(true);
        try { intp.INVOKESTATIC(iid, mid, owner, name, desc); } finally { block.set(false); }
    }

    public static void INVOKEINTERFACE(int iid, int mid, String owner, String name, String desc) {
        if (block.get()) return; else block.set(true);
        try { intp.INVOKEINTERFACE(iid, mid, owner, name, desc); } finally { block.set(false); }
    }

    public static void GETSTATIC(int iid, int mid, int cIdx, int fIdx, String desc) {
        if (block.get()) return; else block.set(true);
        try { intp.GETSTATIC(iid, mid, cIdx, fIdx, desc); } finally { block.set(false); }
    }

    public static void PUTSTATIC(int iid, int mid, int cIdx, int fIdx, String desc) {
        if (block.get()) return; else block.set(true);
        try { intp.PUTSTATIC(iid, mid, cIdx, fIdx, desc); } finally { block.set(false); }
    }

    public static void GETFIELD(int iid, int mid, int cIdx, int fIdx, String desc) {
        if (block.get()) return; else block.set(true);
        try { intp.GETFIELD(iid, mid, cIdx, fIdx, desc); } finally { block.set(false); }
    }

    public static void PUTFIELD(int iid, int mid, int cIdx, int fIdx, String desc) {
        if (block.get()) return; else block.set(true);
        try { intp.PUTFIELD(iid, mid, cIdx, fIdx, desc); } finally { block.set(false); }
    }

    public static void HEAPLOAD1(Object object, String field, int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.HEAPLOAD(iid, mid, System.identityHashCode(object), field); } finally { block.set(false); }
    }

    public static void HEAPLOAD2(Object object, int idx, int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.HEAPLOAD(iid, mid, System.identityHashCode(object), String.valueOf(idx)); } finally { block.set(false); }
    }

    public static void NEW(int iid, int mid, String type) {
        if (block.get()) return; else block.set(true);
        try { intp.NEW(iid, mid, type, 0); } finally { block.set(false); }
    }

    public static void ANEWARRAY(int iid, int mid, String type) {
        if (block.get()) return; else block.set(true);
        try { intp.ANEWARRAY(iid, mid, type); } finally { block.set(false); }
    }

    public static void CHECKCAST(int iid, int mid, String type) {
        if (block.get()) return; else block.set(true);
        try { intp.CHECKCAST(iid, mid, type); } finally { block.set(false); }
    }

    public static void INSTANCEOF(int iid, int mid, String type) {
        if (block.get()) return; else block.set(true);
        try { intp.INSTANCEOF(iid, mid, type); } finally { block.set(false); }
    }

    public static void BIPUSH(int iid, int mid, int value) {
        if (block.get()) return; else block.set(true);
        try { intp.BIPUSH(iid, mid, value); } finally { block.set(false); }
    }

    public static void SIPUSH(int iid, int mid, int value) {
        if (block.get()) return; else block.set(true);
        try { intp.SIPUSH(iid, mid, value); } finally { block.set(false); }
    }

    public static void NEWARRAY(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.NEWARRAY(iid, mid); } finally { block.set(false); }
    }

    public static void ILOAD(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        try { intp.ILOAD(iid, mid, var); } finally { block.set(false); }
    }

    public static void LLOAD(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        try { intp.LLOAD(iid, mid, var); } finally { block.set(false); }
    }

    public static void FLOAD(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        try { intp.FLOAD(iid, mid, var); } finally { block.set(false); }
    }

    public static void DLOAD(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        try { intp.DLOAD(iid, mid, var); } finally { block.set(false); }
    }

    public static void ALOAD(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        try { intp.ALOAD(iid, mid, var); } finally { block.set(false); }
    }

    public static void ISTORE(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        try { intp.ISTORE(iid, mid, var); } finally { block.set(false); }
    }

    public static void LSTORE(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        try { intp.LSTORE(iid, mid, var); } finally { block.set(false); }
    }

    public static void FSTORE(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        try { intp.FSTORE(iid, mid, var); } finally { block.set(false); }
    }

    public static void DSTORE(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        try { intp.DSTORE(iid, mid, var); } finally { block.set(false); }
    }

    public static void ASTORE(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        try { intp.ASTORE(iid, mid, var); } finally { block.set(false); }
    }

    public static void RET(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        try { intp.RET(iid, mid, var); } finally { block.set(false); }
    }

    public static void NOP(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.NOP(iid, mid); } finally { block.set(false); }
    }

    public static void ACONST_NULL(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.ACONST_NULL(iid, mid); } finally { block.set(false); }
    }

    public static void ICONST_M1(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.ICONST_M1(iid, mid); } finally { block.set(false); }
    }

    public static void ICONST_0(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.ICONST_0(iid, mid); } finally { block.set(false); }
    }

    public static void ICONST_1(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.ICONST_1(iid, mid); } finally { block.set(false); }
    }

    public static void ICONST_2(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.ICONST_2(iid, mid); } finally { block.set(false); }
    }

    public static void ICONST_3(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.ICONST_3(iid, mid); } finally { block.set(false); }
    }

    public static void ICONST_4(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.ICONST_4(iid, mid); } finally { block.set(false); }
    }

    public static void ICONST_5(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.ICONST_5(iid, mid); } finally { block.set(false); }
    }

    public static void LCONST_0(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.LCONST_0(iid, mid); } finally { block.set(false); }
    }

    public static void LCONST_1(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.LCONST_1(iid, mid); } finally { block.set(false); }
    }

    public static void FCONST_0(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.FCONST_0(iid, mid); } finally { block.set(false); }
    }

    public static void FCONST_1(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.FCONST_1(iid, mid); } finally { block.set(false); }
    }

    public static void FCONST_2(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.FCONST_2(iid, mid); } finally { block.set(false); }
    }

    public static void DCONST_0(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DCONST_0(iid, mid); } finally { block.set(false); }
    }

    public static void DCONST_1(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DCONST_1(iid, mid); } finally { block.set(false); }
    }

    public static void IALOAD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.IALOAD(iid, mid); } finally { block.set(false); }
    }

    public static void LALOAD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.LALOAD(iid, mid); } finally { block.set(false); }
    }

    public static void FALOAD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.FALOAD(iid, mid); } finally { block.set(false); }
    }

    public static void DALOAD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DALOAD(iid, mid); } finally { block.set(false); }
    }

    public static void AALOAD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.AALOAD(iid, mid); } finally { block.set(false); }
    }

    public static void BALOAD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.BALOAD(iid, mid); } finally { block.set(false); }
    }

    public static void CALOAD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.CALOAD(iid, mid); } finally { block.set(false); }
    }

    public static void SALOAD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.SALOAD(iid, mid); } finally { block.set(false); }
    }

    public static void IASTORE(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.IASTORE(iid, mid); } finally { block.set(false); }
    }

    public static void LASTORE(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.LASTORE(iid, mid); } finally { block.set(false); }
    }

    public static void FASTORE(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.FASTORE(iid, mid); } finally { block.set(false); }
    }

    public static void DASTORE(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DASTORE(iid, mid); } finally { block.set(false); }
    }

    public static void AASTORE(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.AASTORE(iid, mid); } finally { block.set(false); }
    }

    public static void BASTORE(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.BASTORE(iid, mid); } finally { block.set(false); }
    }

    public static void CASTORE(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.CASTORE(iid, mid); } finally { block.set(false); }
    }

    public static void SASTORE(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.SASTORE(iid, mid); } finally { block.set(false); }
    }

    public static void POP(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.POP(iid, mid); } finally { block.set(false); }
    }

    public static void POP2(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.POP2(iid, mid); } finally { block.set(false); }
    }

    public static void DUP(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DUP(iid, mid); } finally { block.set(false); }
    }

    public static void DUP_X1(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DUP_X1(iid, mid); } finally { block.set(false); }
    }

    public static void DUP_X2(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DUP_X2(iid, mid); } finally { block.set(false); }
    }

    public static void DUP2(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DUP2(iid, mid); } finally { block.set(false); }
    }

    public static void DUP2_X1(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DUP2_X1(iid, mid); } finally { block.set(false); }
    }

    public static void DUP2_X2(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DUP2_X2(iid, mid); } finally { block.set(false); }
    }

    public static void SWAP(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.SWAP(iid, mid); } finally { block.set(false); }
    }

    public static void IADD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.IADD(iid, mid); } finally { block.set(false); }
    }

    public static void LADD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.LADD(iid, mid); } finally { block.set(false); }
    }

    public static void FADD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.FADD(iid, mid); } finally { block.set(false); }
    }

    public static void DADD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DADD(iid, mid); } finally { block.set(false); }
    }

    public static void ISUB(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.ISUB(iid, mid); } finally { block.set(false); }
    }

    public static void LSUB(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.LSUB(iid, mid); } finally { block.set(false); }
    }

    public static void FSUB(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.FSUB(iid, mid); } finally { block.set(false); }
    }

    public static void DSUB(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DSUB(iid, mid); } finally { block.set(false); }
    }

    public static void IMUL(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.IMUL(iid, mid); } finally { block.set(false); }
    }

    public static void LMUL(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.LMUL(iid, mid); } finally { block.set(false); }
    }

    public static void FMUL(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.FMUL(iid, mid); } finally { block.set(false); }
    }

    public static void DMUL(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DMUL(iid, mid); } finally { block.set(false); }
    }

    public static void IDIV(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.IDIV(iid, mid); } finally { block.set(false); }
    }

    public static void LDIV(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.LDIV(iid, mid); } finally { block.set(false); }
    }

    public static void FDIV(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.FDIV(iid, mid); } finally { block.set(false); }
    }

    public static void DDIV(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DDIV(iid, mid); } finally { block.set(false); }
    }

    public static void IREM(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.IREM(iid, mid); } finally { block.set(false); }
    }

    public static void LREM(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.LREM(iid, mid); } finally { block.set(false); }
    }

    public static void FREM(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.FREM(iid, mid); } finally { block.set(false); }
    }

    public static void DREM(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DREM(iid, mid); } finally { block.set(false); }
    }

    public static void INEG(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.INEG(iid, mid); } finally { block.set(false); }
    }

    public static void LNEG(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.LNEG(iid, mid); } finally { block.set(false); }
    }

    public static void FNEG(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.FNEG(iid, mid); } finally { block.set(false); }
    }

    public static void DNEG(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DNEG(iid, mid); } finally { block.set(false); }
    }

    public static void ISHL(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.ISHL(iid, mid); } finally { block.set(false); }
    }

    public static void LSHL(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.LSHL(iid, mid); } finally { block.set(false); }
    }

    public static void ISHR(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.ISHR(iid, mid); } finally { block.set(false); }
    }

    public static void LSHR(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.LSHR(iid, mid); } finally { block.set(false); }
    }

    public static void IUSHR(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.IUSHR(iid, mid); } finally { block.set(false); }
    }

    public static void LUSHR(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.LUSHR(iid, mid); } finally { block.set(false); }
    }

    public static void IAND(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.IAND(iid, mid); } finally { block.set(false); }
    }

    public static void LAND(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.LAND(iid, mid); } finally { block.set(false); }
    }

    public static void IOR(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.IOR(iid, mid); } finally { block.set(false); }
    }

    public static void LOR(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.LOR(iid, mid); } finally { block.set(false); }
    }

    public static void IXOR(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.IXOR(iid, mid); } finally { block.set(false); }
    }

    public static void LXOR(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.LXOR(iid, mid); } finally { block.set(false); }
    }

    public static void I2L(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.I2L(iid, mid); } finally { block.set(false); }
    }

    public static void I2F(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.I2F(iid, mid); } finally { block.set(false); }
    }

    public static void I2D(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.I2D(iid, mid); } finally { block.set(false); }
    }

    public static void L2I(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.L2I(iid, mid); } finally { block.set(false); }
    }

    public static void L2F(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.L2F(iid, mid); } finally { block.set(false); }
    }

    public static void L2D(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.L2D(iid, mid); } finally { block.set(false); }
    }

    public static void F2I(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.F2I(iid, mid); } finally { block.set(false); }
    }

    public static void F2L(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.F2L(iid, mid); } finally { block.set(false); }
    }

    public static void F2D(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.F2D(iid, mid); } finally { block.set(false); }
    }

    public static void D2I(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.D2I(iid, mid); } finally { block.set(false); }
    }

    public static void D2L(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.D2L(iid, mid); } finally { block.set(false); }
    }

    public static void D2F(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.D2F(iid, mid); } finally { block.set(false); }
    }

    public static void I2B(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.I2B(iid, mid); } finally { block.set(false); }
    }

    public static void I2C(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.I2C(iid, mid); } finally { block.set(false); }
    }

    public static void I2S(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.I2S(iid, mid); } finally { block.set(false); }
    }

    public static void LCMP(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.LCMP(iid, mid); } finally { block.set(false); }
    }

    public static void FCMPL(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.FCMPL(iid, mid); } finally { block.set(false); }
    }

    public static void FCMPG(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.FCMPG(iid, mid); } finally { block.set(false); }
    }

    public static void DCMPL(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DCMPL(iid, mid); } finally { block.set(false); }
    }

    public static void DCMPG(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DCMPG(iid, mid); } finally { block.set(false); }
    }

    public static void IRETURN(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.IRETURN(iid, mid); } finally { block.set(false); }
    }

    public static void LRETURN(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.LRETURN(iid, mid); } finally { block.set(false); }
    }

    public static void FRETURN(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.FRETURN(iid, mid); } finally { block.set(false); }
    }

    public static void DRETURN(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.DRETURN(iid, mid); } finally { block.set(false); }
    }

    public static void ARETURN(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.ARETURN(iid, mid); } finally { block.set(false); }
    }

    public static void RETURN(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.RETURN(iid, mid); } finally { block.set(false); }
    }

    public static void ARRAYLENGTH(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.ARRAYLENGTH(iid, mid); } finally { block.set(false); }
    }

    public static void ATHROW(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.ATHROW(iid, mid); } finally { block.set(false); }
    }

    public static void MONITORENTER(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.MONITORENTER(iid, mid); } finally { block.set(false); }
    }

    public static void MONITOREXIT(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        try { intp.MONITOREXIT(iid, mid); } finally { block.set(false); }
    }

    public static void GETVALUE_double(double v) {
        if (block.get()) return; else block.set(true);
        try { intp.GETVALUE_double(v); } finally { block.set(false); }
    }

    public static void GETVALUE_long(long v) {
        if (block.get()) return; else block.set(true);
        try { intp.GETVALUE_long(v); } finally { block.set(false); }
    }

    public static void GETVALUE_Object(Object v) {
        if (block.get()) return; else block.set(true);
        try { intp.GETVALUE_Object(v); } finally { block.set(false); }
    }

    public static void GETVALUE_boolean(boolean v) {
        if (block.get()) return; else block.set(true);
        try { intp.GETVALUE_boolean(v); } finally { block.set(false); }
    }

    public static void GETVALUE_byte(byte v) {
        if (block.get()) return; else block.set(true);
        try { intp.GETVALUE_byte(v); } finally { block.set(false); }
    }

    public static void GETVALUE_char(char v) {
        if (block.get()) return; else block.set(true);
        try { intp.GETVALUE_char(v); } finally { block.set(false); }
    }

    public static void GETVALUE_float(float v) {
        if (block.get()) return; else block.set(true);
        try { intp.GETVALUE_float(v); } finally { block.set(false); }
    }

    public static void GETVALUE_int(int v) {
        if (block.get()) return; else block.set(true);
        try { intp.GETVALUE_int(v); } finally { block.set(false); }
    }

    public static void GETVALUE_short(short v) {
        if (block.get()) return; else block.set(true);
        try { intp.GETVALUE_short(v); } finally { block.set(false); }
    }

    public static void GETVALUE_void() {
        if (block.get()) return; else block.set(true);
        try { intp.GETVALUE_void(); } finally { block.set(false); }
    }

    public static void METHOD_BEGIN(String className, String methodName, String desc) {
        if (block.get()) return; else block.set(true);
        try { intp.METHOD_BEGIN(className, methodName, desc); } finally { block.set(false); }
    }

    public static void METHOD_BEGIN(String className, String methodName, String desc, Object obj) {
        if (block.get()) return; else block.set(true);
        try { intp.METHOD_BEGIN(className, methodName, desc, obj); } finally { block.set(false); }
    }

    public static void METHOD_THROW() {
        if (block.get()) return; else block.set(true);
        try { intp.METHOD_THROW(); } finally { block.set(false); }
    }

    public static void INVOKEMETHOD_EXCEPTION(Throwable err) {
        if (block.get()) return; else block.set(true);
        try { intp.INVOKEMETHOD_EXCEPTION(err); } finally { block.set(false); }
    }

    public static void INVOKEMETHOD_END() {
        if (block.get()) return; else block.set(true);
        try { intp.INVOKEMETHOD_END(); } finally { block.set(false); }
    }

    public static void SPECIAL(int i) {
        if (block.get()) return; else block.set(true);
        try { intp.SPECIAL(i); } finally { block.set(false); }
    }

    public static void MAKE_SYMBOLIC() {
        if (block.get()) return; else block.set(true);
        try { intp.MAKE_SYMBOLIC(); } finally { block.set(false); }
    }

    public static void flush() {
        intp.flush();
    }
}
