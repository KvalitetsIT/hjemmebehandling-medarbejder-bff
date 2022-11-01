package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.types.Weekday;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

public class FrequencyModel {
    private List<Weekday> weekdays;
    private LocalTime timeOfDay;

    public List<Weekday> getWeekdays() {
        return weekdays;
    }

    public void setWeekdays(List<Weekday> weekdays) {
        this.weekdays = weekdays;
    }

    public LocalTime getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(LocalTime timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FrequencyModel that = (FrequencyModel) o;
        return weekdays.equals(that.weekdays) && timeOfDay.equals(that.timeOfDay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(weekdays, timeOfDay);
    }
}
