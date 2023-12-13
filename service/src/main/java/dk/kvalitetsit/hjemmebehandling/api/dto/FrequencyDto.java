package dk.kvalitetsit.hjemmebehandling.api.dto;

import dk.kvalitetsit.hjemmebehandling.mapping.ToModel;
import dk.kvalitetsit.hjemmebehandling.model.FrequencyModel;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

public class FrequencyDto implements ToModel<FrequencyModel> {
    private List<Weekday> weekdays;
    private String timeOfDay;

    public List<Weekday> getWeekdays() {
        return weekdays;
    }

    public void setWeekdays(List<Weekday> weekdays) {
        this.weekdays = weekdays;
    }

    public String getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(String timeOfDay) {
        this.timeOfDay = timeOfDay;
    }


    @Override
    public FrequencyModel toModel() {
        FrequencyModel frequencyModel = new FrequencyModel();

        if(this.getWeekdays() == null) {
            throw new IllegalArgumentException("Weekdays must be non-null!");
        }
        frequencyModel.setWeekdays(this.getWeekdays());
        if(this.getTimeOfDay() == null || Objects.equals(this.getTimeOfDay(), "")) {
            throw new IllegalArgumentException("TimeOfDay must not be null or empty string!");
        }
        frequencyModel.setTimeOfDay(LocalTime.parse(this.getTimeOfDay()));

        return frequencyModel;
    }
}
