package com.github.rayman717.enums;

public enum ActivityState {
    NORMAL("normal"),   // 常态
    ASKING("asking"),   // 疑常，语音询问中
    ALARM("alarm");     // 报警

    private final String dbValue;
    ActivityState(String dbValue) { this.dbValue = dbValue; }
    public String getDbValue() { return dbValue; }

    public static ActivityState fromString(String text) {
        if (text == null) return NORMAL; // 防崩溃兜底

        for (ActivityState state : ActivityState.values()) {
            if (state.dbValue.equalsIgnoreCase(text)) {
                return state;
            }
        }
        return NORMAL; // 找不到就默认正常
    }
}
