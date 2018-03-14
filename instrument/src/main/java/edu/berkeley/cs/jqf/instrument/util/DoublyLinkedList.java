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

package edu.berkeley.cs.jqf.instrument.util;

import java.util.Iterator;

/**
 * @author Rohan Padhye
 */
public class DoublyLinkedList<T> implements Iterable<T>, Stack<T> {

    static class Node<T> {
        T value;
        Node<T> next;
        Node<T> prev;
        Node(T value) {
            this.value = value;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private int length;

    public DoublyLinkedList() {
        head = null;
        tail = null;
        length = 0;
    }

    public void addFirst(T value) {
        Node<T> node = new Node<>(value);
        if (head != null) {
            head.prev = node;
            node.next = head;
        }
        head = node;
        if (tail == null)
            tail = node;
        length++;
    }

    public void addLast(T value) {
        Node<T> node = new Node<>(value);
        if (tail != null) {
            tail.next = node;
            node.prev = tail;
        }
        tail = node;
        if (head == null)
            head = node;
        length++;
    }

    public T removeFirst() {
        if (length == 0)
            throw new IllegalStateException("Cannot remove from empty list");

        T value = head.value;

        if (length == 1) {
            head = null;
            tail = null;
        } else {
            head = head.next;
            head.prev = null;
        }
        length--;
        return value;
    }


    public T removeLast() {
        if (length == 0)
            throw new IllegalStateException("Cannot remove from empty list");

        T value = head.value;

        if (length == 1) {
            head = null;
            tail = null;
        } else {
            tail = tail.prev;
            tail.next = null;
        }
        length--;
        return value;
    }

    private T removeNode(Node<T> node) {
        if (node == this.head) {
            return this.removeFirst();
        } else if (node == this.tail) {
            return this.removeLast();
        } else {
            // Removal of internal node
            T value = node.value;
            assert(node.prev != null); // It's not the head
            assert(node.next != null); // It's not the tail
            node.prev.next = node.next;
            node.next.prev = node.prev;
            node.prev = null;
            node.next = null;
            node.value = null;
            length--;
            return value;
        }
    }

    public boolean remove(T item) {
        Iterator<T> it = this.iterator();
        while (it.hasNext()) {
            if (item.equals(it.next())) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public int size() {
        return length;
    }

    public void push(T item) {
        addLast(item);
    }

    public T peek() {
        if (length == 0)
            throw new IllegalStateException("Cannot peek at empty stack");
        return tail.value;
    }

    public T pop() {
        return removeLast();
    }

    public boolean isEmpty() {
        return length == 0;
    }

    public void clear() {
        head = null;
        tail = null;
        length = 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new LinkedListIterator(this);
    }

    static class LinkedListIterator<T> implements Iterator<T> {

        DoublyLinkedList<T> list;
        Node<T> node;
        Node<T> lastReturnedNode;

        LinkedListIterator(DoublyLinkedList<T> list) {
            this.list = list;
            node = list.head;
        }

        @Override
        public boolean hasNext() {
            return node != null;
        }

        @Override
        public T next() {
            T val = node.value;
            this.lastReturnedNode = node;
            node = node.next;
            return val;
        }

        @Override
        public void remove() {
            list.removeNode(lastReturnedNode);
        }
    }

    public synchronized void synchronizedAddFirst(T item) {
        this.addFirst(item);
    }

    public synchronized void synchronizedAddLast(T item) {
        this.addLast(item);
    }

    public synchronized boolean synchronizedRemove(T item) {
        return this.remove(item);
    }

    public synchronized T synchronizedRemoveFirst() {
        return this.removeFirst();
    }

    public synchronized T synchronizedRemoveLast() {
        return this.removeLast();
    }

    @Override
    public String toString() {
        if (this.isEmpty()) {
            return "[]";
        }
        StringBuffer sb = new StringBuffer();
        sb.append('[');
        for (T item: this) {
            sb.append(item.toString());
            sb.append(", ");
        }
        sb.replace(sb.length()-2, sb.length(), "]");
        return sb.toString();
    }
}
