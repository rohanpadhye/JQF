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
public class BinaryTreeSearch {

    private static class BinaryTree {
        /**
         * Internal class representing a Node in the tree.
         */
        private static class Node {
            int value;
            Node left;
            Node right;

            Node(int v, Node l, Node r) {
                value = v;
                left = l;
                right = r;
            }
        }

        private Node root = null;

        /**
         * Inserts a value in to the tree.
         */
        public void insert(int v) {

            if (root == null) {
                root = new Node(v, null, null);
                return;
            }

            Node curr = root;
            while (true) {
                if (curr.value < v) {
                    if (curr.right != null) {
                        curr = curr.right;
                    } else {
                        curr.right = new Node(v, null, null);
                        break;
                    }
                } else if (curr.value > v) {
                    if (curr.left != null) {
                        curr = curr.left;
                    } else {
                        curr.left = new Node(v, null, null);
                        break;
                    }
                } else {
                    break;
                }
            }
        }

        /**
         * Searches for a value in the tree.
         */
        public boolean search(int v) {
            Node curr = root;
            while (curr != null) {  // N branches
                if (curr.value == v) {  // N-1 branches
                    return true;
                } else if (curr.value < v) {  // N-1 branches
                    curr = curr.right;
                } else {
                    curr = curr.left;
                }
            }
            return false;
        }
    }


    public static void main(String args[]) {
        final int N = Driver.readInteger();

        BinaryTree b = new BinaryTree();
        for (int i = 0; i < N; i++) {
            b.insert(Driver.readInteger());
        }

        // We only measure the complexity (i.e. path length) of the
        // final search operation.  That is, we count branches only
        // from this point forward in the execution.

        b.search(Driver.readInteger());
        Driver.exit();
    }
}
