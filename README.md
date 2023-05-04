This Program tests the speed and accuracy of the use of Redis Streams in a Redis Enterprise Active Active (or Active Passive) scenario.  

Normally, there should be less than a 100 millisecond lag between one instance and its peers or replica.

It should be that the speed of the WAN between the regions where such instances are deployed will have the largest impact on the measured lag.


The user is expected to provide the following:

```
--host1 <an endpoint for one instance of the active active DB>
--host2 <an endpoint for a different instance of the active active DB>
--port <this will be the same for all instances>
--password <this will be the same for all instances>
``` 
The user can also provide:

```
--outerloopsize 
```
and 
```
--innerloopsize
```

Where innerloopsize determines the # entries written into each batch 
and the outerloopsize determines how many batches to write. 
(default is 50 batch size and 100 batches)

At the beginning of each run, the existing Stream is deleted at one of the peer instances of Redis Enterprise.
Data is then immediately written into the other peer instance in a newly created Stream.

Data is written in batches using Pipeline.

At the end of the execution of this program, the program reads from the instance that has not been written to directly 
to check the length of the Stream as it sees it. 
It repeats this test until the size of the stream matches the expected length.

There are a few additional arguments that can be provided such as the name of the stream...

Here is a sample run:

```
mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host1 redis-18031.mc290-0.us-central1-mz.gcp.cloud.rlrcp.com --host2 redis-18031.mc290-1.us-west1-mz.gcp.cloud.rlrcp.com --port 18031 --password C2iadH1haCAk1zrHKongj2nLwYrI0GvzdMn --streamname x:stream1 --sleeptimemillis 6 --outerloopsize 5 --innerloopsize 1000"
loading custom --streamname == x:stream1
loading custom --host1 == redis-18031.mc290-0.us-central1-mz.gcp.cloud.rlrcp.com
loading custom --host2 == redis-18031.mc290-1.us-west1-mz.gcp.cloud.rlrcp.com
loading custom --port == 18031
loading custom --outerloopsize == 5
loading custom --innerloopsize == 1000
loading custom --password == C2iadH1haCAk1zrHKongj2nLwYrI0GvzdMn
loading custom --sleeptimemillis == 6
connecting to redis-18031.mc290-0.us-central1-mz.gcp.cloud.rlrcp.com on port 18031
Connection Creation Debug --> 3


Using user: default / password @@@@@@@@@@C2iadH1haCAk1zrHKongj2nLwYrI0GvzdMn
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
Sleeping for 6 milliseconds
1
connecting to redis-18031.mc290-1.us-west1-mz.gcp.cloud.rlrcp.com on port 18031
Connection Creation Debug --> 3


Using user: default / password @@@@@@@@@@C2iadH1haCAk1zrHKongj2nLwYrI0GvzdMn
Sleeping for 6 milliseconds
pong
Deleting existing Stream on --host2
Writing 5000 entries to a new stream in batches of 1000
batch complete
Sleeping for 6 milliseconds
batch complete
Sleeping for 6 milliseconds
batch complete
Sleeping for 6 milliseconds
batch complete
Sleeping for 6 milliseconds
batch complete
Sleeping for 6 milliseconds

The time is now: 15:19:43  or as milliseconds --> 1683231583655
Expecting a stream length of 5000 the replicated stream length is now: 4377

The time is now: 15:19:43  or as milliseconds --> 1683231583715
Expecting a stream length of 5000 the replicated stream length is now: 5000
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  2.837 s
[INFO] Finished at: 2023-05-04T15:19:43-05:00
[INFO] ------------------------------------------------------------------------
```