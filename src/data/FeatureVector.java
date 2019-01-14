package data;

import java.util.HashMap;
import java.util.Objects;

public class FeatureVector {
    public HashMap<String, String> from;
    public HashMap<String,String> to;
    public String label;

    public FeatureVector(Event from, Event to){
        this.from = from.payload;
        this.to = to.payload;
        this.label = null;
    }

    public FeatureVector(HashMap<String, String> from, HashMap<String, String> to){
        this.from = new HashMap<>();
        this.to = new HashMap<>();
        for(String attribute: from.keySet())
            this.from.put(attribute, from.get(attribute));
        if(to != null){
            for(String attribute: to.keySet())
                this.to.put(attribute, to.get(attribute));
        }
        else
            this.to = null;
    }

    public String toString(){
        return from + " => " + to + " label = " + label;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof FeatureVector)) {
            return false;
        }
        FeatureVector fv = (FeatureVector) o;
        return fv.from.equals(this.from) && fv.to.equals(this.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}