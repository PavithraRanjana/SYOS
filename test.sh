#!/bin/bash

echo "Running SYOS Tests..."
echo "==================="

# Run tests with coverage
mvn test jacoco:report

if [ $? -eq 0 ]; then
    echo ""
    echo "Tests completed successfully!"
    echo "Coverage report available at: target/site/jacoco/index.html"
else
    echo "Some tests failed!"
    exit 1
fi
