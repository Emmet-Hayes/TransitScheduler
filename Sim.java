import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.*;

public class Sim 
{	
    public static void run_sim(Transit mbta, Log log, int numIterations, int tickDuration) 
    {
	    TransitGlobalCache.startUpTransitCache(mbta, log);
	    TransitGlobalCache.updateTrainsAndPassengerJourneys(mbta, log, numIterations, tickDuration);
	    TransitGlobalCache.printFinalData(mbta);
    }
  
    public static void main(String[] args) 
    {
    	// ARG PARSING SECTION
        String configFile = "sampleBOS.json";
	    int numIterations = 200;
	    int tickDuration = 500;
  	    try 
  	    {
  	    	if (args.length > 3) 
  	    	{
  	    		System.out.println("usage: ./sim <config file> <tickDuration> <numIterations>");
	            System.exit(1);
  	    	}
	        if (args.length > 2) 
	        {
	    	    numIterations = Integer.parseInt(args[2]);
	    	    if (numIterations < 0 || numIterations > 1000) throw new NumberFormatException();
	        }
	        if (args.length > 1) 
	        {
	        	tickDuration = Integer.parseInt(args[1]);
	        	if (tickDuration < 0 || tickDuration > 10000) throw new NumberFormatException();
	        } 
	        if (args.length > 0) configFile = args[0];

	    	// TRANSIT STARTUP
	        Transit mbta = new Transit();
	        mbta.loadConfig(configFile);

	        // TRANSIT RUN AND LOG
	        Log log = new Log();
	        run_sim(mbta, log, numIterations, tickDuration);
	    
	        String s = new LogJson(log).toJson();
	        PrintWriter out = new PrintWriter("log.json");
	        out.print(s);
	        out.close();
	    	Verify.verify(mbta, log);
	    	mbta.reset();
		} catch (Exception e) {
			if (e instanceof NumberFormatException) {
				System.out.println("Number formatting incorrect in some argument..");
				System.out.println("Expected an integer between 0 and 1000 for number of iterations.");
				System.out.println("Received: " + numIterations);
				System.out.println("Expected an integer between 0 and 10000 for tick duration.");
				System.out.println("Received: " + tickDuration);
			}
			else if (e instanceof FileNotFoundException) {
				System.out.println(".json file could not be found. make sure your path is correct");
			}
			else if (e instanceof IllegalStateException) {
				System.out.println("Ended in some illegal state.");
				e.printStackTrace();
			}
			else {
				System.out.println("Unhandled exception thrown: ");
				e.printStackTrace();
			}
			System.out.println("usage: ./sim <config file> <tickDuration> <numIterations>");
		}
    }
}