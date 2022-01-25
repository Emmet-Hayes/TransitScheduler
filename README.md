# TransitScheduler
This project is a command line tool that can read .json data formulated for tracking transit patterns to a multithreaded concurrent simulation of passengers boarding and unboarding trains that constantly move to the next station on the line. The trick here, is that two trains cannot occupy the same station at any time.

You will need the java SDK to run this. Once that's done...

# How to Run
Run this project with:
    `./sim <json file> <tickDuration> <numMaxIterations>`


Recompile this project with:
  `javac -cp "gson-2.8.9.jar:junit-4.13.2.jar" BoardEvent.java DeboardEvent.java Entity.java Event.java Log.java Transit.java TransitGlobalCache.java TransitScheduler.java MoveEvent.java Passenger.java PassengerBuffer.java Sim.java Station.java Tests.java Train.java TrainBuffer.java Verify.java LogJson.java`
  
# How to Create Valid Json Data for this Project
This system is expecting a .json file with `lines` and `trips` provided. each line consists of one train moving to each stations, and each trip consists of one passenger boarding and deboarding trains that go to the next station on their trip.

The data formatting expected for the program aligns with typical json syntax.
Please format the data like so:

```
{
  "lines": {
    "red": [ "Davis", "Harvard", "Kendall", "Park", "Downtown Crossing",
      "South Station", "Broadway", "Andrew", "JFK" ],
    "orange": [ "Ruggles", "Back Bay", "Tufts Medical Center", "Chinatown",
      "Downtown Crossing", "State", "North Station",  "Sullivan" ],
    "green": [ "Tufts", "East Sommerville", "Lechmere", "North Station",
      "Government Center", "Park", "Boylston", "Arlington", "Copley" ],
    "blue": [ "Bowdoin", "Government Center", "State", "Aquarium",
      "Maverick", "Airport" ]
  },
  "trips": {
    "Bob": [ "Park", "Tufts" ],
    "Alice": [ "Davis", "Kendall" ],
    "Carol": [ "Maverick", "Government Center", "Tufts" ],
    "Emmet": [ "Copley", "Government Center", "Airport" ],
    "Sally": [ "Airport", "Government Center", "North Station", "Downtown Crossing", "Davis" ]
  }
}
```

Each line will spawn a train and each trip will spawn a passenger. Each train will move to the next station, unless the next station is already occupied. Each passenger will wait at their starting station for a train that travels to their next destination, and board that train until their stop.

# Important Notes

Any specific order of train movement is not guaranteed, and its important to remember that trains will always attempt to move as quickly as possible.

It is recommended that users do not supply data that has impossible deadlocking scenarios, such as two trains that share the same next station but could end up going in opposite directions. That can cause both trains to block each other, with no possibility of either one ever moving forward!
see this example:

```
{
  "lines": {
    "red": [ "west", "central", "east"],
    "green": [ "east", "central", "west"]
  },
  "trips": {
    "Adam": ["west", "northeast"]
  }
}
```

for the first iteration, we can imagine that either the `red` or `green` line will go first, occupying the `central` station. Then what?
If `red` goes first to `central`, 
then `green` will have to wait until `central` is no longer occupied.
BUT, since `red` can never move out of `central` and into `east` because `green` is still waiting at `east` for `central` to clear,
we have an impossible deadlock. 

Just to be thorough lets see what happens if `green` goes first...

If `green` goes first to `central`,
then `red` will have to wait until `central` is no longer occupied.
BUT, since `green` can never move out of `central` and into `west` because `red` is still waiting at `west` for `central` to clear,
we have an impossible deadlock yet again.
