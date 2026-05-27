package com.oncologia.agenda.model;

import java.time.LocalTime;

public class ScheduleConfig {
    private LocalTime workStart = LocalTime.of(8, 0);
    private LocalTime workEnd = LocalTime.of(18, 0);
    private int slotMinutes = 30;
    private int bedCount = 4;

    public LocalTime getWorkStart() {
        return workStart;
    }

    public void setWorkStart(LocalTime workStart) {
        this.workStart = workStart;
    }

    public LocalTime getWorkEnd() {
        return workEnd;
    }

    public void setWorkEnd(LocalTime workEnd) {
        this.workEnd = workEnd;
    }

    public int getSlotMinutes() {
        return slotMinutes;
    }

    public void setSlotMinutes(int slotMinutes) {
        this.slotMinutes = slotMinutes;
    }

    public int getBedCount() {
        return bedCount;
    }

    public void setBedCount(int bedCount) {
        this.bedCount = bedCount;
    }
}
