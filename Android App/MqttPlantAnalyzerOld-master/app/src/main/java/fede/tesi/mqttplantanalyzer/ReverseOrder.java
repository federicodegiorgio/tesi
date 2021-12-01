package fede.tesi.mqttplantanalyzer;

import java.util.Comparator;

public class ReverseOrder<T> implements Comparator<T> {
    private Comparator<T> delegate;
    public ReverseOrder(Comparator<T> delegate){
        this.delegate = delegate;
    }

    public int compare(T a, T b) {
        //reverse order of a and b!!!
        return this.delegate.compare(b,a);
    }
}