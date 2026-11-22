package com.github.rayman717.entity;

import com.github.rayman717.enums.EventsType;

import java.time.LocalDateTime;

public class Events {
    public String deviceID;
    public EventsType type;
    public int Level;
    public String detail;
    public int handled;
    public LocalDateTime time;
}
