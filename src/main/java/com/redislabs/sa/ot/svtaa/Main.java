package com.redislabs.sa.ot.svtaa;

import redis.clients.jedis.JedisPooled;
import com.redislabs.sa.ot.util.JedisConnectionHelper;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.StreamEntryID;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * The purpose of this class is to test the consistency and reliability of Redis stream information
 * written to an AA database using Redis Enterprise technology
 *
 * To test, this program will write a sequence of events to the provided stream
 * It will write 10,000 entries
 * with a Thread.sleep after every 100 are written
 * each event will contain an incremented counter and the default timestamp provided by the
 * built-in stream mechanism
 * mvn compile exec:java -Dexec.cleanupDaemonThreads=false -Dexec.args="--host1 192.168.1.21 --host2 192.168.10.06 --port 14787 --streamname x:stream1 --sleeptimemillis 5"
 */

public class Main {

    static int localCounter = 0;
    static JedisPooled jedisPooledHost1 = null;
    static JedisPooled jedisPooledHost2 = null;
    static long sleepTimeMillis = 10;
    static int outerLoopSize = 100;
    static int innerLoopSize = 50;
    static JedisConnectionHelper jedisConnectionHelper1 = null;
    static JedisConnectionHelper jedisConnectionHelper2 = null;

    public static void main(String args[]) {

        String streamName = "x:mystream";
        String username = "default";
        String password = "";
        String password2 = "";
        String host1 = "localhost";
        String host2 = "localhost"; //should be the address of the peer Active Active instance
        int port = 7001;
        ArrayList<String> argList = null;
        if (args.length > 0) {
            argList = new ArrayList<>(Arrays.asList(args));
            if (argList.contains("--streamname")) {
                int argIndex = argList.indexOf("--streamname");
                streamName = argList.get(argIndex + 1);
                System.out.println("loading custom --streamname == " + streamName);
            }
            if (argList.contains("--host1")) {
                int argIndex = argList.indexOf("--host1");
                host1 = argList.get(argIndex + 1);
                System.out.println("loading custom --host1 == " + host1);
            }
            if (argList.contains("--host2")) {
                int argIndex = argList.indexOf("--host2");
                host2 = argList.get(argIndex + 1);
                System.out.println("loading custom --host2 == " + host2);
            }
            if (argList.contains("--port")) {
                int argIndex = argList.indexOf("--port");
                port = Integer.parseInt(argList.get(argIndex + 1));
                System.out.println("loading custom --port == " + port);
            }
            if (argList.contains("--outerloopsize")) {
                int argIndex = argList.indexOf("--outerloopsize");
                outerLoopSize = Integer.parseInt(argList.get(argIndex + 1));
                System.out.println("loading custom --outerloopsize == " + outerLoopSize);
            }
            if (argList.contains("--innerloopsize")) {
                int argIndex = argList.indexOf("--innerloopsize");
                innerLoopSize = Integer.parseInt(argList.get(argIndex + 1));
                System.out.println("loading custom --innerloopsize == " + innerLoopSize);
            }
            if (argList.contains("--username")) {
                int argIndex = argList.indexOf("--username");
                username = argList.get(argIndex + 1);
                System.out.println("loading custom --username == " + username);
            }
            if (argList.contains("--password")) {
                int argIndex = argList.indexOf("--password");
                password = argList.get(argIndex + 1);
                System.out.println("loading custom --password == " + password);
            }
            if (argList.contains("--password2")) {
                int argIndex = argList.indexOf("--password2");
                password2 = argList.get(argIndex + 1);
                System.out.println("loading custom --password2 == " + password2);
            }else{
                password2=password;
            }
            if (argList.contains("--sleeptimemillis")) {
                int argIndex = argList.indexOf("--sleeptimemillis");
                sleepTimeMillis = Long.parseLong(argList.get(argIndex + 1));
                System.out.println("loading custom --sleeptimemillis == " + sleepTimeMillis);
            }
        }

        System.out.println("connecting to "+host1+" on port "+port);
        jedisConnectionHelper1 = new JedisConnectionHelper(host1,port,username,password,outerLoopSize);
        jedisPooledHost1 = jedisConnectionHelper1.getPooledJedis();
        sleep(sleepTimeMillis);
        System.out.println(jedisPooledHost1.lpush("l"+streamName,"pong"));

        System.out.println("connecting to "+host2+" on port "+port);
        jedisConnectionHelper2 = new JedisConnectionHelper(host2,port,username,password2,200);
        jedisPooledHost2 = jedisConnectionHelper2.getPooledJedis();
        sleep(sleepTimeMillis);
        System.out.println(jedisPooledHost1.lpop("l"+streamName));

        populateStream(streamName);
        do {
            long timestamp = System.currentTimeMillis();
            System.out.println("\nThe time is now: "+new Time(timestamp).toLocalTime()+"  or as milliseconds --> "+timestamp);
        }while(!checkStreamLength(streamName));
    }

    static void populateStream(String streamName){
        //cleanup old stream if exists:
        //note that we delete on one peer and immediately begin writing on the other
        System.out.println("Deleting existing Stream on --host2");
        jedisPooledHost2.del(streamName);
        System.out.println("Writing "+outerLoopSize*innerLoopSize+" entries to a new stream in batches of "+innerLoopSize);
        for(int x=0;x<outerLoopSize;x++){
            Pipeline pipeLine1 = jedisConnectionHelper1.getPipeline();
            for(int y=0;y<innerLoopSize;y++){
                localCounter++;
                HashMap entry = new HashMap();
                entry.put("counter",localCounter);
                pipeLine1.xadd(streamName,StreamEntryID.NEW_ENTRY,entry);
            }
            pipeLine1.syncAndReturnAll();
            pipeLine1.close();
            System.out.println("batch complete");
            sleep(sleepTimeMillis);
        }
    }

    static boolean checkStreamLength(String streamName){
        boolean lengthsMatch = false;
        long streamLength = jedisPooledHost2.xlen(streamName);
        System.out.println("Expecting a stream length of "+localCounter+" the replicated stream length is now: "+streamLength);
        if(localCounter==streamLength){
            lengthsMatch=true;
        }
        return lengthsMatch;
    }

    static void sleep(long milliseconds){
        System.out.println("Sleeping for "+milliseconds+" milliseconds");
        try{
            Thread.sleep(milliseconds);
        }catch(InterruptedException ie){
            System.out.println("Aw heck - my sleep was interrupted!");
        }
    }

}