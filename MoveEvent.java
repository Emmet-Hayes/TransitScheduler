import java.util.*;
import java.util.Map.Entry;

public class MoveEvent implements Event 
{
    static { LogJson.registerEvent(MoveEvent.class, "Move"); }

    public final Train t; public final Station s1, s2;
    
    public MoveEvent(Train t, Station s1, Station s2) 
    {
        this.t = t; this.s1 = s1; this.s2 = s2;
    }
  
    public boolean equals(Object o) 
    {
        if (o instanceof MoveEvent e)
            return t.equals(e.t) && s1.equals(e.s1) && s2.equals(e.s2);
        return false;
    }
  
    public int hashCode() { return Objects.hash(t, s1, s2); }
  
    public String toString() { return "Train " + t + " moves from " + s1 + " to " + s2; }
  
    public List<String> toStringList() { return List.of(t.toString(), s1.toString(), s2.toString()); }
    
    public void replayAndCheck(Transit mbta) 
    {
	    if (!mbta.stations.contains(s1)) 
	    {
	  	    System.out.println("Couldn't find point A station.");
		    throw new IllegalStateException();
	    }
	    if (!mbta.stations.contains(s2)) 
	    {
		    System.out.println("Couldnt' find point B station. ");
		    throw new IllegalStateException();
	    }
	    if (!mbta.trains.contains(t)) 
	    {
		    System.out.println("Couldn't find train. ");
		    throw new IllegalStateException();
	    }
	    if (!mbta.scheduler.lines.containsKey(t.toString())) 
	    {
		    System.out.println("Couldn't find the line. ");
		    throw new IllegalStateException();
	    }
	  
	    boolean foundStation1OnLine = false;
	    boolean foundStation2OnLine = false;
	    // check the station names in the line list to make sure the station is actually on the line
	    for (String stationName : mbta.scheduler.lines.get(t.toString())) 
	    {
		    if (stationName.compareTo(s1.toString()) == 0)
		        foundStation1OnLine = true;
		    if (stationName.compareTo(s2.toString()) == 0)
			  foundStation2OnLine = true;
        }
	    if (!foundStation1OnLine) 
	    {
		    System.out.println("Couldn't find the current station on the line.");
		    throw new IllegalStateException();
	    }
	    if (!foundStation1OnLine) 
	    {
		    System.out.println("Couldn't find the next station on the line.");
		    throw new IllegalStateException();
	    }
	  
	    //make sure each train starts at the initial start and end points of their journey
	    String resultInitStation = mbta.scheduler.lines.get(t.toString()).get(0);
	    String expectedInitStation = TransitGlobalCache.trainStartStationCache.get(t).toString();
	    String expectedOtherInitStation = TransitGlobalCache.trainEndStationCache.get(t).toString();
	    
	    if (resultInitStation.compareTo(expectedInitStation) != 0  && 
	  	    resultInitStation.compareTo(expectedOtherInitStation) != 0) 
	    {
		    System.out.println("Train started at the wrong station: " + resultInitStation);
		    System.out.println("Expected: " + expectedInitStation + " or " + expectedOtherInitStation);
		    throw new IllegalStateException();
	    }
	  
	    //make sure train loops back around if were at the end
	    int endIndex = mbta.scheduler.lines.get(t.toString()).size() - 1;
	    if (mbta.scheduler.lines.get(t.toString()).get(endIndex).compareTo(s1.toString()) == 0) 
	    {
		    //and were not looping back around to the second to last station, 
		    if (mbta.scheduler.lines.get(t.toString()).get(endIndex - 1).compareTo(s2.toString()) != 0) {
			    //if the line already loops around, lets not worry about it
			    if (mbta.scheduler.lines.get(t.toString()).get(endIndex).compareTo(mbta.scheduler.lines.get(t.toString()).get(0)) != 0)
			    	throw new IllegalStateException();
		    }
	    }
    }
}
