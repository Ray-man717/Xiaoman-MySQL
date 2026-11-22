package com.github.rayman717.enums;

public enum DeviceStatus {
    ONLINE("online"),
    OFFLINE("offline");

    private final String dbValue;
    DeviceStatus(String dbValue) { this.dbValue = dbValue; }
    public String getDbValue() { return dbValue; }
}
