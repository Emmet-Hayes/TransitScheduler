import java.io.*;
import java.util.*;

public class Verify 
{
    public static void verify(Transit mbta, Log log) 
    {
        mbta.checkStart();
        for (Event e : log.events()) {
            e.replayAndCheck(mbta);
        }
        mbta.checkEnd();
    }

    public static void main(String[] args) throws FileNotFoundException 
    {
        String configFile = "sampleBOS.json";
        String logFile = "log.json";
    
        if (args.length > 2) 
        {
            System.out.println("usage: ./verify <config file> <log file>");
            System.exit(1);
        }
        if (args.length > 1) 
            logFile = args[1];
        if (args.length > 0)
            logFile = args[0];

        Transit mbta = new Transit();
        mbta.loadConfig(configFile);
        
        Reader r = new BufferedReader(new FileReader(logFile));
        Log log = LogJson.fromJson(r).toLog();
        verify(mbta, log);
    }
}
