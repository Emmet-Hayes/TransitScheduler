public class Station extends Entity 
{    
    private Station(String name) { super(name); }

    public static Station make(String name) 
    {
	    if (TransitGlobalCache.stationCache.containsKey(name))
		    return TransitGlobalCache.stationCache.get(name);

	    Station s = new Station(name);
	    TransitGlobalCache.stationCache.put(name, s);
        return s;
    }
}
