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

package edu.berkeley.cs.jqf.examples.trees;

/**
 * @author Rohan Padhye
 */
public class BinaryTree {
    int size = 0;
    private static class Node {
        Node left;
        Node right;
        int value;

        Node(int val) {
            this.value = val;
        }
    }

    private Node root;

    public void insert(int x) {
        root = insertIntoNode(x, root);
    }

    public int size() {
        return this.size;
    }

    private Node insertIntoNode(int x, Node n) {
        if (n == null) {
            size++;
            return new Node(x);
        } else if (x < n.value) {
            n.left = insertIntoNode(x, n.left);
            return n;
        } else if (x > n.value) {
            n.right = insertIntoNode(x, n.right);
            return n;
        } else {
            return n;
        }
    }

    public boolean contains(int x) {
        return nodeContains(root, x);
    }

    private boolean nodeContains(Node n, int x) {
        if (n == null) {
            return false;
        } else if (x < n.value) {
            return nodeContains(n.left, x);
        } else if (x > n.value) {
            return nodeContains(n.right, x);
        } else {
            return true;
        }
    }
}
