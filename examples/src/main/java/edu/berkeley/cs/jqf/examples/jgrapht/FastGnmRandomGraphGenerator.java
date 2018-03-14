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
package edu.berkeley.cs.jqf.examples.jgrapht;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.Graph;
import org.jgrapht.VertexFactory;
import org.jgrapht.generate.GraphGenerator;

/**
 * @author Rohan Padhye
 */
public class FastGnmRandomGraphGenerator<V, E, T> implements GraphGenerator<V, E, T> {

    private int n;
    private int m;
    private boolean loops;
    private Random rng;

    public FastGnmRandomGraphGenerator(int n, int m, Random rng, boolean loops) {
        this.n = n;
        this.m = m;
        this.loops = loops;
        this.rng = rng;
    }

    private int numEdges(boolean isDirected, boolean loops) {
        if (loops) {
            if (isDirected) {
                return n * n;
            } else {
                return (n * (n + 1)) / 2;
            }
        } else {
            if (isDirected) {
                return n * (n - 1);
            } else {
                return (n * (n - 1)) / 2;
            }
        }
    }


    @Override
    public void generateGraph(Graph<V, E> graph, VertexFactory<V> vertexFactory, Map<String, T> map) {
        // Create nodes
        V[] nodes = (V[]) new Object[n];
        for (int i = 0; i < n; i++) {
            V v = vertexFactory.createVertex();
            nodes[i] = v;
            graph.addVertex(v);
        }

        // Collect ALL edges into a list
        boolean isDirected = graph instanceof DirectedGraph;
        int maxEdges = numEdges(isDirected, loops);
        if (m > maxEdges) {
            throw new IllegalArgumentException("Cannot generate " + m + " edges; " +
                    "max allowed is " + maxEdges);
        }
        E[] edges = (E[]) new Object[maxEdges];
        EdgeFactory<V, E> edgeFactory = graph.getEdgeFactory();
        Map<E, V> sources = new HashMap<>();
        Map<E, V> targets = new HashMap<>();

        int k = 0;
        for (int i = 0; i < n; i++) {
            int start = isDirected ? 0 : i;
            for (int j = start; j < n; j++) {
                if (i != j || loops) {
                    V source = nodes[i];
                    V target = nodes[j];
                    E e = edgeFactory.createEdge(source, target);
                    edges[k++] = e;
                    sources.put(e, source);
                    targets.put(e, target);
                }
            }
        }
        assert(k == maxEdges);

        // Select `m` edges to add
        int available = maxEdges;
        for (int i = 0; i < m; i++) {
            // Select an edge in [0, available)
            int idx = rng.nextInt(available);
            E e = edges[idx];
            graph.addEdge(sources.get(e), targets.get(e), e);
            // Decrement upper bound and fill gap
            available--;
            edges[idx] = edges[available];
        }


    }
}
