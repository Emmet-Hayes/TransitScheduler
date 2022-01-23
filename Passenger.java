public class Passenger extends Entity 
{
    private Passenger(String name) { super(name); }

    public static Passenger make(String name) 
    {
	    if (TransitGlobalCache.passengerCache.containsKey(name))
		    return TransitGlobalCache.passengerCache.get(name);

	    Passenger p = new Passenger(name);
	    TransitGlobalCache.passengerCache.put(name, p);
        return p;
    }
}
