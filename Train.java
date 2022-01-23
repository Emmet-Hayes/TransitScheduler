public class Train extends Entity 
{
    private Train(String name) { super(name); };
  
    public static Train make(String name) 
    {
	    if (TransitGlobalCache.trainCache.containsKey(name))
		    return TransitGlobalCache.trainCache.get(name);
	
	    Train t = new Train(name);
	    TransitGlobalCache.trainCache.put(name, t);
        return t;
    }
}
