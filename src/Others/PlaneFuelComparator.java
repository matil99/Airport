package Others;

import java.util.Comparator;

public class PlaneFuelComparator implements Comparator<Plane> {
    @Override
    public int compare(Plane o1, Plane o2) {
        if (o1.getFuel() >= o2.getFuel()) return 1;
        else return -1;
    }
}
