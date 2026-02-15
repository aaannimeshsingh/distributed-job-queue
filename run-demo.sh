#!/bin/bash

# Run demos with proper Maven classpath
echo "Job Queue Demo Runner"
echo ""

cd "$(dirname "$0")"

if [ -z "$1" ]; then
    echo "Usage: ./run-demo.sh [demo-type]"
    echo ""
    echo "Available demos:"
    echo "  api         - Start API server"
    echo "  multi       - Multi-worker demo"
    echo "  load        - Load test (1000 jobs)"
    echo "  financial   - Financial services demo"
    echo "  hardware    - Hardware/EDA demo"
    echo "  security    - Security operations demo"
    echo "  all         - All domains demo"
    exit 1
fi

case "$1" in
    api)
        mvn exec:java -Dexec.mainClass="com.jobqueue.JobQueueAPI"
        ;;
    multi)
        mvn exec:java -Dexec.mainClass="com.jobqueue.MultiWorkerDemo"
        ;;
    load)
        mvn exec:java -Dexec.mainClass="com.jobqueue.LoadTestDemo"
        ;;
    financial)
        mvn exec:java -Dexec.mainClass="com.jobqueue.DomainDemos" -Dexec.args="financial"
        ;;
    hardware)
        mvn exec:java -Dexec.mainClass="com.jobqueue.DomainDemos" -Dexec.args="hardware"
        ;;
    security)
        mvn exec:java -Dexec.mainClass="com.jobqueue.DomainDemos" -Dexec.args="security"
        ;;
    all)
        mvn exec:java -Dexec.mainClass="com.jobqueue.DomainDemos" -Dexec.args="all"
        ;;
    *)
        echo "Unknown demo: $1"
        exit 1
        ;;
esac