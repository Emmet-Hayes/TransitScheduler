public class Passenger extends Entity 
{
    private Passenger(String name) { super(name); }

    public static Passenger make(String name) 
    {
    	// If the passenger already exists in the cache, just return the existing passenger.
	    if (TransitGlobalCache.passengerCache.containsKey(name))
		    return TransitGlobalCache.passengerCache.get(name);

	    Passenger p = new Passenger(name);
	    TransitGlobalCache.passengerCache.put(name, p);
        return p;
    }
}
