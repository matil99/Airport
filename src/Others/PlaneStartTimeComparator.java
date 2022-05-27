package Others;

import java.util.Comparator;

public class PlaneStartTimeComparator implements Comparator<Plane> {
    @Override
    public int compare(Plane o1, Plane o2) {
        if (o1.getStartTime() > o2.getStartTime()) return 1;
        else if (o1.getStartTime() < o2.getStartTime()) return -1;
        else return 0;
    }
}
