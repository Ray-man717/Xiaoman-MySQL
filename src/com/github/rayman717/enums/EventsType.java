package com.github.rayman717.enums;

public enum EventsType {
    ALARM_NO_ACTIVITY("alarm_no_activity", 3),
    ALARM_BUTTON("alarm_button", 3),
    ALARM_STRANGER("alarm_stranger", 3),
    ASK("ask", 1),
    MEDICATION("medication", 2),
    REMOTE_OP("remote_op", 1);

    private final String dbValue;
    private final int level; // 1提示 2重要 3紧急

    EventsType(String dbValue, int level) {
        this.dbValue = dbValue;
        this.level = level;
    }
    public String getDbValue() { return dbValue; }
    public int getLevel() { return level; }
}
