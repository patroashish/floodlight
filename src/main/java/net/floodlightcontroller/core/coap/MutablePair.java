package net.floodlightcontroller.core.coap;

public class MutablePair<X, Y> { 
    public X fst; 
    public Y snd; 
    public MutablePair(X x, Y y) { 
        this.fst = x; 
        this.snd = y; 
    }

    @Override
    public String toString() {
        return "(" + fst + "," + snd + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof MutablePair)){
            return false;
        }
        MutablePair<X,Y> other_ = (MutablePair<X,Y>) other;
        return other_.fst == this.fst && other_.snd == this.snd;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fst == null) ? 0 : fst.hashCode());
        result = prime * result + ((snd == null) ? 0 : snd.hashCode());
        return result;
    }
}