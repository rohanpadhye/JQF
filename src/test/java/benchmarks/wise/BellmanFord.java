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
 * @author Sudeep Juvekar <sjuvekar@cs.berkeley.edu>
 * @author Jacob Burnim <jburnim@cs.berkeley.edu>
 */
public class BellmanFord {

    static final int INFINITY = 1000000;

    static int[] runBellmanFord(int N, int D[][], int src) {
        // Initialize distances.
        int dist[] = new int[N];
        boolean infinite[] = new boolean[N];
        for (int i = 0; i < N; i++) {  // V+1 branches
            dist[i] = INFINITY;
            infinite[i] = true;
        }
        dist[src] = 0;
        infinite[src] = false;

        // Keep relaxing edges until either:
        //  (1) No more edges need to be updated.
        //  (2) We have passed through the edges N times.
        int k;
        for (k = 0; k < N; k++) {  // V+1 branches
            boolean relaxed = false;
            for (int i = 0; i < N; i++) {  // V(V+1) branches
                for (int j = 0; j < N; j++) {  // V^2(V+1) branches
                    if (i == j) continue;  // V^3 branches
                    if (!infinite[i]) {  // V^2(V-1) branches
                        if (dist[j] > dist[i] + D[i][j]) {  // V^2(V-1) branches
                            dist[j] = dist[i] + D[i][j];
                            infinite[j] = false;
                            relaxed = true;
                        }
                    }
                }
            }
            if (!relaxed)  // V branches
                break;
        }

        // Check for negative-weight egdes.
        if (k == N) {  // 1 branch
            // We relaxed during the N-th iteration, so there must be
            // a negative-weight cycle.
        }

        // Return the computed distances.
        return dist;
    }

    public static void main(String[] args) {
        final int V = Driver.readInteger();

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

        runBellmanFord(V, D, 0);
        Driver.exit();
    }
}
