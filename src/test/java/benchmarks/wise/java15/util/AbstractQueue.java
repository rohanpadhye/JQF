/*
 * @(#)AbstractQueue.java	1.6 04/01/27
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package benchmarks.wise.java15.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * This class provides skeletal implementations of some {@link Queue}
 * operations. The implementations in this class are appropriate when
 * the base implementation does <em>not</em> allow <tt>null</tt>
 * elements.  Methods {@link #add add}, {@link #remove remove}, and
 * {@link #element element} are based on {@link #offer offer}, {@link
 * #poll poll}, and {@link #peek peek}, respectively but throw
 * exceptions instead of indicating failure via <tt>false</tt> or
 * <tt>null</tt> returns.
 *
 * <p> A <tt>Queue</tt> implementation that extends this class must
 * minimally define a method {@link Queue#offer} which does not permit
 * insertion of <tt>null</tt> elements, along with methods {@link
 * Queue#peek}, {@link Queue#poll}, {@link Collection#size}, and a
 * {@link Collection#iterator} supporting {@link
 * Iterator#remove}. Typically, additional methods will be overridden
 * as well. If these requirements cannot be met, consider instead
 * subclassing {@link AbstractCollection}.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../guide/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this collection
 */
public abstract class AbstractQueue<E>
    extends AbstractCollection<E>
    implements Queue<E> {

    /**
     * Constructor for use by subclasses.
     */
    protected AbstractQueue() {
    }


    /**
     * Adds the specified element to this queue. This implementation
     * returns <tt>true</tt> if <tt>offer</tt> succeeds, else
     * throws an IllegalStateException.
     *
     * @param o the element
     * @return <tt>true</tt> (as per the general contract of
     *         <tt>Collection.add</tt>).
     *
     * @throws NullPointerException if the specified element is <tt>null</tt>
     * @throws IllegalStateException if element cannot be added
     */
    public boolean add(E o) {
        if (offer(o))
            return true;
        else
            throw new IllegalStateException("Queue full");
    }

    /**
     * Retrieves and removes the head of this queue.
     * This implementation returns the result of <tt>poll</tt>
     * unless the queue is empty.
     *
     * @return the head of this queue.
     * @throws NoSuchElementException if this queue is empty.
     */
    public E remove() {
        E x = poll();
        if (x != null)
            return x;
        else
            throw new NoSuchElementException();
    }


    /**
     * Retrieves, but does not remove, the head of this queue.
     * This implementation returns the result of <tt>peek</tt>
     * unless the queue is empty.
     *
     * @return the head of this queue.
     * @throws NoSuchElementException if this queue is empty.
     */
    public E element() {
        E x = peek();
        if (x != null)
            return x;
        else
            throw new NoSuchElementException();
    }

    /**
     * Removes all of the elements from this collection.
     * The collection will be empty after this call returns.
     * <p>This implementation repeatedly invokes {@link #poll poll} until it
     * returns <tt>null</tt>.
     */
    public void clear() {
        while (poll() != null)
            ;
    }

    /**
     * Adds all of the elements in the specified collection to this
     * queue.  Attempts to addAll of a queue to itself result in
     * <tt>IllegalArgumentException</tt>. Further, the behavior of
     * this operation is undefined if the specified collection is
     * modified while the operation is in progress.
     *
     * <p>This implementation iterates over the specified collection,
     * and adds each element returned by the iterator to this
     * collection, in turn.  A runtime exception encountered while
     * trying to add an element (including, in particular, a
     * <tt>null</tt> element) may result in only some of the elements
     * having been successfully added when the associated exception is
     * thrown.
     *
     * @param c collection whose elements are to be added to this collection.
     * @return <tt>true</tt> if this collection changed as a result of the
     *         call.
     * @throws NullPointerException if the specified collection or
     * any of its elements are null.
     * @throws IllegalArgumentException if c is this queue.
     *
     * @see #add(Object)
     */
    public boolean addAll(Collection<? extends E> c) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        boolean modified = false;
        Iterator<? extends E> e = c.iterator();
        while (e.hasNext()) {
            if (add(e.next()))
                modified = true;
        }
        return modified;
    }

}
