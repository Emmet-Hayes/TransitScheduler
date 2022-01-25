# TransitScheduler
This project is a command line tool that can read .json data formulated for tracking transit patterns to a multithreaded concurrent simulation of passengers boarding and unboarding trains that constantly move to the next station on the line. The trick here, is that two trains cannot occupy the same station at any time.

You will need the java SDK to run this. Once that's done...

# HOW TO RUN
Run this project with:
    `./sim <json file> <tickDuration> <numMaxIterations>`


Recompile this project with:
  `javac -cp "gson-2.8.9.jar:junit-4.13.2.jar" BoardEvent.java DeboardEvent.java Entity.java Event.java Log.java Transit.java TransitGlobalCache.java TransitScheduler.java MoveEvent.java Passenger.java PassengerBuffer.java Sim.java Station.java Tests.java Train.java TrainBuffer.java Verify.java LogJson.java`
  
