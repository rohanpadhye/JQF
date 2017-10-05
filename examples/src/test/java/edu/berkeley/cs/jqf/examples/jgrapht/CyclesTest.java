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
package edu.berkeley.cs.jqf.examples.jgrapht;

import java.io.IOException;
import java.util.List;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.junit.Fuzz;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.JQF;
import org.jgrapht.alg.cycle.JohnsonSimpleCycles;
import org.jgrapht.alg.cycle.SzwarcfiterLauerSimpleCycles;
import org.jgrapht.alg.cycle.TarjanSimpleCycles;
import org.jgrapht.alg.cycle.TiernanSimpleCycles;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.junit.After;
import org.junit.Assume;
import org.junit.runner.RunWith;

/**
 * @author Rohan Padhye
 */
@RunWith(JQF.class)
public class CyclesTest {

    private static final int V = 5;

    private List<?> cycles;

    @Fuzz
    public void johnson(@From(SimpleDirectedGraphGenerator.class)
                            @GraphModel(nodes=V, p=0.5) SimpleDirectedGraph graph) {
        Assume.assumeTrue(graph.edgeSet().size() < 2 * V);
        this.cycles = new JohnsonSimpleCycles<>(graph).findSimpleCycles();
    }

    @Fuzz
    public void tarjan(@From(SimpleDirectedGraphGenerator.class)
                           @GraphModel(nodes=V, p=0.5) SimpleDirectedGraph graph) throws IOException {
        Assume.assumeTrue(graph.edgeSet().size() < 2 * V);
        this.cycles = new TarjanSimpleCycles<>(graph).findSimpleCycles();
    }

    @Fuzz
    public void tiernan(@From(SimpleDirectedGraphGenerator.class)
                            @GraphModel(nodes=V, p=0.5) SimpleDirectedGraph graph) {
        Assume.assumeTrue(graph.edgeSet().size() < 2 * V);
        this.cycles = new TiernanSimpleCycles<>(graph).findSimpleCycles();
    }

    @Fuzz
    public void sl(@GraphModel(nodes=V, p=0.5) SimpleDirectedGraph graph) {
        this.cycles = new SzwarcfiterLauerSimpleCycles<>(graph).findSimpleCycles();
    }

    @After
    public void printCycles() {
        if (this.cycles != null) {
            System.out.println(this.cycles.size() + " cycles found");
        }
    }


}
