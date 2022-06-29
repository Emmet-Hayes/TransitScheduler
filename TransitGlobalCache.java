import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.*;

public class TransitGlobalCache
{	
	public static Map<Train, Thread> trainMapToThread = new HashMap<>(); //each train's thread
	public static Map<Train, List<String>> trainMapToStops = new HashMap<>(); //each train's stops
	public static Map<Train, Integer> trainMapToCounter = new HashMap<>(); //each train's current index in the list of stops
	public static Map<String, Train> trainCache = new HashMap<>(); // maps line name to thread
	public static Map<Train, List<Station>> leftoverTrainCache = new HashMap<>(); // tracks trains from interrupted threads
	public static Map<Train, Station> trainStartStationCache = new HashMap<>();
	public static Map<Train, Station> trainEndStationCache = new HashMap<>();
	public static Map<Passenger, Thread> passengerMapToThread = new HashMap<>(); // each passenger's thread
	public static Map<Passenger, List<String>> passengerMapToJourney = new HashMap<>(); //each passenger's trip
	public static Map<Passenger, Boolean> passengerMapToIsBoarded = new HashMap<>(); //if passenger is currently on a train
	public static Map<Passenger, Boolean> passengerMapToCompletedJourney = new HashMap<>(); //if passenger is completely done with trip
	public static Map<Passenger, Train> passengerOnWhatTrain = new HashMap<>();
	public static Map<String, Passenger> passengerCache = new HashMap<>();
	public static Map<Passenger, Integer> passengerFinalIterationCount = new HashMap<>();
	public static Map<Passenger, Station> passengerStartStationCache = new HashMap<>();
	public static Map<Passenger, Station> passengerIntermediateStartStationCache = new HashMap<>();
	public static Map<Passenger, Station> passengerIntermediateEndStationCache = new HashMap<>();
	public static Map<Passenger, Station> passengerEndStationCache = new HashMap<>();
	public static Map<String, Integer> currentDestinationMap = new HashMap<>();
	public static Map<Station, Boolean> stationToIsOccupied = new HashMap<>();
	public static Map<String, List<String>> stationHoldsTrains = new HashMap<>();
	public static Map<String, Station> stationCache = new HashMap<>();
	
	public static int maxWaits = 5;
	public static int tickDuration = 500;
	public static int numIterations = 200;

	public static void clearAllCachedData() 
	{
		trainMapToThread.clear();
		trainMapToStops.clear();
		trainMapToCounter.clear();
		passengerMapToThread.clear();
		passengerMapToJourney.clear();
		passengerMapToIsBoarded.clear();
		passengerMapToCompletedJourney.clear();
		passengerCache.clear();
		passengerStartStationCache.clear();
		passengerEndStationCache.clear();
		passengerIntermediateStartStationCache.clear();
		passengerIntermediateEndStationCache.clear();
		passengerOnWhatTrain.clear();
		passengerFinalIterationCount.clear();
		trainCache.clear();
		trainStartStationCache.clear();
		trainEndStationCache.clear();
		stationHoldsTrains.clear();
		stationCache.clear();
		stationToIsOccupied.clear();
	}

	public static void startUpTransitCache(Transit mbta, Log log) 
	{
		//set up counters that trigger when we need to reverse direction
		for (String line : mbta.scheduler.lines.keySet()) 
		{
			Train t = Train.make(line);
			if (!TransitGlobalCache.trainMapToCounter.containsKey(t))
				TransitGlobalCache.trainMapToCounter.put(t, 0);
		}

		//start the passenger threads waiting for trains.
		for (String passengerName : mbta.scheduler.trips.keySet()) 
		{
			Passenger passenger = Passenger.make(passengerName);
			Station startStation = Station.make(mbta.scheduler.trips.get(passengerName).get(0));
			Station finalStation = Station.make(mbta.scheduler.trips.get(passengerName).get(mbta.scheduler.trips.get(passengerName).size() - 1));
			Thread passengerThread = new Thread(new PassengerBuffer(passenger, startStation, finalStation, mbta, log));

			TransitGlobalCache.passengerMapToThread.put(passenger, passengerThread);
			TransitGlobalCache.passengerMapToJourney.put(passenger, mbta.scheduler.trips.get(passengerName));
			TransitGlobalCache.passengerMapToIsBoarded.put(passenger, false);
			TransitGlobalCache.passengerMapToCompletedJourney.put(passenger, false);
			TransitGlobalCache.passengerStartStationCache.put(passenger, startStation);
			TransitGlobalCache.passengerEndStationCache.put(passenger, finalStation);
			passengerThread.start();
		}
		
		// keep track of what stations can have what kind of train at the station
		for (String line : mbta.scheduler.lines.keySet()) 
		{
			for (String station : mbta.scheduler.lines.get(line)) 
			{
				List<String> listOfTrains = null;
				//if the inner list already exists lets add to id
				if (TransitGlobalCache.stationHoldsTrains.containsKey(station)) 
				{
					listOfTrains = TransitGlobalCache.stationHoldsTrains.get(station);
					listOfTrains.add(line);
					TransitGlobalCache.stationHoldsTrains.put(station, listOfTrains);
				} else 
				{
					listOfTrains = new LinkedList<>();
					listOfTrains.add(line);
					TransitGlobalCache.stationHoldsTrains.put(station, listOfTrains);
				}
			}
		}
		for (String str : TransitGlobalCache.stationCache.keySet())
			TransitGlobalCache.stationToIsOccupied.put(Station.make(str), false);
	}

