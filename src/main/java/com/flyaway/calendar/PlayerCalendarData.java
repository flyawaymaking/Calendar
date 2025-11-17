package com.flyaway.calendar;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class PlayerCalendarData {
    private int lastClaimYear = 0;
    private int lastClaimMonth = 0;
    private String lastClaimDate = "";
    private final Set<Integer> claimedDays = new HashSet<>();

    public PlayerCalendarData(FileConfiguration config) {
        if (config != null) {
            this.lastClaimYear = config.getInt("lastClaimYear", 0);
            this.lastClaimMonth = config.getInt("lastClaimMonth", 0);
            this.lastClaimDate = config.getString("lastClaimDate", "");
            this.claimedDays.addAll(config.getIntegerList("claimedDays"));
        }
    }

    public PlayerCalendarData() {
    }

    public void saveToConfig(FileConfiguration config) {
        config.set("lastClaimYear", this.lastClaimYear);
        config.set("lastClaimMonth", this.lastClaimMonth);
        config.set("lastClaimDate", this.lastClaimDate);
        config.set("claimedDays", new ArrayList<>(this.claimedDays));
    }

    public void claimDay(int sequentialDay, String day) {
        this.claimedDays.add(sequentialDay);
        this.lastClaimDate = day;
    }

    public boolean hasClaimedDay(int sequentialDay) {
        return this.claimedDays.contains(sequentialDay);
    }

    public int getLastClaimedDay() {
        return this.claimedDays.stream().max(Integer::compareTo).orElse(0);
    }

    public int getNextClaimDay() {
        return getLastClaimedDay() + 1;
    }

    public int getLastClaimYear() {
        return lastClaimYear;
    }

    public int getLastClaimMonth() {
        return lastClaimMonth;
    }

    public String getLastClaimDate() {
        return lastClaimDate;
    }

    public Set<Integer> getClaimedDays() {
        return new HashSet<>(claimedDays);
    }

    public void setLastClaimYear(int lastClaimYear) {
        this.lastClaimYear = lastClaimYear;
    }

    public void setLastClaimMonth(int lastClaimMonth) {
        this.lastClaimMonth = lastClaimMonth;
    }

    public void setLastClaimDate(String lastClaimDate) {
        this.lastClaimDate = lastClaimDate;
    }
}
