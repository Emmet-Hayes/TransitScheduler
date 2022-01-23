import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.*;

public class Transit 
{
	public TransitScheduler scheduler = new TransitScheduler();
	public List<Station> stations = new LinkedList<>();
	public List<Passenger> passengers = new LinkedList<>();
	public List<Train> trains = new LinkedList<>();
  
    // Adds a new transit line with given name and stations
    public void addLine(String name, List<String> stations) { scheduler.lines.put(name, stations); }

    // Adds a new planned journey to the simulation
    public void addJourney(String name, List<String> stations) { scheduler.trips.put(name, stations); }

    // Return normally if initial simulation conditions are satisfied, otherwise
    // raises an exception
    public void checkStart() 
    {
	    for (Train t : trains) 
	    {
		    if (!TransitGlobalCache.trainStartStationCache.containsKey(t)) 
		    {
			    System.out.println("Missing starting train in cache: " + t.toString());
			    System.out.println("Wahts in this then: " + TransitGlobalCache.trainStartStationCache);
			    throw new IllegalStateException();
		    }
		  
		    Station s = TransitGlobalCache.trainStartStationCache.get(t);
		    if (!scheduler.lines.containsKey(t.toString())) 
		    {
			    System.out.println("Missing a train somehow: " + t.toString());
			    throw new IllegalStateException();
		    }
		  
		    int lengthOfLine = scheduler.lines.get(t.toString()).size();
		    String str = TransitGlobalCache.trainStartStationCache.get(t).toString();
		    String start = scheduler.lines.get(t.toString()).get(0);
		    String end = scheduler.lines.get(t.toString()).get(lengthOfLine - 1);
		    //System.out.println("Start: " + start + " End: " + end + " Str: " + str);
		    // if were not starting here or at the end then something is wrong
		    if (!(str.compareTo(start) == 0 || str.compareTo(end) == 0)) 
		    {
			    System.out.println("Starting the train at the wrong station" + t.toString());
			    System.out.println("Expected:" + TransitGlobalCache.trainStartStationCache.get(t).toString());
			    //System.out.println("Result: " + scheduler.lines.get(t.toString()).get(0));
			    System.out.println("compare results: " + str.compareTo(start) + str.compareTo(end));
			    throw new IllegalStateException();
		    }
	    }
	  
	    for (Passenger p : passengers) 
	    {
		    if (!TransitGlobalCache.passengerStartStationCache.containsKey(p)) 
		    {
			    System.out.println("Missing passenger " + p.toString() + " in cache.");
			    throw new IllegalStateException();
		    }
		  
		    Station s = TransitGlobalCache.passengerStartStationCache.get(p);
		    if (!scheduler.trips.containsKey(p.toString())) 
		    {
			    System.out.println("Missing a passenger in the scheudler.");
			    throw new IllegalStateException();
		    }
		  
		    // if were not starting here then were likely doing it wrong
		    if (TransitGlobalCache.passengerStartStationCache.get(p).toString().compareTo(scheduler.trips.get(p.toString()).get(0)) != 0) {
			    //if there is more than 2 stops in the journey, lets not throw the exception
			    if (!(TransitGlobalCache.passengerMapToJourney.size() > 2)) 
			    {
				    System.out.println("Passenger not starting at right station..");
				    System.out.println("Expected: " + TransitGlobalCache.passengerStartStationCache.get(p).toString());
				    System.out.println("Result: " + scheduler.trips.get(p.toString()).get(0));
				    throw new IllegalStateException();
			    }
		    }
	    }
    }

    // Return normally if final simulation conditions are satisfied, otherwise
    // raises an exception
    public void checkEnd() 
    {
	    // check if passengers are at the ends of their journeys
	    for (Passenger p : passengers) 
	    {
		    if (!TransitGlobalCache.passengerEndStationCache.containsKey(p)) 
		    {
			    System.out.println("Missing passenger " + p.toString() + " in cache.");
			    throw new IllegalStateException();
		    }
		  
		    Station s = TransitGlobalCache.passengerEndStationCache.get(p);
		    if (!scheduler.trips.containsKey(p.toString())) 
		    {
			    System.out.println("Missing a passenger in the scheudler.");
			    throw new IllegalStateException();
		    }
		  
		    // if were not starting here then were doing it wrong
		    int endIndex = scheduler.trips.get(p.toString()).size() - 1;
		    if (TransitGlobalCache.passengerEndStationCache.get(p).toString().compareTo(scheduler.trips.get(p.toString()).get(endIndex)) != 0) 
		    {
			    System.out.println("Passenger: " + p.toString() + " not ending at right station..");
			    System.out.println("Expected: " + TransitGlobalCache.passengerEndStationCache.get(p).toString());
			    System.out.println("Result: " + scheduler.trips.get(p.toString()).get(endIndex));
			    throw new IllegalStateException();
		    }
	    }
    }

    // reset to an empty simulation
    public void reset() 
    {
        scheduler.lines.clear();
        scheduler.trips.clear();
        stations.clear();
        passengers.clear();
        trains.clear();
        //TransitGlobalCache.clearAllCachedData();
    }

    // adds simulation configuration from a file
    public void loadConfig(String filename) 
    {
		reset(); //just in case
		Gson gson = new Gson();
		String trainData = readTrainData(filename);
		//load the scheduler with data
		try {
			scheduler = gson.fromJson(trainData, TransitScheduler.class);
			if (scheduler == null) 
			{
				System.out.println("Couldn't Initialize scheduler");
				throw new IllegalStateException();
			}
			for (Entry<String, List<String>> line : scheduler.lines.entrySet())
				addLine(line.getKey(), line.getValue());
			for (Entry<String, List<String>> trip : scheduler.trips.entrySet())
				addJourney(trip.getKey(), trip.getValue());
			
			saveSchedulerData();
		} catch (JsonSyntaxException e) {
			System.out.println("Gson couldn't properly parse the file: ");
			e.printStackTrace();
		}
    }
  
    // helper method for parsing the json data from the file.
    public String readTrainData(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) 
        {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) 
                contentBuilder.append(sCurrentLine).append("\n");
        } catch (IOException e) 
        {
    	    e.printStackTrace();
        }
        return contentBuilder.toString();
    }
  
    // helper method for storing the schedulers data into lists of Trains, Passengers and Stations
    public void saveSchedulerData() 
    {
		Set<String> trainNames = scheduler.lines.keySet();
		for (String train : trainNames) 
			trains.add(Train.make(train));
		
		Set<String> passengerNames = scheduler.trips.keySet();
		for (String passenger : passengerNames)
			passengers.add(Passenger.make(passenger));
		
		Collection<List<String>> stationNames = scheduler.lines.values();
		for ( List<String> linestops : stationNames)
			for (String station : linestops)
				stations.add(Station.make(station));
	}
}
