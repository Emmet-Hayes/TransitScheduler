import static org.junit.Assert.*;

import java.util.*;
import java.io.*;
import org.junit.Test;

public class Tests 
{    
	public static void main(String[] args) 
	{
        List<String> allConfigFiles = new LinkedList<>();
        allConfigFiles.add("sampleBOS.json");
        allConfigFiles.add("sampleLA.json"); 
        allConfigFiles.add("sampleNYC.json");
	    int numIterations = 200;
	    int tickDuration = 500;
	    for (String configFile : allConfigFiles) 
	    {
	  	    try 
	  	    {
		        if (args.length > 2) 
		        {
		            System.out.println("usage: ./sim <config file> <max_num_iterations> or ./sim <config file> will run up to 200 iterations");
		            System.exit(1);
		        }
		        if (args.length > 1) 
		        {
		    	    numIterations = Integer.parseInt(args[1]);
		    	    if (numIterations < 0 || numIterations > 1000) throw new NumberFormatException();
		        }
		        if (args.length > 0) configFile = args[0];
		    
		        Transit mbta = new Transit();
		        Log log = new Log();

		        mbta.loadConfig(configFile);
		        Sim.run_sim(mbta, log, numIterations, tickDuration);
		    
		        String s = new LogJson(log).toJson();
		        PrintWriter out = new PrintWriter("log.json");
		        out.print(s);
		        out.close();
		    	
		    	Verify.verify(mbta, log);
		    	mbta.reset();
			} catch (Exception e) 
			{
				if (e instanceof NumberFormatException) 
				{
					System.out.println("Number formatting incorrect in arguments..");
					System.out.println("Received: " + numIterations);
					System.out.println("Expected an integer between 0 and 1000.");
				}
				else if (e instanceof FileNotFoundException) 
				{
					System.out.println(".json file could not be found. make sure your path is correct");
				}
				else if (e instanceof IllegalStateException) 
				{
					System.out.println("Ended in some illegal state.");
				}
				else 
				{
					System.out.println("Unhandled exception thrown: ");
					e.printStackTrace();
				}
			}
		}
	}
}
