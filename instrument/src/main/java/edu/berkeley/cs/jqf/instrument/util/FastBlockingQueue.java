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
package edu.berkeley.cs.jqf.instrument.util;

/**
 * A blocking queue for single-producer and single-consumer where the producer and
 * consumer never change and are distinct threads.
 *
 * This allows reading and writing from the queue without using system
 * locks. The only synchronization is via <tt>volatile</tt> pointers for the producer
 * and consumer in a fixed-size circular buffer.
 *
 * Reading from an empty queue and writing to a full queue results in a spinning wait,
 * and therefore the producer and consumer <em>must</em> be distinct in order to avoid
 * deadlocks.
 *
 * @author Rohan Padhye
 */
public class FastBlockingQueue<T> {
    private final int size;
    private final Object[] buffer;
    private volatile int producer = 0;
    private volatile int consumer = 0;

    public FastBlockingQueue(int size) {
        this.size = size;
        this.buffer = new Object[size];
    }

    private int increment(int idx) {
        return (idx + 1) % size;
    }

    public boolean isEmpty() {
        return producer == consumer;
    }

    public boolean isFull() {
        return increment(producer) == consumer;
    }

    public void put(T item) {
        while (isFull()) {
            // Spin-block
        }
        buffer[producer] = item;
        producer = increment(producer);
    }

    public T remove(long timeout) {
        long ticks = 0;
        while(isEmpty()) {
            if (++ticks > timeout) {
                return null;
            }
        }
        @SuppressWarnings("unchecked")
        T item = (T) buffer[consumer];
        consumer = increment(consumer);
        return item;
    }
}
