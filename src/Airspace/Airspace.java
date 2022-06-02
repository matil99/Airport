package Airspace;

import Others.Plane;
import Others.PlaneFuelComparator;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Random;

public class Airspace
{
    protected float timeToNext;
    protected int currentId;
    protected PriorityQueue<Plane> landingQueue;
    protected Random random;
    public Airspace()
    {
        this.currentId = 0;
        this.random = new Random();
        this.landingQueue = new PriorityQueue<>(new PlaneFuelComparator());
        this.timeToNext = random.nextInt(29)+1;
    }
    public Plane appear(float currentTime)
    {
        timeToNext = random.nextInt(29)+1 + currentTime;
        currentId = currentId + 1;
        int type = random.nextInt(2);
        float fuel = random.nextInt(199)+1;
        float duration = random.nextInt(49)+1;
        Plane plane = new Plane(currentId, type, duration, fuel);
        landingQueue.add(plane);
        return plane;
    }
    public Plane land()
    {
        return landingQueue.poll();
    }
    public void forward(Plane plane)
    {
        landingQueue.remove(plane);
    }
    public void updateFuel(float time)
    {
        for (Plane p : landingQueue )
        {
            p.setFuel(p.getFuel() - time);
        }
    }
    public boolean needEmergencyLanding()
    {
        if (landingQueue.size() != 0)
        {
            return landingQueue.peek().getFuel() < 0;
        }
        else
        {
            return false;
        }
    }
    public int getSpecialCount()
    {
        int specialCounter = 0;
        for (Iterator<Plane> it = landingQueue.iterator(); it.hasNext(); )
        {
            Plane plane = it.next();
            if (plane.getType() == 1)
            {
                specialCounter++;
            }
        }
        return specialCounter;
    }
    public int getPassengerCount()
    {
        int paseengerCounter = 0;
        for (Iterator<Plane> it = landingQueue.iterator(); it.hasNext(); )
        {
            Plane plane = it.next();
            if (plane.getType() == 0)
            {
                paseengerCounter++;
            }
        }
        return paseengerCounter;
    }
    public float getTimeToNext()
    {
        return timeToNext;
    }
    public float getDuration(){return landingQueue.peek().getDuration();}
}
