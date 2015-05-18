
# Running with Live Data

1. Add a file to your visallo configuration directory. (eg ```/opt/visallo/config/flight-aware.properties```)

```
flightaware.username=<your flightaware username>
flightaware.apikey=<your flightaware apikey>
```

2. Run maven to create a FlightAware jar with dependencies

```
mvn -am -pl datasets/flight-aware/ -DskipTests package
```

3. Run

```
java -jar datasets/flight-aware/target/visallo-flight-aware-*-with-dependencies.jar \
   FlightAware
   --query="-idents VRD*"
   --out=/tmp/flightaware
```

# Running Replay

1. Run maven to create a FlightAware jar with dependencies

```
mvn -am -pl datasets/flight-aware/ -DskipTests package
```

2. Run

```
java -cp datasets/flight-aware/target/visallo-flight-aware-*-with-dependencies.jar \
  org.visallo.flightTrack.Replay
  --in=datasets/flight-aware/sample-data
```
