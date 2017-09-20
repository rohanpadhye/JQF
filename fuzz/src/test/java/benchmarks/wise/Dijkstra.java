/**
 * Copyright (c) 2011, Regents of the University of California
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * <p/>
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * <p/>
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution.
 * <p/>
 * 3. Neither the name of the University of California, Berkeley nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package benchmarks.wise;

import benchmarks.wise.driver.Driver;

/**
 * @author jburnim@cs.berkeley.edu
 */
public class Dijkstra {

    static final int INFINITY = 1000000;

    static int[] runDijkstra(int N, int D[][], int src) {
        // Initialize distances.
        int dist[] = new int[N];
        boolean fixed[] = new boolean[N];
        for (int i = 0; i < N; i++) {  // V+1 branches
            dist[i] = INFINITY;
            fixed[i] = false;
        }
        dist[src] = 0;

        for (int k = 0; k < N; k++) { // V+1 branches
            // Find the minimum-distance, unfixed vertex.
            int min = -1;
            int minDist = INFINITY;
            for (int i = 0; i < N; i++) { // V(V+1) branches
                if (!fixed[i] && (dist[i] < minDist)) { // V^2 + V(V+1)/2
                    min = i;
                    minDist = dist[i];
                }
            }

            // Fix the vertex.
            fixed[min] = true;

            // Process the vertex's outgoing edges.
            for (int i = 0; i < N; i++) { // V(V+1) branches
                // V^2 + V(V-1)/2 branches
                if (!fixed[i] && (dist[min] + D[min][i] < dist[i])) {
                    dist[i] = dist[min] + D[min][i];
                }
            }
        }

        // Return the computed distances.
        return dist;
    }

    public static void main(String[] args) {
        final int V = 4;

        final int D[][] = new int[V][V];

        for (int i = 0; i < V; i++) {
            for (int j = 0; j < V; j++) {
                if (i ==j) continue;
                D[i][j] = Driver.readInteger();
            }
        }

        // We only measure the complexity (i.e. path length) of the
        // graph algorithm itself.  That is, we count branches only
        // from this point forward in the execution.

        runDijkstra(V, D, 0);
        Driver.exit();

    }
}
