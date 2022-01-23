import java.util.*;

public class DeboardEvent implements Event 
{
    static { LogJson.registerEvent(DeboardEvent.class, "Deboard"); }
    
    public final Passenger p; public final Train t; public final Station s;
    
    public DeboardEvent(Passenger p, Train t, Station s) 
    {
        this.p = p; this.t = t; this.s = s;
    }
    
    public boolean equals(Object o) 
    {
        if (o instanceof DeboardEvent e) 
        {
            return p.equals(e.p) && t.equals(e.t) && s.equals(e.s);
        }
        return false;
    }
    
    public int hashCode() 
    {
        return Objects.hash(p, t, s);
    }
    public String toString() 
    {
        return "Passenger " + p + " deboards " + t + " at " + s;
    }
    public List<String> toStringList() 
    {
        return List.of(p.toString(), t.toString(), s.toString());
    }
    public void replayAndCheck(Transit mbta) 
    {
	    if (!mbta.passengers.contains(p)) 
	    {
		    System.out.println("Couldn't find passenger " + p.toString() + " on the mbta.");
		    throw new IllegalStateException();
	    }
	    if (!mbta.stations.contains(s)) 
	    {
		    System.out.println("Couldn't find station " + s.toString() + " on the mbta.");
		    throw new IllegalStateException();
	    }
	    if (!mbta.trains.contains(t)) 
	    {
		    System.out.println("Couldn't find train " + t.toString() + " on the mbta.");
		    throw new IllegalStateException();
	    }
	    if (!mbta.scheduler.lines.containsKey(t.toString())) 
	    {
		    System.out.println("Couldn't find the line for train " + t.toString() + " on the mbta.");
		    throw new IllegalStateException();
	    }
	  
	    // check the station names in the line list to make sure the station is actually on the line
	    boolean foundStationOnLine = false;
	    for (String stationName : mbta.scheduler.lines.get(t.toString()))
		    if (stationName.compareTo(s.toString()) == 0)
		        foundStationOnLine = true;

	    if (!foundStationOnLine) 
	    {
		    System.out.println("Couldn't find the station " + s.toString() + " on the " + t.toString() + " line.");
		    throw new IllegalStateException();
	    }
	  
	    //make sure each passenger boards at the right station of their journey
	    boolean foundRightStation = false;
	    if (mbta.scheduler.trips.get(p.toString()).get(0).compareTo(s.toString()) != 0) 
	    {
	  	    int size = mbta.scheduler.trips.get(p.toString()).size();
	  	    int i = 1;
		    while (size > i) 
		    {
			    // lets check to see if it starts at the next station then
		  	    String nextStationOnJourney = mbta.scheduler.trips.get(p.toString()).get(i++);
			    if (nextStationOnJourney.compareTo(s.toString()) == 0) foundRightStation = true;
		    }
		    if (!foundRightStation) 
		    {
			    System.out.println("Passenger boarded at the wrong intermediate station.");
			    System.out.println("Couldn't find station " + s.toString() + " in passenger " + p.toString() + "'s trip.");
			    System.out.println("Passenger: " + p.toString() + " at station " + s.toString());
			    throw new IllegalStateException();
		    }
	    } 
	}
}
