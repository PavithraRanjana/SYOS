/**
 * Repository layer providing data access abstraction.
 * 
 * Implements the Repository pattern to abstract database operations
 * from business logic. Each repository interface defines data access
 * operations for a specific aggregate root.
 * 
 * - interfaces: Repository contracts (ProductRepository, BillRepository, etc.)
 * - impl: Concrete implementations using JDBC/MySQL
 * 
 * This layer isolates domain logic from persistence concerns.
 */
package com.syos.repository;
