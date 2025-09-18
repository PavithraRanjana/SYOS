# SYOS Grocery Store - Point of Sale System

A comprehensive CLI-based Point of Sale system for SYOS Grocery Store implementing clean code practices, SOLID principles, and design patterns.

## Quick Start

```bash
# Compile the project
mvn compile

# Run tests
mvn test

# Run the application
mvn exec:java -Dexec.mainClass="com.syos.SyosApplication"
```

## Features

- Physical store POS with cash-only transactions
- Real-time inventory management with FIFO
- Batch tracking for complete product traceability
- Employee-friendly short product codes
- Comprehensive receipt generation
- Exception handling with user-friendly messages

## Architecture

- Clean Architecture with layered design
- Repository pattern for data access
- Strategy pattern for payment processing
- Value objects for domain modeling
- Dependency injection throughout
- Comprehensive unit testing with JUnit5 + Mockito

## Database Setup

1. Create MySQL database: `syos_grocery_store`
2. Run the provided database schema
3. Update connection settings in `application.properties`

For detailed setup and usage instructions, see the documentation in the artifacts.
