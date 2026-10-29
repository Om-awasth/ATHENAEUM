package com.library.util;

import java.io.Serializable;
import java.util.Objects;

/**
 * Generic tuple utility class storing a key-value or heterogeneous pair.
 * [Concept: Generic Classes]
 *
 * @param <K> Type of first element
 * @param <V> Type of second element
 */
public class Pair<K, V> implements Serializable {

    private final K first;
    private final V second;

    public Pair(K first, V second) {
        this.first = first;
        this.second = second;
    }

    /**
     * [Concept: Generic Static Factory Method]
     */
    public static <A, B> Pair<A, B> of(A first, B second) {
        return new Pair<>(first, second);
    }

    public K getFirst() {
        return first;
    }

    public V getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair<?, ?> pair)) return false;
        return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", first, second);
    }
}
