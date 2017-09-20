/*
 * @(#)PriorityQueue.java	1.6 04/06/11
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package benchmarks.wise.java15.util;

import benchmarks.wise.java15.lang.Math;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;

/**
 * An unbounded priority {@linkplain Queue queue} based on a priority
 * heap.  This queue orders elements according to an order specified
 * at construction time, which is specified either according to their
 * <i>natural order</i> (see {@link Comparable}), or according to a
 * {@link java.util.Comparator}, depending on which constructor is
 * used. A priority queue does not permit <tt>null</tt> elements.
 * A priority queue relying on natural ordering also does not
 * permit insertion of non-comparable objects (doing so may result
 * in <tt>ClassCastException</tt>).
 *
 * <p>The <em>head</em> of this queue is the <em>least</em> element
 * with respect to the specified ordering.  If multiple elements are
 * tied for least value, the head is one of those elements -- ties are
 * broken arbitrarily.  The queue retrieval operations <tt>poll</tt>,
 * <tt>remove</tt>, <tt>peek</tt>, and <tt>element</tt> access the
 * element at the head of the queue.
 *
 * <p>A priority queue is unbounded, but has an internal
 * <i>capacity</i> governing the size of an array used to store the
 * elements on the queue.  It is always at least as large as the queue
 * size.  As elements are added to a priority queue, its capacity
 * grows automatically.  The details of the growth policy are not
 * specified.
 *
 * <p>This class and its iterator implement all of the
 * <em>optional</em> methods of the {@link Collection} and {@link
 * Iterator} interfaces.
 * The
 * Iterator provided in method {@link #iterator()} is <em>not</em>
 * guaranteed to traverse the elements of the PriorityQueue in any
 * particular order. If you need ordered traversal, consider using
 * <tt>Arrays.sort(pq.toArray())</tt>.
 *
 * <p> <strong>Note that this implementation is not synchronized.</strong>
 * Multiple threads should not access a <tt>PriorityQueue</tt>
 * instance concurrently if any of the threads modifies the list
 * structurally. Instead, use the thread-safe {@link
 * java.util.concurrent.PriorityBlockingQueue} class.
 *
 *
 * <p>Implementation note: this implementation provides O(log(n)) time
 * for the insertion methods (<tt>offer</tt>, <tt>poll</tt>,
 * <tt>remove()</tt> and <tt>add</tt>) methods; linear time for the
 * <tt>remove(Object)</tt> and <tt>contains(Object)</tt> methods; and
 * constant time for the retrieval methods (<tt>peek</tt>,
 * <tt>element</tt>, and <tt>size</tt>).
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../guide/collections/index.html">
 * Java Collections Framework</a>.
 * @since 1.5
 * @version 1.6, 06/11/04
 * @author Josh Bloch
 * @param <E> the type of elements held in this collection
 */
