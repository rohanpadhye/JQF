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
package edu.berkeley.cs.jqf.examples.jdk;

import java.util.Arrays;

import com.pholser.junit.quickcheck.generator.Size;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

/**
 * @author Rohan Padhye
 */

@RunWith(JQF.class)
public class SortTest {

    @BeforeClass
    public static void ensureTimSortEnabled() {
        Assert.assertFalse(Boolean.getBoolean("java.jdk.Arrays.useLegacyMergeSort"));
    }



    // Merges two subarrays of arr[].
    // First subarray is arr[l..m]
    // Second subarray is arr[m+1..r]
    private void merge(int arr[], int l, int m, int r)
    {
        // Find sizes of two subarrays to be merged
        int n1 = m - l + 1;
        int n2 = r - m;

        /* Create temp arrays */
        int L[] = new int [n1];
        int R[] = new int [n2];

        /*Copy data to temp arrays*/
        for (int i=0; i<n1; ++i)
            L[i] = arr[l + i];
        for (int j=0; j<n2; ++j)
            R[j] = arr[m + 1+ j];


        /* Merge the temp arrays */

        // Initial indexes of first and second subarrays
        int i = 0, j = 0;

        // Initial index of merged subarry array
        int k = l;
        while (i < n1 && j < n2)
        {
            if (L[i] <= R[j])
            {
                arr[k] = L[i];
                i++;
            }
            else
            {
                arr[k] = R[j];
                j++;
            }
            k++;
        }

        /* Copy remaining elements of L[] if any */
        while (i < n1)
        {
            arr[k] = L[i];
            i++;
            k++;
        }

        /* Copy remaining elements of R[] if any */
        while (j < n2)
        {
            arr[k] = R[j];
            j++;
            k++;
        }
    }

    // Main function that sorts arr[l..r] using
    // merge()
    private void mergeSort(int arr[], int l, int r)
    {
        if (l < r)
        {
            // Find the middle point
            int m = (l+r)/2;

            // Sort first and second halves
            mergeSort(arr, l, m);
            mergeSort(arr , m+1, r);

            // Merge the sorted halves
            merge(arr, l, m, r);
        }
    }

    @Fuzz
    public void mergeSort(int @Size(min=200, max=200)[] items){
        mergeSort(items, 0, items.length-1);
    }

    // from http://www.java2novice.com/java-sorting-algorithms/quick-sort/
    private void quickSort(int[] array, int lowerIndex, int higherIndex) {

        int i = lowerIndex;
        int j = higherIndex;
        // calculate pivot number, I am taking pivot as middle index number
        int pivot = array[lowerIndex+(higherIndex-lowerIndex)/2];
        // Divide into two arrays
        while (i <= j) {
            /**
             * In each iteration, we will identify a number from left side which
             * is greater then the pivot value, and also we will identify a number
             * from right side which is less then the pivot value. Once the search
             * is done, then we exchange both numbers.
             */
            while (array[i] < pivot) {
                i++;
            }
            while (array[j] > pivot) {
                j--;
            }
            if (i <= j) {
                int temp = array[i];
                array[i] = array[j];
                array[j] = temp;
                //move index to next position on both sides
                i++;
                j--;
            }
        }
        // call quickSort() method recursively
        if (lowerIndex < j)
            quickSort(array, lowerIndex, j);
        if (i < higherIndex)
            quickSort(array, i, higherIndex);
    }

    @Fuzz
    public void quickSort(int @Size(min=200, max=200)[] items){
        quickSort(items, 0, items.length-1);
    }

    @Fuzz
    public void timSort(Integer @Size(min=200, max=200)[] items) {
        // Sort using TimSort
        Arrays.sort(items);

        // Assert sorted
        for (int i = 1; i < items.length; i++) {
            Assert.assertTrue(items[i-1] <= items[i]);
        }
    }


    @Fuzz
    public void dualPivotQuicksort(int @Size(min=200, max=200)[] items) {
        // Sort using DualPivotQuicksort
        Arrays.sort(items);

        // Assert sorted
        for (int i = 1; i < items.length; i++) {
            Assert.assertTrue(items[i-1] <= items[i]);
        }
    }

    @Fuzz
    public void smallArraySort(int @Size(min=20, max=20)[] items){

        int comps = 0;

        for (int i = 1; i < items.length; i ++){
            int key = items[i];
            int j = i-1;
            while (j >=0 && (items[j] > key)){
                comps++;
                items[j+1]= items[j];
                j--;
            }
            items[j+1] = key;
        }

        //System.out.println("comps: " + comps);
        Assert.assertTrue(comps != 20*19/2);

        for (int i=1; i < items.length; i++){
            Assert.assertTrue(items[i-1] <= items[i]);
        }
    }

    @Fuzz
    public void insertionSort(int @Size(min=20, max=20)[] items) {
        for (int i = 1; i < items.length; i ++){
            int key = items[i];
            int j = i-1;
            while (j >=0 && (items[j] > key)){
                items[j+1]= items[j];
                j--;
            }
            items[j+1] = key;
        }

        // Assert sorted
        for (int i = 1; i < items.length; i++) {
            Assert.assertTrue(items[i-1] <= items[i]);
        }
    }

    @Fuzz
    public void allSorts(int @Size(min=20, max= 400)[] items){
        Arrays.sort(items);
    }


    @Fuzz
    public void conditionalSort(boolean useInsertion, int @Size(min=100, max=100)[] items) {
        if (useInsertion) {
            dualPivotQuicksort(items);
        } else {
            for (int i = 0; i < 2000; i++) {
                items[0] = items[0];
            }
        }
    }


}
