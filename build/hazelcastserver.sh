#!/bin/sh

java -server -Xms1G -Xmx1G -Djava.net.preferIPv4Stack=true -cp TIPL.jar com.hazelcast.examples.StartServer


