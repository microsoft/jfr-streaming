// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.package com.microsoft.jfr;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.IntStream;

/**
 * This class uses Fibonacci number generation to produce some load on the CPU.
 */
public class LoadGenerator {

    // The result of the calculations are kept in this cache. This helps drive up
    // some memory use, especially if the range is significant. Each time
    // generateLoad() is called, the cache is recreated which may cause some
    // garbage collection activity (depending on the heap size).
    private ArrayList<BigInteger> cache;

    // This just keeps track of the index of the last Fibbonacci number in cache.
    private int lastFibonacciIndex;

    // How many Fibbonacci numbers to calculate. This can be set as an command line argument.
    private int range = 100;

    /**
     * In order to keep Main simple, the constructor just takes the array of
     * arguments passed on the command line.
     * @param args The command line arguments passed from Main.
     */
    public LoadGenerator (String[] args) {
        try {
            range = Integer.parseInt(args[0]);
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
        }
    }

    /**
     * This is the method that generates the load. It uses fork-join generate load on the system.
     * Each time this method is called, a new cache for the Fibonacci numbers is created. Calling
     * this method repeatedly cause garabage collections if the heap is small enough.
     */
    public void generateLoad () {
        cache = new ArrayList<>(Collections.nCopies(range + 1, BigInteger.ZERO));
        cache.set(0, BigInteger.ZERO);
        cache.set(1, BigInteger.ONE);
        lastFibonacciIndex = 1;

        // Each value in `stream` gets its own thread to run
        IntStream.range(0, range)
                .parallel()
                .forEach(this::nthFibonacci);
    }

    private BigInteger nthFibonacci (int n) {
        if (lastFibonacciIndex >= n) return cache.get(n);
        for(int i = lastFibonacciIndex + 1; i <= n; i++){
            cache.set(i, cache.get(i - 1).add(cache.get(i - 2)));
        }
        lastFibonacciIndex = n;
        return cache.get(n);
    }

}
