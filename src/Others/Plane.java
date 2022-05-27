package Others;

public class Plane
{
    private int id;
    private int type;
    private float fuel;
    private float startTime;
    public Plane(int id, int type, float fuel)
    {
        this.id = id;
        this.type = type;
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
    public float getFuel()
    {
        return this.fuel;
    }
    public void setFuel(float fuel) {this.fuel = fuel;}
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
