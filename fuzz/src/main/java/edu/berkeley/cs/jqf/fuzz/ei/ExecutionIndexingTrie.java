/*
 * Copyright (c) 2018, University of California, Berkeley
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
package edu.berkeley.cs.jqf.fuzz.ei;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * @author Rohan Padhye
 */
public class ExecutionIndexingTrie {

    Collection<Node> children = new ArrayList<>();

    public ExecutionIndexingTrie() {

    }

    public ExecutionIndexingTrie(ExecutionIndexingTrie other) {
        for (Node child : other.children) {
            children.add(child.copy());
        }
    }


    public Integer getValue(ExecutionIndex executionIndex) {
        return getValue(executionIndex.ei, 0, executionIndex.ei.length, false);
    }

    public int getValueOrGenerateFresh(ExecutionIndex executionIndex, Supplier<Integer> supplier) {
        Node node = getNode(executionIndex.ei, 0, executionIndex.ei.length, true);
        if (node.value == null) {
            node.value = supplier.get();
        }
        return node.value;

    }

    public void putValue(ExecutionIndex executionIndex, int value) {
        Node node = getNode(executionIndex.ei, 0, executionIndex.ei.length, true);
        node.value = value;
    }

    public void graft(ExecutionIndexingTrie that, ExecutionIndex executionIndex, int depth) {
        assert (depth <= executionIndex.ei.length);

        // Get my node
        Node myNode = this.getNode(executionIndex.ei, 0, depth, false);
        if (myNode == null) {
            throw new IllegalArgumentException("No such node in this trie");
        }

        // Get their node
        Node thatNode = that.getNode(executionIndex.ei, 0, depth, false);
        if (thatNode == null) {
            throw new IllegalArgumentException("No such node in other trie");
        }

        // Take value of other node
        myNode.value = thatNode.value;
        myNode.children = new ArrayList<>(thatNode.children.size());
        for (Node child : thatNode.children) {
            myNode.children.add(child.copy());
        }


    }

    protected Integer getValue(int[] ei, int start, int end, boolean create) {
        Node node = getNode(ei, start, end, create);
        return node.value;
    }

    protected Node getNode(int[] ei, int start, int end, boolean create) {

        // Extract IID and count from the input
        int iid = ei[start];
        int count = ei[start+1];
        Node child = null;

        // Search for a child node with given iid/count
        for (Node c : children) {
            if (c.iid == iid && c.count == count) {
                child = c;
                break;
            }
        }

        // Handle the case where no child matches the search
        if (child == null) {
            if (create) {
                child = new Node(iid, count);
                children.add(child);
            } else {
                return null;
            }
        }

        // `child` is not a valid child node
        int offset = start + 2;
        if (offset >= end) {
            // If we have reached the end of `ei`, then return
            return child;
        } else {
            // Else recurse
            return child.getNode(ei, offset, end, create);
        }
    }


    protected void collectNodes(Collection<ExecutionIndexingTrie> result) {
        for (ExecutionIndexingTrie child : children) {
            result.add(child);
            child.collectNodes(result);
        }
    }




    /*
    Operations:
    Integer val = trie.getValue(int[] ei);
    if (val == null)
      trie.putValue(int[] ei, x);

    trie.hasSubTrie(int[] ei, length);
    Trie subTrie = trie.getSubTrie(int[] ei, int length);
    Trie subTrie = trie.getRandomSubTrieIgnoreCounts(int[] ei, int length);


    trie.replaceChildrenWithThatOf(subTrie);
     */


    private static class Node extends ExecutionIndexingTrie {

        private final int iid;
        private final int count;
        private Integer value = null;

        Node(int iid, int count, Integer value) {
            this.iid = iid;
            this.count = count;
            this.value = value;
        }

        Node(int iid, int count) {
            this(iid, count, null);
        }

        Node copy() {
            Node clone = new Node(iid, count, value);
            for (Node child : this.children) {
                clone.children.add(child.copy());
            }
            return clone;
        }

    }



}
