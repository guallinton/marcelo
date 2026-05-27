package com.oncologia.agenda.model;

import java.time.LocalDateTime;

public class Appointment {
    private long id;
    private Patient patient;
    private User doctor;
    private User createdBy;
    private LocalDateTime start;
    private int durationMinutes;
    private int bedChair;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public User getDoctor() {
        return doctor;
    }

    public void setDoctor(User doctor) {
        this.doctor = doctor;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getBedChair() {
        return bedChair;
    }

    public void setBedChair(int bedChair) {
        this.bedChair = bedChair;
    }

    public LocalDateTime getEndWithCleaning() {
        return start.plusMinutes(durationMinutes + 15L);
    }

    public LocalDateTime getEnd() {
        return start.plusMinutes(durationMinutes);
    }
}
