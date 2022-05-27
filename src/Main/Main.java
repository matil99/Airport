package Main;

import Airport.Airport;
import Airspace.Airspace;
import Others.Plane;

public class Main {

    public static void main(String[] args) throws InterruptedException
    {
        Airport airport = new Airport(100, 100);
        Airspace airspace = new Airspace();
        float timeToNext = airspace.getTimeToNext();
        for (int i = 0; i < 1000; i++)
        {
            if (i >= timeToNext)
            {
                timeToNext = i + airspace.getTimeToNext();
                Plane p = airspace.appear();
                airport.land(p, i);
            }
            if (i >= airport.getStartTime())
            {
                airport.takeOff();
            }
            Thread.sleep(500);
        }
    }
}
