package com.odyssey.environment;

public class WorldClock {

    public static final long TICKS_PER_DAY = 24000;
    private static final float SECONDS_PER_DAY = 1200; // 20 minutes
    private static final float TICK_RATE = TICKS_PER_DAY / SECONDS_PER_DAY;

    private long totalTicks = 0;
    private long timeOfDay = 6000; // Start at morning

    public void update(float deltaTime) {
        long ticksToAdd = (long) (deltaTime * TICK_RATE);
        totalTicks += ticksToAdd;
        timeOfDay = totalTicks % TICKS_PER_DAY;
    }
    
    public long getTimeOfDay() {
        return timeOfDay;
    }

    public int getCurrentDay() {
        return (int) (totalTicks / TICKS_PER_DAY);
    }
    
    public boolean isDay() {
        return timeOfDay >= 0 && timeOfDay < 13000;
    }

    public boolean isNight() {
        return !isDay();
    }
} 