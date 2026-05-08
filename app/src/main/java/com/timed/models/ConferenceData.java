package com.timed.models;

import java.util.List;

public class ConferenceData {
    public String conferenceId;
    public List<EntryPoint> entryPoints;

    public ConferenceData() {
        this.entryPoints = new java.util.ArrayList<>();
    }
}
