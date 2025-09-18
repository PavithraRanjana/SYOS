#!/bin/bash

echo "Building SYOS Grocery Store Project..."

# Clean and compile
mvn clean compile

if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo ""
    echo "Available commands:"
    echo "  ./run.sh     - Run the application"
    echo "  ./test.sh    - Run all tests"
    echo "  mvn exec:java - Run via Maven"
else
    echo "Build failed!"
    exit 1
fi
