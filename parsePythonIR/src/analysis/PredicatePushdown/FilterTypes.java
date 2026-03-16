package analysis.PredicatePushdown;

import java.util.List;

public class FilterTypes {
    List<String> AndSet;
    List<String> OrSet;

    public FilterTypes(List<String> andSet, List<String> orSet) {
        AndSet = andSet;
        OrSet = orSet;
    }

    public List<String> getAndSet() {
        return AndSet;
    }

    public void setAndSet(List<String> andSet) {
        AndSet = andSet;
    }

    public List<String> getOrSet() {
        return OrSet;
    }

    public void setOrSet(List<String> orSet) {
        OrSet = orSet;
    }
}
