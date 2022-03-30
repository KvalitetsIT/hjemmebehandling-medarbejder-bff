package dk.kvalitetsit.hjemmebehandling.service.frequency;

import dk.kvalitetsit.hjemmebehandling.model.FrequencyModel;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;

import java.time.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FrequencyEnumerator {
    private Instant currentPointInTime;
    private List<DayOfWeek> weekDays;
    private LocalTime deadlineTime; //fx if you wanna say "Før kl 11", deadlineTime should be 11:00

    public FrequencyEnumerator(Instant seed, FrequencyModel frequency) {
        currentPointInTime = seed;
        this.deadlineTime = frequency.getTimeOfDay();

        initializeWeekdays(frequency.getWeekdays());
    }

    public Instant getPointInTime() {
        return currentPointInTime;
    }

    public FrequencyEnumerator next() {
        // Determine the current weekday
        var currentDayOfWeek = getCurrentDayOfWeek(currentPointInTime);

        // Determine number of days to add
        var currentTimeOfDay = getCurrentTimeOfDay(currentPointInTime);
        int daysToAdd = 0;
        var includeToday = false; //!currentTimeAfterOrOnTimeOfDay(currentTimeOfDay);

        // Get the successive weekday from the frequency model
        var successiveDayOfWeek = getSuccessiveDayOfWeek(currentDayOfWeek, includeToday);
        daysToAdd = getDaysToAdd(currentDayOfWeek, currentTimeOfDay, successiveDayOfWeek);

        // Advance currentPointInTime
        this.currentPointInTime = advanceCurrentPointInTime(currentPointInTime, daysToAdd);
        return this;
    }

    private DayOfWeek getCurrentDayOfWeek(Instant pointInTime) {
        return LocalDate.ofInstant(pointInTime, ZoneId.of("UTC")).getDayOfWeek();
    }

    private DayOfWeek getSuccessiveDayOfWeek(DayOfWeek currentDayOfWeek, boolean includeToday) {
        for(int index = 0; index < weekDays.size(); index++) {
            var day = weekDays.get(index);

            if(currentDayOfWeek.ordinal() == day.ordinal() && !includeToday) {
                continue;
            }
            if(currentDayOfWeek.ordinal() > day.ordinal()) {
                continue;
            }
            return weekDays.get(index);
        }

        return weekDays.get(0);
    }

    private LocalTime getCurrentTimeOfDay(Instant pointInTime) {
        return LocalTime.ofInstant(pointInTime, ZoneId.of("Europe/Copenhagen"));
    }

    private int getDaysToAdd(DayOfWeek currentDayOfWeek, LocalTime currentTimeOfDay, DayOfWeek successiveDayOfWeek) {

        var successiveDayIsSameDayAsCurrent =currentDayOfWeek == successiveDayOfWeek;

        // If the two weekdays are the same, and currentTime is after (or on) deadline, we fast forward to next week
        if(successiveDayIsSameDayAsCurrent && currentTimeAfterOrOnTimeOfDay(currentTimeOfDay))
            return 7; //If today is monday, and succesive day is monday, we should advance to next week

        // If the two weekdays are the same, and currentTime is before deadline, we should not add any days
        if(successiveDayIsSameDayAsCurrent)
           return 0;

        // If currentDay does not match with the successive day - We should calculate the number of days
        return getDaysBetween(currentDayOfWeek, successiveDayOfWeek);
    }

    private boolean currentTimeAfterOrOnTimeOfDay(LocalTime currentTimeOfDay) {
        return !currentTimeOfDay.isBefore(deadlineTime);
    }

    private int getDaysBetween(DayOfWeek firstDay, DayOfWeek secondDay) {
        
        if(firstDay.ordinal() <= secondDay.ordinal()) {
            return secondDay.ordinal() - firstDay.ordinal();
        }
        else {
            return 7 - (firstDay.ordinal() - secondDay.ordinal());
        }
    }

    private Instant advanceCurrentPointInTime(Instant pointInTime, int daysToAdd) {
        return LocalDate
                .ofInstant(pointInTime, ZoneId.of("UTC"))
                .atStartOfDay()
                .plusDays(daysToAdd)
                .plusHours(deadlineTime.getHour())
                .plusMinutes(deadlineTime.getMinute())
                .toInstant(ZoneId.of("Europe/Copenhagen").getRules().getOffset(pointInTime));
    }

    private void initializeWeekdays(List<Weekday> weekdays) {
        var days = weekdays;
        if(days.isEmpty()) {
            days = List.of(Weekday.MON, Weekday.TUE, Weekday.WED, Weekday.THU, Weekday.FRI, Weekday.SAT, Weekday.SUN);
        }
        this.weekDays = days.stream().map(d -> toDayOfWeek(d)).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    private DayOfWeek toDayOfWeek(Weekday weekday) {
        switch(weekday) {
            case MON:
                return DayOfWeek.MONDAY;
            case TUE:
                return DayOfWeek.TUESDAY;
            case WED:
                return DayOfWeek.WEDNESDAY;
            case THU:
                return DayOfWeek.THURSDAY;
            case FRI:
                return DayOfWeek.FRIDAY;
            case SAT:
                return DayOfWeek.SATURDAY;
            case SUN:
                return DayOfWeek.SUNDAY;
            default:
                throw new IllegalArgumentException(String.format("Can't map Weekday: %s", weekday));
        }
    }
}
