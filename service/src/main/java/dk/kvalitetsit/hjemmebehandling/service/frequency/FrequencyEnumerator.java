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
    private LocalTime timeOfDay;

    public FrequencyEnumerator(Instant seed, FrequencyModel frequency) {
        currentPointInTime = seed;
        this.timeOfDay = frequency.getTimeOfDay();

        initializeWeekdays(frequency.getWeekdays());
    }

    public Instant getPointInTime() {
        return currentPointInTime;
    }

    public FrequencyEnumerator next() {
        // Determine the current weekday
        var currentDayOfWeek = getCurrentDayOfWeek(currentPointInTime);

        // Get the successive weekday from the frequency model
        var successiveDayOfWeek = getSuccessiveDayOfWeek(currentDayOfWeek);

        // Determine number of days to add
        var currentTimeOfDay = getCurrentTimeOfDay(currentPointInTime);
        int daysToAdd = getDaysToAdd(currentDayOfWeek, currentTimeOfDay, successiveDayOfWeek);

        // Advance currentPointInTime
        this.currentPointInTime = advanceCurrentPointInTime(currentPointInTime, daysToAdd);
        return this;
    }

    private DayOfWeek getCurrentDayOfWeek(Instant pointInTime) {
        return LocalDate.ofInstant(pointInTime, ZoneId.of("UTC")).getDayOfWeek();
    }

    private DayOfWeek getSuccessiveDayOfWeek(DayOfWeek currentDayOfWeek) {
        for(int index = 0; index < weekDays.size(); index++) {
            var day = weekDays.get(index);
            if(currentDayOfWeek.ordinal() >= day.ordinal()) {
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
        int daysToAdd = 0;
        // If the two weekdays are the same, compare time of day.
        if(currentDayOfWeek == successiveDayOfWeek) {
            if(currentTimeAfterOrOnTimeOfDay(currentTimeOfDay)) {
                // Advance until next week.
                daysToAdd = 7;
            }
            else {
                // Don't add any days - just advance until timeOfDay today.
            }
        }
        else {
            // Compute the number of days to add.
            daysToAdd = getDaysBetween(currentDayOfWeek, successiveDayOfWeek);
        }
        return daysToAdd;
    }

    private boolean currentTimeAfterOrOnTimeOfDay(LocalTime currentTimeOfDay) {
        return !currentTimeOfDay.isBefore(timeOfDay);
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
                .plusHours(timeOfDay.getHour())
                .plusMinutes(timeOfDay.getMinute())
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
