package com.syos.service.impl;

import com.syos.domain.models.Product;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.repository.interfaces.ProductRepository;
import com.syos.repository.interfaces.InventoryRepository;
import com.syos.service.interfaces.OnlineStoreService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OnlineStoreServiceImpl implements OnlineStoreService {
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public OnlineStoreServiceImpl(ProductRepository productRepository, 
                                 InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public Map<String, List<Product>> getProductsByCategory() {
        List<Product> allProducts = productRepository.findActiveProducts();
        
        // Group products by category - you'll need to add category lookup
        // For now, using a simple approach
        return allProducts.stream()
            .collect(Collectors.groupingBy(product -> getCategoryName(product.getCategoryId())));
    }

    @Override
    public List<Product> getProductsInCategory(String categoryName) {
        // This needs category lookup implementation
        return productRepository.findActiveProducts().stream()
            .filter(product -> getCategoryName(product.getCategoryId()).equals(categoryName))
            .collect(Collectors.toList());
    }

    @Override
    public int getAvailableStock(ProductCode productCode) {
        return inventoryRepository.getTotalOnlineStock(productCode);
    }

    @Override
    public boolean isProductAvailableOnline(ProductCode productCode, int quantity) {
        return getAvailableStock(productCode) >= quantity;
    }

    // Temporary method - should be replaced with proper category service
    private String getCategoryName(int categoryId) {
        switch (categoryId) {
            case 1: return "Beverages";
            case 2: return "Chocolate";
            case 3: return "Snacks";
            case 4: return "Spreads";
            default: return "Other";
        }
    }
}