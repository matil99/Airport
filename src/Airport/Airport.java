package Airport;

import Others.Plane;
import Others.PlaneStartTimeComparator;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Random;

public class Airport extends JFrame
{
    private PriorityQueue<Plane> takeOffQueue = new PriorityQueue<>(new PlaneStartTimeComparator());
    private ArrayList<Plane> passengerTerminal = new ArrayList<>();
    private ArrayList<Plane> specialTerminal = new ArrayList<>();
    private Random random = new Random();
    private int maxPassengerPlanes;
    private int maxSpecialPlanes;
    private boolean free;
    private JLabel lPassengerTerminal, lSpecialTerminal, lStartSchedule;
    private ArrayList<JLabel> lStarts= new ArrayList<>();
    private ArrayList<JLabel> lStartsTime= new ArrayList<>();

    public Airport(int maxPassengerPlanes, int maxSpecialPlanes)
    {
        this.maxPassengerPlanes = maxPassengerPlanes;
        this.maxSpecialPlanes = maxSpecialPlanes;
        this.free = true;
        this.init();
    }
    public void land(Plane plane, float time) throws InterruptedException
    {
        free = false;
        this.repaint();
        Thread.sleep(250);
        if (plane.getType() == 0)
        {
            passengerTerminal.add(plane);
        }
        if (plane.getType() == 1)
        {
            specialTerminal.add(plane);
        }
        int takeOffTime = random.nextInt(29)+1;
        plane.setStartTime(time + takeOffTime);
        plane.setFuel(0);
        takeOffQueue.add(plane);
        free = true;
        this.repaint();
    }
    public void takeOff() throws InterruptedException
    {
        free = false;
        this.repaint();
        Thread.sleep(250);
        Plane plane = takeOffQueue.poll();
        if (plane.getType() == 0)
        {
            passengerTerminal.remove(plane);
        }
        if (plane.getType() == 1)
        {
            specialTerminal.remove(plane);
        }
        System.out.println("Start samolotu: " + plane);
        this.repaint();
    }
    public float getStartTime()
    {
        if (takeOffQueue.size() != 0)
        {
        return takeOffQueue.peek().getStartTime();
        }
        else
        {
            return Integer.MAX_VALUE;
        }
    }
    public void init()
    {
        setSize(1080,680);
        setTitle("Airport");
        setLayout(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        setResizable(false);

        lPassengerTerminal = new JLabel("Terminal pasażerski [" + passengerTerminal.size() + "/" + maxPassengerPlanes + "]");
        lPassengerTerminal.setBounds(50,50,250,50);
        add(lPassengerTerminal);

        lSpecialTerminal = new JLabel("Terminal specjalny [" + specialTerminal.size() + "/" + maxPassengerPlanes + "]");
        lSpecialTerminal.setBounds(350,50,250,50);
        add(lSpecialTerminal);

        lStartSchedule = new JLabel("Najbliższe starty");
        lStartSchedule.setBounds(650,50,250,50);
        add(lStartSchedule);
        Border blackline = BorderFactory.createLineBorder(Color.black);
        for (int i = 0; i < 15; i++)
        {
            lStarts.add(new JLabel( i + ": "));
            lStartsTime.add(new JLabel());
            lStarts.get(i).setBounds(650, i*25 + 100, 250, 25);
            lStartsTime.get(i).setBounds(900, i*25 + 100, 100, 25);
            lStarts.get(i).setBorder(blackline);
            lStartsTime.get(i).setBorder(blackline);
            add(lStarts.get(i));
            add(lStartsTime.get(i));
        }
    }
    public void paint(Graphics g)
    {
        super.paint(g);
        drawTerminal(g);
        updateTerminal(g);
        updateStartSchedule(g);
        drawAirStrip(g);
        updateLights(g);
    }
    public void drawTerminal(Graphics g)
    {
        g.setColor(Color.BLACK);
        g.drawRect(50,150,250,100); /*Terminal pasażerski - pusty*/
        g.drawRect(350,150,250,100); /*Terminal specjalny - pusty*/
    }
    public void updateTerminal(Graphics g)
    {
        int heightPassenger, heightSpecial;
        g.setColor(Color.GREEN);
        heightPassenger  = (int)(((float)passengerTerminal.size()/(float)maxPassengerPlanes)*100); /*Wysokość według zajętości terminala*/
        heightSpecial = (int)(((float)specialTerminal.size()/(float)maxSpecialPlanes)*100);
        lPassengerTerminal.setText("Terminal pasażerski [" + passengerTerminal.size() + "/" + maxPassengerPlanes + "]");
        lSpecialTerminal.setText("Terminal specjalny [" + specialTerminal.size() + "/" + maxPassengerPlanes + "]");
        g.fillRect(50,150,250, heightPassenger);
        g.fillRect(350,150,250, heightSpecial);
    }
    public void updateStartSchedule(Graphics g)
    {
        PriorityQueue<Plane> tmp = new PriorityQueue<>(new PlaneStartTimeComparator());
        for (Iterator<Plane> it = takeOffQueue.iterator(); it.hasNext(); )
        {
            Plane p = it.next();
            tmp.add(p);
        }
        for (int i = 0; i < lStarts.size(); i++)
        {
            Plane plane = tmp.poll();
            if (plane != null)
            {
                lStarts.get(i).setText(i + ": " + plane);
                lStartsTime.get(i).setText("" + plane.getStartTime());
            }
            else
            {
                lStarts.get(i).setText(i + ": ");
                lStartsTime.get(i).setText("");
            }
        }
    }
    public void drawAirStrip(Graphics g)
    {
        g.setColor(Color.BLACK);
        g.fillRect(50,350,550, 50);
        g.setColor(Color.WHITE);
        for (int i = 0; i < 10; i++)
        {
            g.fillRect(75 + i*50, 375, 25, 5);
        }
        g.setColor(Color.BLACK);
        g.drawRect(50,400,50,50);
        g.drawRect(100,400,50,50);
        g.setColor(Color.WHITE);
        g.fillRect(50,400,50,50);
        g.fillRect(100,400,50,50);
        g.setColor(Color.BLACK);
        g.drawOval(50,400,50,50);
        g.drawOval(100,400,50,50);
    }
    public void updateLights(Graphics g)
    {
        if (free)
        {
            g.setColor(Color.GREEN);
            g.fillOval(50,400,50,50);
            g.setColor(Color.WHITE);
            g.fillOval(100,400,50,50);
        }
        else
        {
            g.setColor(Color.WHITE);
            g.fillOval(50,400,50,50);
            g.setColor(Color.RED);
            g.fillOval(100,400,50,50);
        }
    }

}
