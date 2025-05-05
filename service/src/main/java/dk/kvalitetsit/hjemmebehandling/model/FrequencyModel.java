package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.types.Weekday;

import java.time.LocalTime;
import java.util.List;


public record FrequencyModel(
        List<Weekday> weekdays,
        LocalTime timeOfDay
) {
    public FrequencyModel {
        // Ensure weekdays is never null
        weekdays = (weekdays != null) ? List.copyOf(weekdays) : List.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<Weekday> weekdays;
        private LocalTime timeOfDay;

        public Builder weekdays(List<Weekday> weekdays) {
            this.weekdays = (weekdays != null) ? List.copyOf(weekdays) : List.of();
            return this;
        }

        public Builder timeOfDay(LocalTime timeOfDay) {
            this.timeOfDay = timeOfDay;
            return this;
        }

        public FrequencyModel build() {
            return new FrequencyModel(weekdays, timeOfDay);
        }
    }


}
