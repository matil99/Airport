package Others;

public class Plane
{
    private final int id;
    private final int type;
    private float duration;
    private float fuel;
    private float startTime;
    public Plane(int id, int type,float duration, float fuel)
    {
        this.id = id;
        this.type = type;
        this.duration = duration;
        this.fuel = fuel;
        this.startTime = 0;
    }
    public int getId()
    {
        return this.id;
    }
    public int getType()
    {
        return this.type;
    }
    public float getDuration() {return this.duration;}
    public float getFuel()
    {
        return this.fuel;
    }
    public void setFuel(float fuel) {this.fuel = fuel;}
    public void setDuration(float duration){this.duration = duration;}
    public float getStartTime()
    {
        return this.startTime;
    }
    public void setStartTime(float startTime)
    {
        this.startTime = startTime;
    }
    @Override
    public String toString()
    {
        if (this.type == 0) return "Passenger plane number " + this.id;
        if (this.type == 1) return "Special plane number " + this.id;
        return "Other type " + this.id;
    }
}
