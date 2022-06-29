import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class TrainBuffer implements Runnable 
{
	private Train t; // null if empty
	private Station s1; //might as well get the targeted next station
	private Station s2; 
	private Log log; //we'll need the log too
	private final Lock l = new ReentrantLock(); //trainBuffer lock and condition
	private Condition c = l.newCondition();

	TrainBuffer(Train t, Station s1, Station s2, Log log) 
	{ 
		this.t = t; this.s1 = s1; this.s2 = s2; this.log = log; 
	}

	boolean verboseDebugNextStationOccupied = false;
	boolean verboseDebugInterrupt = false;
	boolean verboseDebugTrainCache = false;

	@Override 
	public void run() 
	{
		Boolean hasLock = false;
		try 
		{
			hasLock = l.tryLock();
			TransitGlobalCache.stationToIsOccupied.put(s1, true);

			// throw exception if another train already posted the current destination for this train
			if (TransitGlobalCache.currentDestinationMap.get(s2.toString()) != null) 
				throw new InterruptedException();
			
			TransitGlobalCache.currentDestinationMap.put(s2.toString(), 1);

			// start up trains from the station if they were left over
			if (TransitGlobalCache.leftoverTrainCache.get(t) != null) 
			{
				if (verboseDebugTrainCache) 
				{
					System.out.println("################################################################");
					System.out.println("Multiple trains wanted to hit the same destination last round...");
					System.out.println("Waiting in cache: " + TransitGlobalCache.leftoverTrainCache);
					System.out.println("Run after:        {" + t.toString() + "=[" + s1.toString() + ", " + s2.toString() + "]}");
					System.out.println("################################################################");
				}
				Station cachedStart = TransitGlobalCache.leftoverTrainCache.get(t).get(0);
				Station cachedNext = TransitGlobalCache.leftoverTrainCache.get(t).get(1);
				while (TransitGlobalCache.stationToIsOccupied.get(cachedNext)) 
				{   //wait only once here if the first station is occupied, give threads time to let them notify us
					c.await((int)(TransitGlobalCache.tickDuration * 1.5) , TimeUnit.MILLISECONDS);
					boolean breakGridLock = false;
					try 
					{
						for (Train ot : TransitGlobalCache.leftoverTrainCache.keySet()) 
						{
							Station start = TransitGlobalCache.leftoverTrainCache.get(ot).get(0);
							Station next = TransitGlobalCache.leftoverTrainCache.get(ot).get(1);
							
							//lets stop this from potentially gridlocking itself
							if (start.toString().compareTo(cachedNext.toString()) == 0)
								if (TransitGlobalCache.stationToIsOccupied.get(next))
									breakGridLock = true;

							if (next.toString().compareTo(cachedStart.toString()) == 0)
								if (TransitGlobalCache.stationToIsOccupied.get(start))
									breakGridLock = true;
						}
					} catch (Exception e) {} // if we throw any exception trying to check the rest of the cache
					if (TransitGlobalCache.stationToIsOccupied.get(cachedNext)) //if we still need to wait we'll throw this again
						throw new InterruptedException();
					if (breakGridLock) break; //break out, and the next thread will deal with extra trains
				}
				c.signalAll();
				TransitGlobalCache.stationToIsOccupied.put(cachedStart, false);
				TransitGlobalCache.stationToIsOccupied.put(cachedNext, true);
				TransitGlobalCache.leftoverTrainCache.remove(t);
				log.train_moves(t, cachedStart, cachedNext);
			}
			// wait for train to move if next station is already known to be occupied
			if (TransitGlobalCache.stationToIsOccupied.get(s2)) 
			{
				if (verboseDebugNextStationOccupied) 
				{
					System.out.println("################################################################");
					System.out.println("Occupied...");
					System.out.println("T: " + t.toString() + "\nS: " + s1.toString() + "\nS2: " + s2.toString());
					System.out.println("################################################################");
				}
				for (int i = 0; TransitGlobalCache.stationToIsOccupied.get(s2); ++i) 
				{   
					c.await((int)(TransitGlobalCache.tickDuration * 1.5) , TimeUnit.MILLISECONDS);
					if (i > TransitGlobalCache.maxWaits) throw new InterruptedException();
				}
				c.signalAll();
				TransitGlobalCache.stationToIsOccupied.put(s1, false); //moves from station s1
				TransitGlobalCache.stationToIsOccupied.put(s2, true); // to s2
				log.train_moves(t, s1, s2);
			}
			else 
			{
				c.signalAll();
				c.await(TransitGlobalCache.tickDuration, TimeUnit.MILLISECONDS);	
				TransitGlobalCache.stationToIsOccupied.put(s1, false); //moves from station s1
				TransitGlobalCache.stationToIsOccupied.put(s2, true);
				log.train_moves(t, s1, s2);
			}
		}
		catch (InterruptedException e) 
		{
			if (verboseDebugInterrupt)
			{
				System.out.println("################################################################");
				System.out.println("Another train already posted the desired destination...train interrupted..");
				System.out.println("T: " + t.toString() + "\nS: " + s1.toString() + "\nS2: " + s2.toString());
				try 
				{
					System.out.println("DestinationMap: " + TransitGlobalCache.currentDestinationMap);
				} 
				catch (ConcurrentModificationException cme) 
				{
					System.out.println("Couldnt get the destinationMap this round");
				}
				System.out.println("################################################################");
			}
			List<Station> stations = new LinkedList<>();
			stations.add(s1);
			stations.add(s2);
			TransitGlobalCache.leftoverTrainCache.put(t, stations);
			TransitGlobalCache.stationToIsOccupied.put(s1, false); // this train must go to the cache
		} 
		finally 
		{
			if (hasLock) //only unlocks if the lock was actually acquired.
			{ 
				l.unlock();
			}

			// this map has to be cleared every time a series of threads are released
			TransitGlobalCache.currentDestinationMap.clear(); 
		}
	}
}
