import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PassengerBuffer implements Runnable 
{
	Passenger p; // null if empty
	Station s1; //starting station
	Station s2; //ending station
	Transit mbta; // copy over the whole damn mbta
	Log log;
	private final Lock l = new ReentrantLock(); //trainBuffer lock and condition
	private Condition c = l.newCondition();

	PassengerBuffer(Passenger p, Station currentStation, Station finalStation, Transit mbta, Log log) 
	{ 
		this.p = p;  this.s1 = currentStation; this.s2 = finalStation;
		this.mbta = mbta; this.log = log;
	}
	
	@Override
	public void run() {
		Boolean hasLock = false;
		/*try
		{
			hasLock = l.tryLock();
			//Station next = passengerIntermediateEndStationCache.get(p);
			//Station end = passengerEndStationCache.get(p);
		} catch (InterruptedException e) {
		} finally {
			if (hasLock) l.unlock();
		} */
	}
}