	public static void updateTrainsAndPassengerJourneys(Transit mbta, Log log, int numIterations, int tickDuration) 
	{
		TransitGlobalCache.numIterations = numIterations;
		TransitGlobalCache.tickDuration = tickDuration;
		//running trains
		int j = 0;
		while (j < numIterations) 
		{
			// break if all journeys are complete!
			if (!TransitGlobalCache.passengerMapToCompletedJourney.values().contains(false)) 
			{
				System.out.println("All Journeys are marked complete!!!!!");
				break;
			}

			// for each line in the scheduler we just made,
			for (Entry<String, List<String>> line : mbta.scheduler.lines.entrySet()) 
			{
				Train train = Train.make(line.getKey());

				// add the starting station for train to cache for verifier. only happens if
				// cache doesnt have a value there yet
				if (!TransitGlobalCache.trainStartStationCache.containsKey(train)) 
					TransitGlobalCache.trainStartStationCache.put(train, Station.make(line.getValue().get(0)));
				
				// add the ending station for train to cache for verifier. only happens if
				// cache doesnt have a value there yet
				if (!TransitGlobalCache.trainEndStationCache.containsKey(train)) 
				{
					int endIndex = line.getValue().size() - 1;
					TransitGlobalCache.trainEndStationCache.put(train, Station.make(line.getValue().get(endIndex)));
				}
				
				// reverse the train direction if we hit the end
				List<String> stopsOnTrain = mbta.scheduler.lines.get(line.getKey());
				if (TransitGlobalCache.trainMapToCounter.get(train) + 1 ==  mbta.scheduler.lines.get(line.getKey()).size()) 
				{
					Collections.reverse(stopsOnTrain);
					TransitGlobalCache.trainMapToCounter.put(train, 0);
				}
				
				Station currentStation = Station.make(line.getValue().get(TransitGlobalCache.trainMapToCounter.get(train) % stopsOnTrain.size()));
				TransitGlobalCache.stationToIsOccupied.put(currentStation, true);
				TransitGlobalCache.stationCache.put(currentStation.toString(), currentStation);
				Station nextStation = Station.make(line.getValue().get((TransitGlobalCache.trainMapToCounter.get(train) + 1) % stopsOnTrain.size()));
				
				// check if any passengers want to board or deboard
				for (Passenger p : TransitGlobalCache.passengerMapToJourney.keySet()) 
				{
					String s = currentStation.toString();
					String startingStationName = TransitGlobalCache.passengerMapToJourney.get(p).get(0);
					String destinationName = TransitGlobalCache.passengerMapToJourney.get(p).get(1);

					//if we have different starts and ends in the cache, load those instead here
					if (TransitGlobalCache.passengerIntermediateStartStationCache.containsKey(p)) 
					{
						startingStationName = TransitGlobalCache.passengerIntermediateStartStationCache.get(p).toString();
						
						// if we also have multiple intermediate stops in the journey, we'll have to get intermediate destinations
						if (TransitGlobalCache.passengerMapToJourney.get(p).size() > 3) 
						{
							int size = TransitGlobalCache.passengerMapToJourney.get(p).size();
							int trueIndex = - 1;
							for (int m = 0; m < size; ++m)
								if (TransitGlobalCache.passengerMapToJourney.get(p).get(m).compareTo(startingStationName) == 0)
									if (m + 1 < size) trueIndex = m+1;
							destinationName = TransitGlobalCache.passengerMapToJourney.get(p).get(trueIndex);
						} else 
						{
							destinationName = TransitGlobalCache.passengerEndStationCache.get(p).toString();
						}
					}
						
					
					if (s.compareTo(startingStationName) == 0) 
					{

						if (!(TransitGlobalCache.passengerMapToIsBoarded.get(p) || TransitGlobalCache.passengerMapToCompletedJourney.get(p))) 
						{
							//does this train line go where we want our passengers trip to end?
							if (mbta.scheduler.lines.get(train.toString()).contains(destinationName)) 
							{
								if (!TransitGlobalCache.passengerMapToCompletedJourney.get(p)) 
								{
									log.passenger_boards(p, train, currentStation);
									TransitGlobalCache.passengerOnWhatTrain.put(p, train); //save this checking deboards later
									TransitGlobalCache.passengerMapToIsBoarded.put(p, true);
								} 
							} else 
							{
								// is there another train that shares a station that will connect us to the destination?
								for (Entry<String, List<String>> otherLine : mbta.scheduler.lines.entrySet()) 
								{
									if (otherLine.getKey().compareTo(train.toString()) == 0) 
										continue; //skip own line
									if (otherLine.getValue().contains(destinationName)) 
									{
										//we want to switch now to the other line, so lets search for a station that holds both the
										//current train and our desired train
										String desiredTrain = otherLine.getKey();
										for (Entry<String, List<String>> stationToTrainsEntry : TransitGlobalCache.stationHoldsTrains.entrySet()) 
										{
											// if this station can contain both our station and that train
												int journeySize = TransitGlobalCache.passengerMapToJourney.get(p).size();
												//if this train isnt the most direct way, lets wait for the right one
												if (mbta.scheduler.lines.get(train.toString()).contains(destinationName)) 
												{
													//if youve not already boarded, lets get on the train
													if (!TransitGlobalCache.passengerMapToIsBoarded.get(p)) 
													{
														log.passenger_boards(p, train, currentStation);
														TransitGlobalCache.passengerOnWhatTrain.put(p, train); //save this checking deboards later
														TransitGlobalCache.passengerMapToIsBoarded.put(p, true);
													}
												}
											}
										//}
									}
								}
							}
						}
					}
					if (s.compareTo(destinationName) == 0) 
					{
						if (TransitGlobalCache.passengerMapToIsBoarded.get(p)) 
						{
							// if this passenger is on the right train to get off here, then lets do it
							if (TransitGlobalCache.passengerOnWhatTrain.containsKey(p)) 
							{
								String cachedTrainName = TransitGlobalCache.passengerOnWhatTrain.get(p).toString();
								if (cachedTrainName.compareTo(train.toString()) == 0) 
								{
									log.passenger_deboards(p, train, currentStation);
									TransitGlobalCache.passengerMapToIsBoarded.put(p, false);
									if (TransitGlobalCache.passengerEndStationCache.get(p).toString().compareTo(currentStation.toString()) == 0)
									{
										//if journey is complete, we should not re-board
										TransitGlobalCache.passengerMapToCompletedJourney.put(p, true);
										System.out.println(p.toString() + "'s Journey is complete!!!!!");
										TransitGlobalCache.passengerFinalIterationCount.put(p, j);
									} else 
									{
										//start again, this time starting from this station
										TransitGlobalCache.passengerIntermediateStartStationCache.put(p, currentStation);
									}
								}
							}
						}
					}
				}
				
				Thread trainThread = new Thread(new TrainBuffer(train, currentStation, nextStation, log));
				TransitGlobalCache.trainMapToStops.put(train, stopsOnTrain);
				TransitGlobalCache.trainMapToThread.put(train, trainThread);
				TransitGlobalCache.trainMapToCounter.put(train, TransitGlobalCache.trainMapToCounter.get(train) + 1);
				trainThread.start();
			}
			
			// join train threads
			for (Train train : TransitGlobalCache.trainMapToThread.keySet()) 
			{
				try {
					TransitGlobalCache.trainMapToThread.get(train).join();
					//drawAllTrainsAndStations(mbta);
				} catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
			}
			// join passenger threads
			for (Passenger p : TransitGlobalCache.passengerMapToThread.keySet()) 
			{
				try {
					TransitGlobalCache.passengerMapToThread.get(p).join();
				} catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
			}
			++j;
		}

	}

	public static void printFinalData(Transit mbta) {
		System.out.println("Final iteration counts are: "); {
			for (Passenger p : TransitGlobalCache.passengerFinalIterationCount.keySet()) {
				if (TransitGlobalCache.passengerFinalIterationCount.get(p) < TransitGlobalCache.numIterations) 
					System.out.println(p.toString() + "'s journey consisted of " + TransitGlobalCache.passengerMapToJourney.get(p).size()
						 + " stops: " + TransitGlobalCache.passengerMapToJourney.get(p) + ".\nIt took them " + 
						TransitGlobalCache.passengerFinalIterationCount.get(p) + " iterations to complete.");
				else
					System.out.println(p.toString() + " did not complete their journey.");
			}
		}
		System.out.println("Shutting down simulation...");
	}

}


