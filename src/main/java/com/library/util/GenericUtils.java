package com.library.util;

import com.library.model.LibraryItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class containing generic methods with bounded type parameters and wildcards.
 * [Concept: Generics - Bounded Type Parameters]
 * [Concept: Generics - Wildcards (? extends Type)]
 */
public final class GenericUtils {

    private GenericUtils() {
        // Prevent instantiation
    }

    /**
     * [Concept: Generic Method with Upper Bounded Type Parameter]
     * Accepts a list of any subtype of LibraryItem and returns only those currently available for loan.
     *
     * @param items list of items (Book, Magazine, Journal, etc.)
     * @param <T> bounded subtype of LibraryItem
     * @return filtered list containing only items with availableCopies > 0
     */
    public static <T extends LibraryItem> List<T> filterAvailable(List<T> items) {
        if (items == null) return Collections.emptyList();
        List<T> available = new ArrayList<>();
        for (T item : items) {
            if (item.isAvailable()) {
                available.add(item);
            }
        }
        return available;
    }

    /**
     * [Concept: Wildcards (? extends LibraryItem)]
     * Computes the cumulative fine if all given items were overdue by a specific number of days.
     * Demonstrates read-only access through upper-bounded wildcards.
     *
     * @param items list of unknown subtypes of LibraryItem
     * @param days overdue days to simulate
     * @return total fine across items
     */
    public static double calculateSimulatedFines(List<? extends LibraryItem> items, long days) {
        if (items == null) return 0.0;
        double totalFine = 0.0;
        for (LibraryItem item : items) {
            totalFine += item.calculateFine(days);
        }
        return totalFine;
    }

    /**
     * [Concept: Generic Sorting Method with Comparator]
     */
    public static <T> void sortList(List<T> list, Comparator<? super T> comparator) {
        if (list != null && comparator != null) {
            Collections.sort(list, comparator);
        }
    }
}
