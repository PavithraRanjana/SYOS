package com.syos.service.interfaces;

import com.syos.domain.models.Product;
import com.syos.domain.valueobjects.ProductCode;
import java.util.List;
import java.util.Map;

public interface OnlineStoreService {
    Map<String, List<Product>> getProductsByCategory();
    List<Product> getProductsInCategory(String categoryName);
    int getAvailableStock(ProductCode productCode);
    boolean isProductAvailableOnline(ProductCode productCode, int quantity);
}