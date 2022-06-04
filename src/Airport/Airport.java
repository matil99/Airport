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
    private int planesCount;
    protected int direction;

    protected float maxDelay;
    protected int forwardedPlanes;
    protected int landingCount;
    protected int emergencyCount;

    /*HLA Airstrip*/
    protected boolean free;
    private int maxPassengerPlanes;
    private int maxSpecialPlanes;
    private float releaseTime;

    /*GUI variables*/
    private JLabel lPassengerTerminal, lSpecialTerminal, lStartSchedule;
    private ArrayList<JLabel> lStarts= new ArrayList<>();
    private ArrayList<JLabel> lStartsTime= new ArrayList<>();
    private ArrayList<JLabel> lStatsTitle = new ArrayList<>();
    private ArrayList<JLabel> lStatsValue = new ArrayList<>();

    public Airport(int maxPassengerPlanes, int maxSpecialPlanes, int planesInQueue)
    {
        this.maxPassengerPlanes = maxPassengerPlanes;
        this.maxSpecialPlanes = maxSpecialPlanes;
        this.planesCount = 1;
        this.free = false;
        this.direction = 0;
        this.maxDelay = 0;
        this.forwardedPlanes = 0;
        this.landingCount = 0;
        this.emergencyCount = 0;
        int  n = random.nextInt(1000) + 500;
        for (int i = n; i < n + planesInQueue; i++)
        {
            Plane plane = new Plane(i, random.nextInt(2), 50, 0);
            plane.setStartTime(planesCount * 75);
            planesCount++;
            addPlane(plane);
        }
        this.releaseTime = 0;
        this.init();
    }
    public boolean getFree()
    {
        return free;
    }
    public float getReleaseTime()
    {
        return releaseTime;
    }
    public float getTakeOffTime()
    {
        if (takeOffQueue.size() != 0)
        {
            return takeOffQueue.peek().getStartTime();
        }
        else
        {
            return 0;
        }
    }
    public int getAvailablePassenger()
    {
        int available = maxPassengerPlanes - passengerTerminal.size();
        if (available > 0) return available;
        else return 0;

    }
    public int getAvailableSpecial()
    {
        int available = maxSpecialPlanes - specialTerminal.size();
        if (available > 0) return available;
        else return 0;
    }
    private void addPlane(Plane plane)
    {
        if (plane.getType() == 0)
        {
            passengerTerminal.add(plane);
        }
        if (plane.getType() == 1)
        {
            specialTerminal.add(plane);
        }
        takeOffQueue.add(plane);
    }
    public void land(Plane plane, float time, float duration)
    {
        releaseTime = time + duration;
        if (plane.getType() == 0)
        {
            passengerTerminal.add(plane);
        }
        if (plane.getType() == 1)
        {
            specialTerminal.add(plane);
        }
        int takeOffTime = (planesCount+1) * 100;
        int takeOffDuration = 50;
        plane.setStartTime(takeOffTime);
        plane.setDuration(takeOffDuration);
        plane.setFuel(0);
        takeOffQueue.add(plane);
        planesCount++;
        this.repaint();
    }
    public void release()
    {
        free = true;
        direction = 0;
        repaint();
    }
    public int getTakeOffQueueSize()
    {
        return takeOffQueue.size();
    }
    public Plane takeOff(float time)
    {
        direction = -1;
        Plane plane = takeOffQueue.poll();
        releaseTime = time + plane.getDuration();
        if (plane.getType() == 0)
        {
            passengerTerminal.remove(plane);
        }
        if (plane.getType() == 1)
        {
            specialTerminal.remove(plane);
        }
        this.repaint();
        return plane;
    }

    /*GUI methods*/
    public void init()
    {
        Color color1=new Color(255,217,230);
        setSize(1080,680);
        setTitle("Airport");
        getContentPane().setBackground(color1);
        setLayout(null);
        setBackground(color1);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        setResizable(false);

        Font font1 = new Font("SansSerif", Font.BOLD, 18);
        lPassengerTerminal = new JLabel("Terminal pasażerski [" + passengerTerminal.size() + "/" + maxPassengerPlanes + "]", JLabel.CENTER);
        lPassengerTerminal.setBounds(50,50,250,50);
        lPassengerTerminal.setFont(font1);
        add(lPassengerTerminal);

        lSpecialTerminal = new JLabel("Terminal specjalny [" + specialTerminal.size() + "/" + maxPassengerPlanes + "]", JLabel.CENTER);
        lSpecialTerminal.setBounds(350,50,250,50);
        lSpecialTerminal.setFont(font1);
        add(lSpecialTerminal);


        lStartSchedule = new JLabel("Najbliższe starty", JLabel.CENTER);
        lStartSchedule.setForeground(Color.red);
        lStartSchedule.setFont(font1);
        lStartSchedule.setBounds(650,50,250,50);
        add(lStartSchedule);
        Border blackline = BorderFactory.createLineBorder(Color.black);
        Border redline = BorderFactory.createLineBorder(Color.red);
        for (int i = 0; i < 13; i++)
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
        for (int i = 0; i < 4; i++) /*Budowa tabelki na statystyki*/
        {
            lStatsTitle.add(new JLabel());
            lStatsValue.add(new JLabel());
            lStatsTitle.get(i).setBounds(49 + i*238, 460, 238, 40);
            lStatsValue.get(i).setBounds(49 + i*238, 500, 238, 40);
            lStatsTitle.get(i).setBorder(blackline);
            lStatsValue.get(i).setBorder(blackline);
            add(lStatsTitle.get(i));
            add(lStatsValue.get(i));
        }
        lStatsTitle.get(0).setText("Maksymalne opóźnienie");
        lStatsTitle.get(1).setText("Przekierowane samoloty");
        lStatsTitle.get(2).setText("Udane lądowania");
        lStatsTitle.get(3).setText("Awaryjne lądowania");


        lStatsTitle.get(0).setFont(font1);
        lStatsTitle.get(1).setFont(font1);
        lStatsTitle.get(2).setFont(font1);
        lStatsTitle.get(3).setFont(font1);
        lStatsValue.get(0).setFont(font1);
        lStatsValue.get(1).setFont(font1);
        lStatsValue.get(2).setFont(font1);
        lStatsValue.get(3).setFont(font1);
        lStatsTitle.get(0).setHorizontalAlignment(JTextField.CENTER);
        lStatsTitle.get(1).setHorizontalAlignment(JTextField.CENTER);
        lStatsTitle.get(2).setHorizontalAlignment(JTextField.CENTER);
        lStatsTitle.get(3).setHorizontalAlignment(JTextField.CENTER);
        lStatsValue.get(0).setHorizontalAlignment(JTextField.CENTER);
        lStatsValue.get(1).setHorizontalAlignment(JTextField.CENTER);
        lStatsValue.get(2).setHorizontalAlignment(JTextField.CENTER);
        lStatsValue.get(3).setHorizontalAlignment(JTextField.CENTER);
        //lStatsTitle.get(0).setBackground(Color.red);
        //lStatsTitle.get(0).setVisible(true);
        //lStatsTitle.get(0).setSize(150,20);
    }
    public void paint(Graphics g)
    {
        super.paint(g);
        drawTerminal(g);
        updateTerminal(g);
        updateStartSchedule(g);
        drawAirStrip(g);
        updateLights(g);
        updateDirection(g);
        updateStats(g);
    }
    public void drawTerminal(Graphics g)
    {
        g.setColor(Color.BLACK);
        g.drawRect(50,150,250,100); /*Terminal pasażerski - pusty*/
        g.drawRect(350,150,250,100); /*Terminal specjalny - pusty*/
        g.setColor(Color.WHITE);
        g.fillRect(50,150,250,100); /*Terminal pasażerski - pusty*/
        g.fillRect(350,150,250,100); /*Terminal specjalny - pusty*/
    }
    public void updateTerminal(Graphics g)
    {
        Color color2 = new Color(128, 0, 128);
        int heightPassenger, heightSpecial;
        g.setColor(color2);
        g.fillRect(50,150,250,100); /*Terminal pasażerski - pusty*/
        g.fillRect(350,150,250,100); /*Terminal specjalny - pusty*/
        g.setColor(Color.red);
        heightPassenger  = (int)(((float)passengerTerminal.size()/(float)maxPassengerPlanes)*100); /*Wysokość według zajętości terminala*/
        heightSpecial = (int)(((float)specialTerminal.size()/(float)maxSpecialPlanes)*100);
        lPassengerTerminal.setText("Terminal pasażerski [" + passengerTerminal.size() + "/" + maxPassengerPlanes + "]");
        lSpecialTerminal.setText("Terminal specjalny [" + specialTerminal.size() + "/" + maxSpecialPlanes + "]");
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
        Color color1=new Color(255,217,230);
        g.setColor(Color.BLACK);
        g.drawRect(50,400,100,50);
        g.setColor(color1);
        g.fillRect(50,400,100,50);
        if (free)
        {
            g.setColor(Color.GREEN);
            g.fillOval(50,400,50,50);

            g.setColor(color1);
            g.fillOval(100,400,50,50);
        }
        else
        {
            g.setColor(color1);
            g.fillOval(50,400,50,50);
            g.setColor(Color.RED);
            g.fillOval(100,400,50,50);
        }
    }
    public void updateDirection(Graphics g)
    {

        Color color1=new Color(255,217,230);
        g.setColor(color1);
        g.drawRect(550,400,50,50);
        g.setColor(color1);
        g.fillRect(550,400,50,50);
        if (direction == -1)
        {
            g.setColor(Color.GREEN);
            g.fillPolygon(new int[] {550, 575, 600}, new int[] {450, 400, 450}, 3);
        }
        if (direction == 1)
        {
            g.setColor(Color.GREEN);
            g.fillPolygon(new int[] {550, 575, 600}, new int[] {400, 450, 400}, 3);
        }
        if (direction == 2)
        {
            g.setColor(Color.ORANGE);
            g.fillPolygon(new int[] {550, 575, 600}, new int[] {400, 450, 400}, 3);
        }
        if (direction == 0)
        {
            g.setColor(color1);
            g.fillRect(550,400,50,50);
        }
    }
    public void updateStats(Graphics g)
    {
        lStatsValue.get(0).setText(String.valueOf(maxDelay));
        lStatsValue.get(1).setText(String.valueOf(forwardedPlanes));
        lStatsValue.get(2).setText(String.valueOf(landingCount));
        lStatsValue.get(3).setText(String.valueOf(emergencyCount));
    }
}
