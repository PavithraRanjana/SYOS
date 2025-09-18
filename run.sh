#!/bin/bash

echo "Starting SYOS Grocery Store Application..."
echo "========================================"

# Check if compiled
if [ ! -d "target/classes" ]; then
    echo "Project not compiled. Running build first..."
    ./build.sh
fi

# Run the application
mvn exec:java -Dexec.mainClass="com.syos.SyosApplication"
