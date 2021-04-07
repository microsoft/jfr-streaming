package com.microsoft.jfr;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.IntStream;

/**
 * This class uses Fibonacci number generation to produce some load
 */
public class LoadGenerator {
    private static ArrayList<BigInteger> cache;
    private static int lastFibonacciIndex;
    private static int range = 100;

    private BigInteger nthFibonacci (int n) {
        if (lastFibonacciIndex >= n) return cache.get(n);
        for(int i = lastFibonacciIndex + 1; i <= n; i++){
            cache.set(i, cache.get(i - 1).add(cache.get(i - 2)));
        }
        lastFibonacciIndex = n;
        return cache.get(n);
    }

    public LoadGenerator (String[] args) {
        try {
            range = Integer.parseInt(args[0]);
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            // IndexOutOfBoundsException happens when args is empty, which means no argument was passed
            // NumberFormatException happens when args[0] is not a number
            // In both cases we use the default value for `range` as 100
        }
    }

    public void generateLoad () {
        cache = new ArrayList<>(Collections.nCopies(range + 1, BigInteger.ZERO));
        cache.set(0, BigInteger.ZERO);
        cache.set(1, BigInteger.ONE);
        lastFibonacciIndex = 1;

        // Each value in `stream` gets its own thread to run
        IntStream.range(0, range)
                .parallel()
                .forEach(i ->
                        System.out.println( i + "th Fibonacci number: " + nthFibonacci(i).toString()));
    }
}
