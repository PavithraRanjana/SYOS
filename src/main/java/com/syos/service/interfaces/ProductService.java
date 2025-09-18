package com.syos.service.interfaces;

import com.syos.domain.models.Product;
import com.syos.domain.valueobjects.ProductCode;
import java.util.List;

public interface ProductService {
    Product findProductByCode(ProductCode productCode);
    List<Product> searchProducts(String searchTerm);
    int getAvailableStock(ProductCode productCode);
    boolean isProductAvailable(ProductCode productCode, int quantity);
}
