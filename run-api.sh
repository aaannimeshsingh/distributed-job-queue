#!/bin/bash

# Run API Server with proper Maven classpath
echo "Starting Job Queue API Server..."
echo ""

cd "$(dirname "$0")"

mvn exec:java -Dexec.mainClass="com.jobqueue.JobQueueAPI"