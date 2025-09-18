package com.syos.service.impl;

import com.syos.domain.models.Product;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.exceptions.ProductNotFoundException;
import com.syos.repository.interfaces.InventoryRepository;
import com.syos.repository.interfaces.ProductRepository;
import com.syos.service.interfaces.ProductService;
import java.util.List;

public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public Product findProductByCode(ProductCode productCode) {
        return productRepository.findByCodeAndActive(productCode)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found or inactive: " + productCode));
    }

    @Override
    public List<Product> searchProducts(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return productRepository.findActiveProducts();
        }
        return productRepository.searchByTerm(searchTerm.trim());
    }

    @Override
    public int getAvailableStock(ProductCode productCode) {
        return inventoryRepository.getTotalPhysicalStock(productCode);
    }

    @Override
    public boolean isProductAvailable(ProductCode productCode, int quantity) {
        return getAvailableStock(productCode) >= quantity;
    }
}