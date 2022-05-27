package Airspace;

import Others.Plane;
import Others.PlaneFuelComparator;

import java.util.PriorityQueue;
import java.util.Random;

public class Airspace
{
    private float timeToNext;
    private int currentId;
    private PriorityQueue<Plane> landingQueue;
    private Random random;
    public Airspace()
    {
        this.currentId = 0;
        this.random = new Random();
        this.landingQueue = new PriorityQueue<>(new PlaneFuelComparator());
        this.timeToNext = random.nextInt(29)+1;
    }
    public Plane appear()
    {
        timeToNext = random.nextInt(29)+1;
        currentId = currentId + 1;
        int type = random.nextInt(2);
        float fuel = random.nextInt(59)+1;
        Plane plane = new Plane(currentId, type, fuel);
        landingQueue.add(plane);
        return plane;
    }
    public Plane land()
    {
        Plane plane = landingQueue.poll();
        return plane;
    }
    public void forward(Plane plane)
    {
        landingQueue.remove(plane);
    }
    public void updateFuel(float time)
    {
        for (Plane p : landingQueue )
        {
            System.out.println(p);
        }
        System.out.println("\n");
    }
    public int getLandingQueueSize()
    {
        return landingQueue.size();
    }
    public float getTimeToNext()
    {
        return timeToNext;
    }
}
