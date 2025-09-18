package com.syos.domain.valueobjects;

import java.util.Objects;

public final class BillSerialNumber {
    private final String serialNumber;

    public BillSerialNumber(String serialNumber) {
        if (serialNumber == null || serialNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Bill serial number cannot be null or empty");
        }
        this.serialNumber = serialNumber.trim();
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BillSerialNumber that = (BillSerialNumber) obj;
        return Objects.equals(serialNumber, that.serialNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serialNumber);
    }

    @Override
    public String toString() {
        return serialNumber;
    }
}