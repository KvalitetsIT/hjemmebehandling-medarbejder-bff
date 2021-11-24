package dk.kvalitetsit.hjemmebehandling.service.frequency;

import dk.kvalitetsit.hjemmebehandling.model.FrequencyModel;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.time.Period;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FrequencyEnumeratorTest {
    private static final Instant FRIDAY_AFTERNOON = Instant.parse("2021-11-26T14:00:00.000Z");
    private static final Instant SATURDAY_AFTERNOON = Instant.parse("2021-11-27T14:00:00.000Z");
    private static final Instant TUESDAY_AFTERNOON = Instant.parse("2021-11-30T14:00:00.000Z");

    @Test
    public void getPointInTime_initializedWithSeed() {
        // Arrange
        Instant seed = Instant.parse("2021-11-24T10:11:12.124Z");
        FrequencyModel frequencyModel = buildWeeklyFrequency();

        FrequencyEnumerator subject = new FrequencyEnumerator(seed, frequencyModel);

        // Act
        Instant result = subject.getPointInTime();

        // Assert
        assertEquals(seed, result);
    }

    @Test
    public void next_weekly_advancesWedToFri() {
        // Arrange
        Instant wednesday = Instant.parse("2021-11-24T10:11:12.124Z");
        FrequencyModel frequencyModel = buildWeeklyFrequency();

        FrequencyEnumerator subject = new FrequencyEnumerator(wednesday, frequencyModel);

        // Act
        Instant result = subject.next().getPointInTime();

        // Assert
        assertEquals(FRIDAY_AFTERNOON, result);
    }

    @Test
    public void next_weekly_fridayMorning() {
        // Arrange
        Instant fridayMorning = Instant.parse("2021-11-26T10:11:12.124Z");
        FrequencyModel frequencyModel = buildWeeklyFrequency();

        FrequencyEnumerator subject = new FrequencyEnumerator(fridayMorning, frequencyModel);

        // Act
        Instant result = subject.next().getPointInTime();

        // Assert
        assertEquals(FRIDAY_AFTERNOON, result);
    }

    @Test
    public void next_weekly_fridayEvening() {
        // Arrange
        Instant fridayEvening = Instant.parse("2021-11-26T18:11:12.124Z");
        FrequencyModel frequencyModel = buildWeeklyFrequency();

        FrequencyEnumerator subject = new FrequencyEnumerator(fridayEvening, frequencyModel);

        // Act
        Instant result = subject.next().getPointInTime();

        // Assert
        assertEquals(FRIDAY_AFTERNOON.plus(Period.ofWeeks(1)), result);
    }

    @Test
    public void nextTwice_weekly_advancesWedToFriNextWeek() {
        // Arrange
        Instant wednesday = Instant.parse("2021-11-24T10:11:12.124Z");
        FrequencyModel frequencyModel = buildWeeklyFrequency();

        FrequencyEnumerator subject = new FrequencyEnumerator(wednesday, frequencyModel);

        // Act
        Instant result = subject.next().next().getPointInTime();

        // Assert
        assertEquals(FRIDAY_AFTERNOON.plus(Period.ofWeeks(1)), result);
    }

    @Test
    public void next_semiweekly_advancesWedToFri() {
        // Arrange
        Instant wednesday = Instant.parse("2021-11-24T10:11:12.124Z");
        FrequencyModel frequencyModel = buildSemiWeeklyFrequency();

        FrequencyEnumerator subject = new FrequencyEnumerator(wednesday, frequencyModel);

        // Act
        Instant result = subject.next().getPointInTime();

        // Assert
        assertEquals(FRIDAY_AFTERNOON, result);
    }

    @Test
    public void nextTwice_semiweekly_advancesWedToTue() {
        // Arrange
        Instant wednesday = Instant.parse("2021-11-24T10:11:12.124Z");
        FrequencyModel frequencyModel = buildSemiWeeklyFrequency();

        FrequencyEnumerator subject = new FrequencyEnumerator(wednesday, frequencyModel);

        // Act
        Instant result = subject.next().next().getPointInTime();

        // Assert
        assertEquals(TUESDAY_AFTERNOON, result);
    }

    @Test
    public void next_weekdaysOmitted_interpretedAsDaily() {
        // Arrange
        FrequencyModel frequencyModel = buildDailyFrequency();

        FrequencyEnumerator subject = new FrequencyEnumerator(FRIDAY_AFTERNOON, frequencyModel);

        // Act
        Instant result = subject.next().getPointInTime();

        // Assert
        assertEquals(SATURDAY_AFTERNOON, result);
    }

    private FrequencyModel buildDailyFrequency() {
        return buildFrequency(List.of(), LocalTime.parse("14:00"));
    }

    private FrequencyModel buildWeeklyFrequency() {
        return buildFrequency(List.of(Weekday.FRI), LocalTime.parse("14:00"));
    }

    private FrequencyModel buildSemiWeeklyFrequency() {
        return buildFrequency(List.of(Weekday.TUE, Weekday.FRI), LocalTime.parse("14:00"));
    }

    private FrequencyModel buildFrequency(List<Weekday> weekdays, LocalTime timeOfDay) {
        FrequencyModel frequencyModel = new FrequencyModel();

        frequencyModel.setWeekdays(weekdays);
        frequencyModel.setTimeOfDay(timeOfDay);

        return frequencyModel;
    }
}