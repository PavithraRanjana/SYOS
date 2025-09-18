package com.syos.domain.valueobjects;

import java.util.Objects;

public final class ProductCode {
    private final String code;

    public ProductCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Product code cannot be null or empty");
        }
        if (code.length() > 15) {
            throw new IllegalArgumentException("Product code cannot exceed 15 characters");
        }
        this.code = code.trim().toUpperCase();
    }

    public String getCode() {
        return code;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ProductCode that = (ProductCode) obj;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return code;
    }
}