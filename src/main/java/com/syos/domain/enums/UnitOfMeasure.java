package com.syos.domain.enums;

public enum UnitOfMeasure {
    PCS("pcs"),
    KG("kg"),
    LITER("liter"),
    BOTTLE("bottle"),
    CAN("can"),
    BAG("bag"),
    BOX("box"),
    JAR("jar"),
    BAR("bar");

    private final String value;

    UnitOfMeasure(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}