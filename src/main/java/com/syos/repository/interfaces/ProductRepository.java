package com.syos.repository.interfaces;

import com.syos.domain.models.Product;
import com.syos.domain.valueobjects.ProductCode;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends Repository<Product, ProductCode> {
    List<Product> searchByTerm(String searchTerm);
    List<Product> findActiveProducts();
    Optional<Product> findByCodeAndActive(ProductCode productCode);
}