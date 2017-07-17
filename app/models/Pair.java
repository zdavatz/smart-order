package models;

/**
 * Implements a Pair class
 *
 * @param <T>
 * @param <U>
 * @author Max
 */

public class Pair<T, U> {
    public final T first;
    public final U second;

    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }
}
