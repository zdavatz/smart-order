package models;

/**
 * Created by maxl on 10.03.2017.
 */
public class Pair<T, U> {
    public final T first;
    public final U second;

    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }
}
