package ControlTower;

public class ControlTower
{
    protected float maxDelay;
    protected int forwardedPlanes;
    protected int landingCount;
    protected int emergencyCount;

    public ControlTower()
    {
        this.maxDelay = 0;
        this.forwardedPlanes = 0;
        this.landingCount = 0;
        this.emergencyCount = 0;
    }
}