public class PriorityQueue<E> extends AbstractQueue<E>
    implements java.io.Serializable {

    private static final long serialVersionUID = -7720805057305804111L;

    private static final int DEFAULT_INITIAL_CAPACITY = 11;

    /**
     * Priority queue represented as a balanced binary heap: the two children
     * of queue[n] are queue[2*n] and queue[2*n + 1].  The priority queue is
     * ordered by comparator, or by the elements' natural ordering, if
     * comparator is null:  For each node n in the heap and each descendant d
     * of n, n <= d.
     *
     * The element with the lowest value is in queue[1], assuming the queue is
     * nonempty.  (A one-based array is used in preference to the traditional
     * zero-based array to simplify parent and child calculations.)
     *
     * queue.length must be >= 2, even if size == 0.
     */
    private transient Object[] queue;

    /**
     * The number of elements in the priority queue.
     */
    private int size = 0;

    /**
     * The comparator, or null if priority queue uses elements'
     * natural ordering.
     */
    private final Comparator<? super E> comparator;

    /**
     * The number of times this priority queue has been
     * <i>structurally modified</i>.  See AbstractList for gory details.
     */
    private transient int modCount = 0;

    /**
     * Creates a <tt>PriorityQueue</tt> with the default initial capacity
     * (11) that orders its elements according to their natural
     * ordering (using <tt>Comparable</tt>).
     */
    public PriorityQueue() {
        this(DEFAULT_INITIAL_CAPACITY, null);
    }

    /**
     * Creates a <tt>PriorityQueue</tt> with the specified initial capacity
     * that orders its elements according to their natural ordering
     * (using <tt>Comparable</tt>).
     *
     * @param initialCapacity the initial capacity for this priority queue.
     * @throws IllegalArgumentException if <tt>initialCapacity</tt> is less
     * than 1
     */
    public PriorityQueue(int initialCapacity) {
        this(initialCapacity, null);
    }

    /**
     * Creates a <tt>PriorityQueue</tt> with the specified initial capacity
     * that orders its elements according to the specified comparator.
     *
     * @param initialCapacity the initial capacity for this priority queue.
     * @param comparator the comparator used to order this priority queue.
     * If <tt>null</tt> then the order depends on the elements' natural
     * ordering.
     * @throws IllegalArgumentException if <tt>initialCapacity</tt> is less
     * than 1
     */
    public PriorityQueue(int initialCapacity,
                         Comparator<? super E> comparator) {
        if (initialCapacity < 1)
            throw new IllegalArgumentException();
        this.queue = new Object[initialCapacity + 1];
        this.comparator = comparator;
    }

    /**
     * Common code to initialize underlying queue array across
     * constructors below.
     */
    private void initializeArray(Collection<? extends E> c) {
        int sz = c.size();
        int initialCapacity = (int)Math.min((sz * 110L) / 100,
                                            Integer.MAX_VALUE - 1);
        if (initialCapacity < 1)
            initialCapacity = 1;

        this.queue = new Object[initialCapacity + 1];
    }

    /**
     * Initially fill elements of the queue array under the
     * knowledge that it is sorted or is another PQ, in which
     * case we can just place the elements in the order presented.
     */
    private void fillFromSorted(Collection<? extends E> c) {
        for (Iterator<? extends E> i = c.iterator(); i.hasNext(); )
            queue[++size] = i.next();
    }

    /**
     * Initially fill elements of the queue array that is not to our knowledge
     * sorted, so we must rearrange the elements to guarantee the heap
     * invariant.
     */
    private void fillFromUnsorted(Collection<? extends E> c) {
        for (Iterator<? extends E> i = c.iterator(); i.hasNext(); )
            queue[++size] = i.next();
        heapify();
    }

    /**
     * Creates a <tt>PriorityQueue</tt> containing the elements in the
     * specified collection.  The priority queue has an initial
     * capacity of 110% of the size of the specified collection or 1
     * if the collection is empty.  If the specified collection is an
     * instance of a {@link java.util.SortedSet} or is another
     * <tt>PriorityQueue</tt>, the priority queue will be sorted
     * according to the same comparator, or according to its elements'
     * natural order if the collection is sorted according to its
     * elements' natural order.  Otherwise, the priority queue is
     * ordered according to its elements' natural order.
     *
     * @param c the collection whose elements are to be placed
     *        into this priority queue.
     * @throws ClassCastException if elements of the specified collection
     *         cannot be compared to one another according to the priority
     *         queue's ordering.
     * @throws NullPointerException if <tt>c</tt> or any element within it
     * is <tt>null</tt>
     */
    public PriorityQueue(Collection<? extends E> c) {
        initializeArray(c);
        if (c instanceof SortedSet) {
            SortedSet<? extends E> s = (SortedSet<? extends E>)c;
            comparator = (Comparator<? super E>)s.comparator();
            fillFromSorted(s);
        } else if (c instanceof PriorityQueue) {
            PriorityQueue<? extends E> s = (PriorityQueue<? extends E>) c;
            comparator = (Comparator<? super E>)s.comparator();
            fillFromSorted(s);
        } else {
            comparator = null;
            fillFromUnsorted(c);
        }
    }

    /**
     * Creates a <tt>PriorityQueue</tt> containing the elements in the
     * specified collection.  The priority queue has an initial
     * capacity of 110% of the size of the specified collection or 1
     * if the collection is empty.  This priority queue will be sorted
     * according to the same comparator as the given collection, or
     * according to its elements' natural order if the collection is
     * sorted according to its elements' natural order.
     *
     * @param c the collection whose elements are to be placed
     *        into this priority queue.
     * @throws ClassCastException if elements of the specified collection
     *         cannot be compared to one another according to the priority
     *         queue's ordering.
     * @throws NullPointerException if <tt>c</tt> or any element within it
     * is <tt>null</tt>
     */
    public PriorityQueue(PriorityQueue<? extends E> c) {
        initializeArray(c);
        comparator = (Comparator<? super E>)c.comparator();
        fillFromSorted(c);
    }

    /**
     * Creates a <tt>PriorityQueue</tt> containing the elements in the
     * specified collection.  The priority queue has an initial
     * capacity of 110% of the size of the specified collection or 1
     * if the collection is empty.  This priority queue will be sorted
     * according to the same comparator as the given collection, or
     * according to its elements' natural order if the collection is
     * sorted according to its elements' natural order.
     *
     * @param c the collection whose elements are to be placed
     *        into this priority queue.
     * @throws ClassCastException if elements of the specified collection
     *         cannot be compared to one another according to the priority
     *         queue's ordering.
     * @throws NullPointerException if <tt>c</tt> or any element within it
     * is <tt>null</tt>
     */
    public PriorityQueue(SortedSet<? extends E> c) {
        initializeArray(c);
        comparator = (Comparator<? super E>)c.comparator();
        fillFromSorted(c);
    }

    /**
     * Resize array, if necessary, to be able to hold given index
     */
    private void grow(int index) {
        int newlen = queue.length;
        if (index < newlen) // don't need to grow
            return;
        if (index == Integer.MAX_VALUE)
            throw new OutOfMemoryError();
        while (newlen <= index) {
            if (newlen >= Integer.MAX_VALUE / 2)  // avoid overflow
                newlen = Integer.MAX_VALUE;
            else
                newlen <<= 2;
        }
        Object[] newQueue = new Object[newlen];
        System.arraycopy(queue, 0, newQueue, 0, queue.length);
        queue = newQueue;
    }


    /**
     * Inserts the specified element into this priority queue.
     *
     * @return <tt>true</tt>
     * @throws ClassCastException if the specified element cannot be compared
     * with elements currently in the priority queue according
     * to the priority queue's ordering.
     * @throws NullPointerException if the specified element is <tt>null</tt>.
     */
    public boolean offer(E o) {
        if (o == null)  // 1 branch
            throw new NullPointerException();
        modCount++;
        ++size;

        // Grow backing store if necessary
        if (size >= queue.length)  // 1 branch
            grow(size);

        queue[size] = o;
        fixUp(size);
        return true;
    }

    public E peek() {
        if (size == 0)
            return null;
        return (E) queue[1];
    }

    // Collection Methods - the first two override to update docs

    /**
     * Adds the specified element to this queue.
     * @return <tt>true</tt> (as per the general contract of
     * <tt>Collection.add</tt>).
     *
     * @throws NullPointerException if the specified element is <tt>null</tt>.
     * @throws ClassCastException if the specified element cannot be compared
     * with elements currently in the priority queue according
     * to the priority queue's ordering.
     */
    public boolean add(E o) {
        return offer(o);
    }

    /**
     * Removes a single instance of the specified element from this
     * queue, if it is present.
     */
    public boolean remove(Object o) {
        if (o == null)
            return false;

        if (comparator == null) {
            for (int i = 1; i <= size; i++) {
                if (((Comparable<E>)queue[i]).compareTo((E)o) == 0) {
                    removeAt(i);
                    return true;
                }
            }
        } else {
            for (int i = 1; i <= size; i++) {
                if (comparator.compare((E)queue[i], (E)o) == 0) {
                    removeAt(i);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns an iterator over the elements in this queue. The iterator
     * does not return the elements in any particular order.
     *
     * @return an iterator over the elements in this queue.
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {

        /**
         * Index (into queue array) of element to be returned by
         * subsequent call to next.
         */
        private int cursor = 1;

        /**
         * Index of element returned by most recent call to next,
         * unless that element came from the forgetMeNot list.
         * Reset to 0 if element is deleted by a call to remove.
         */
        private int lastRet = 0;

        /**
         * The modCount value that the iterator believes that the backing
         * List should have.  If this expectation is violated, the iterator
         * has detected concurrent modification.
         */
        private int expectedModCount = modCount;

        /**
         * A list of elements that were moved from the unvisited portion of
         * the heap into the visited portion as a result of "unlucky" element
         * removals during the iteration.  (Unlucky element removals are those
         * that require a fixup instead of a fixdown.)  We must visit all of
         * the elements in this list to complete the iteration.  We do this
         * after we've completed the "normal" iteration.
         *
         * We expect that most iterations, even those involving removals,
         * will not use need to store elements in this field.
         */
        private ArrayList<E> forgetMeNot = null;

        /**
         * Element returned by the most recent call to next iff that
         * element was drawn from the forgetMeNot list.
         */
        private Object lastRetElt = null;

        public boolean hasNext() {
            return cursor <= size || forgetMeNot != null;
        }

        public E next() {
            checkForComodification();
            E result;
            if (cursor <= size) {
                result = (E) queue[cursor];
                lastRet = cursor++;
            }
            else if (forgetMeNot == null)
                throw new NoSuchElementException();
            else {
                int remaining = forgetMeNot.size();
                result = forgetMeNot.remove(remaining - 1);
                if (remaining == 1)
                    forgetMeNot = null;
                lastRet = 0;
                lastRetElt = result;
            }
            return result;
        }

        public void remove() {
            checkForComodification();

            if (lastRet != 0) {
                E moved = PriorityQueue.this.removeAt(lastRet);
                lastRet = 0;
                if (moved == null) {
                    cursor--;
                } else {
                    if (forgetMeNot == null)
                        forgetMeNot = new ArrayList<E>();
                    forgetMeNot.add(moved);
                }
            } else if (lastRetElt != null) {
                PriorityQueue.this.remove(lastRetElt);
                lastRetElt = null;
            } else {
                throw new IllegalStateException();
            }

            expectedModCount = modCount;
        }

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

    public int size() {
        return size;
    }

    /**
     * Removes all elements from the priority queue.
     * The queue will be empty after this call returns.
     */
    public void clear() {
        modCount++;

        // Null out element references to prevent memory leak
        for (int i=1; i<=size; i++)
            queue[i] = null;

        size = 0;
    }

    public E poll() {
        if (size == 0)
            return null;
        modCount++;

        E result = (E) queue[1];
        queue[1] = queue[size];
        queue[size--] = null;  // Drop extra ref to prevent memory leak
        if (size > 1)
            fixDown(1);

        return result;
    }

    /**
     * Removes and returns the ith element from queue.  (Recall that queue
     * is one-based, so 1 <= i <= size.)
     *
     * Normally this method leaves the elements at positions from 1 up to i-1,
     * inclusive, untouched.  Under these circumstances, it returns null.
     * Occasionally, in order to maintain the heap invariant, it must move
     * the last element of the list to some index in the range [2, i-1],
     * and move the element previously at position (i/2) to position i.
     * Under these circumstances, this method returns the element that was
     * previously at the end of the list and is now at some position between
     * 2 and i-1 inclusive.
     */
    private E removeAt(int i) {
        assert i > 0 && i <= size;
        modCount++;

        E moved = (E) queue[size];
        queue[i] = moved;
        queue[size--] = null;  // Drop extra ref to prevent memory leak
        if (i <= size) {
            fixDown(i);
            if (queue[i] == moved) {
                fixUp(i);
                if (queue[i] != moved)
                    return moved;
            }
        }
        return null;
    }

    /**
     * Establishes the heap invariant (described above) assuming the heap
     * satisfies the invariant except possibly for the leaf-node indexed by k
     * (which may have a nextExecutionTime less than its parent's).
     *
     * This method functions by "promoting" queue[k] up the hierarchy
     * (by swapping it with its parent) repeatedly until queue[k]
     * is greater than or equal to its parent.
     */
    private void fixUp(int k) {
        if (comparator == null) {  // 1 branch
            while (k > 1) {  // log2(N)+1 branches
                int j = k >> 1;
                // log2(N) branches
                if (((Comparable<E>)queue[j]).compareTo((E)queue[k]) <= 0)
                    break;
                Object tmp = queue[j];  queue[j] = queue[k]; queue[k] = tmp;
                k = j;
            }
        } else {
            while (k > 1) {
                int j = k >>> 1;
                if (comparator.compare((E)queue[j], (E)queue[k]) <= 0)
                    break;
                Object tmp = queue[j];  queue[j] = queue[k]; queue[k] = tmp;
                k = j;
            }
        }
    }

    /**
     * Establishes the heap invariant (described above) in the subtree
     * rooted at k, which is assumed to satisfy the heap invariant except
     * possibly for node k itself (which may be greater than its children).
     *
     * This method functions by "demoting" queue[k] down the hierarchy
     * (by swapping it with its smaller child) repeatedly until queue[k]
     * is less than or equal to its children.
     */
    private void fixDown(int k) {
        int j;
        if (comparator == null) {
            while ((j = k << 1) <= size && (j > 0)) {
                if (j<size &&
                    ((Comparable<E>)queue[j]).compareTo((E)queue[j+1]) > 0)
                    j++; // j indexes smallest kid

                if (((Comparable<E>)queue[k]).compareTo((E)queue[j]) <= 0)
                    break;
                Object tmp = queue[j];  queue[j] = queue[k]; queue[k] = tmp;
                k = j;
            }
        } else {
            while ((j = k << 1) <= size && (j > 0)) {
                if (j<size &&
                    comparator.compare((E)queue[j], (E)queue[j+1]) > 0)
                    j++; // j indexes smallest kid
                if (comparator.compare((E)queue[k], (E)queue[j]) <= 0)
                    break;
                Object tmp = queue[j];  queue[j] = queue[k]; queue[k] = tmp;
                k = j;
            }
        }
    }

    /**
     * Establishes the heap invariant (described above) in the entire tree,
     * assuming nothing about the order of the elements prior to the call.
     */
    private void heapify() {
        for (int i = size/2; i >= 1; i--)
            fixDown(i);
    }

    /**
     * Returns the comparator used to order this collection, or <tt>null</tt>
     * if this collection is sorted according to its elements natural ordering
     * (using <tt>Comparable</tt>).
     *
     * @return the comparator used to order this collection, or <tt>null</tt>
     * if this collection is sorted according to its elements natural ordering.
     */
    public Comparator<? super E> comparator() {
        return comparator;
    }

    /**
     * Save the state of the instance to a stream (that
     * is, serialize it).
     *
     * @serialData The length of the array backing the instance is
     * emitted (int), followed by all of its elements (each an
     * <tt>Object</tt>) in the proper order.
     * @param s the stream
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException{
        // Write out element count, and any hidden stuff
        s.defaultWriteObject();

        // Write out array length
        s.writeInt(queue.length);

        // Write out all elements in the proper order.
        for (int i=1; i<=size; i++)
            s.writeObject(queue[i]);
    }

    /**
     * Reconstitute the <tt>ArrayList</tt> instance from a stream (that is,
     * deserialize it).
     * @param s the stream
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        // Read in size, and any hidden stuff
        s.defaultReadObject();

        // Read in array length and allocate array
        int arrayLength = s.readInt();
        queue = new Object[arrayLength];

        // Read in all elements in the proper order.
        for (int i=1; i<=size; i++)
            queue[i] = (E) s.readObject();
    }

}
