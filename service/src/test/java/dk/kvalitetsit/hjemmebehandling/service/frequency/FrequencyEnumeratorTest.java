package dk.kvalitetsit.hjemmebehandling.service.frequency;

import dk.kvalitetsit.hjemmebehandling.model.FrequencyModel;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class FrequencyEnumeratorTest {
    private static final Instant FRIDAY_AFTERNOON = Instant.parse("2021-11-26T13:00:00.000Z");
    private static final Instant SATURDAY_AFTERNOON = Instant.parse("2021-11-27T13:00:00.000Z");
    private static final Instant TUESDAY_AFTERNOON = Instant.parse("2021-11-30T13:00:00.000Z");


    @Test
    public void check_winter_and_daylight_saving_time_returns_same_next_hour() {
        ZoneId zoneId = ZoneId.of("Europe/Copenhagen");
        ZoneRules zoneRules = zoneId.getRules();
        ZoneOffsetTransition zoneOffsetTransition = zoneRules.nextTransition(Instant.now());

        Instant winterTime, daylightSavingTime;
        if (zoneRules.isDaylightSavings(Instant.now())) {
            daylightSavingTime = Instant.now();
            winterTime = zoneOffsetTransition.getInstant();
        }
        else {
            winterTime = Instant.now();
            daylightSavingTime = zoneOffsetTransition.getInstant();
        }

        FrequencyModel fm = buildFrequency(List.of(Weekday.FRI), LocalTime.parse("14:00"));
        FrequencyEnumerator winterTimeFrequencyEnumerator = new FrequencyEnumerator(winterTime, fm);
        FrequencyEnumerator daylightSavingTimeFrequencyEnumerator = new FrequencyEnumerator(daylightSavingTime, fm);

        // Act
        Instant winterTimePointInTime = winterTimeFrequencyEnumerator.getPointInTime();
        Instant winterTimeNext = winterTimeFrequencyEnumerator.next().getPointInTime();

        Instant daylightSavingTimePointInTime = daylightSavingTimeFrequencyEnumerator.getPointInTime();
        Instant daylightSavingTimeNext = daylightSavingTimeFrequencyEnumerator.next().getPointInTime();


        // Assert
        assertTrue(zoneRules.isDaylightSavings(daylightSavingTime));
        assertFalse(zoneRules.isDaylightSavings(winterTime));

        assertNotEquals(winterTimePointInTime.atZone(ZoneOffset.UTC).getHour(), daylightSavingTimePointInTime.atZone(ZoneOffset.UTC).getHour());
        assertNotEquals(winterTimeNext.atZone(ZoneOffset.UTC).getHour(), daylightSavingTimeNext.atZone(ZoneOffset.UTC).getHour());
        assertEquals(winterTimeNext.atZone(zoneId).getHour(), daylightSavingTimeNext.atZone(zoneId).getHour());
        assertEquals(fm.getTimeOfDay().getHour(), winterTimeNext.atZone(zoneId).getHour());

    }


    private static final LocalTime ellevenOClock = LocalTime.ofInstant(Instant.parse("2021-11-25T11:00:00.00Z"),ZoneId.of("Europe/Copenhagen"));
    private static final LocalTime fourteenOClock = LocalTime.ofInstant(Instant.parse("2021-11-25T14:00:00.00Z"),ZoneId.of("Europe/Copenhagen"));

    private static final FrequencyModel allWeekAt11 = buildFrequency(List.of(Weekday.MON,Weekday.TUE,Weekday.WED, Weekday.THU, Weekday.FRI,Weekday.SAT,Weekday.SUN),ellevenOClock);
    private static final FrequencyModel tuesdayAndFridayAt14 = buildFrequency(List.of(Weekday.TUE, Weekday.FRI),fourteenOClock);
    private static final FrequencyModel FridayAt14 = buildFrequency(List.of(Weekday.FRI),fourteenOClock);

    private static Stream<Arguments> getPointInTime_GivenFrequencyTimeToCalculateAndExpectedResult() {
        return Stream.of(
                //getPointInTime_initializedWithSeed
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-24T10:11:12.124Z"),Instant.parse("2021-11-24T10:11:12.124Z"))
        );
    }
    @ParameterizedTest
    @MethodSource // arguments comes from a method that is name the same as the test
    public void getPointInTime_GivenFrequencyTimeToCalculateAndExpectedResult(FrequencyModel frequencyModel, Instant timeOfRecalculate, Instant timeCalculatedResult) {
        // Arrange
        FrequencyEnumerator subject = new FrequencyEnumerator(timeOfRecalculate, frequencyModel);

        // Act
        Instant result = subject.getPointInTime();
        // Assert
        assertEquals(timeCalculatedResult, result);
    }


    private static Stream<Arguments> nextCalledTwice_GivenFrequencyTimeToCalculateAndExpectedResult() {
        return Stream.of(
                //nextTwice_weekly_advancesWedToFriNextWeek
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-24T10:11:12.124Z"),Instant.parse("2021-11-30T14:00:00.000Z"))
        );
    }
    @ParameterizedTest
    @MethodSource // arguments comes from a method that is name the same as the test
    public void nextCalledTwice_GivenFrequencyTimeToCalculateAndExpectedResult(FrequencyModel frequencyModel, Instant timeOfRecalculate, Instant timeCalculatedResult) {
        // Arrange
        FrequencyEnumerator subject = new FrequencyEnumerator(timeOfRecalculate, frequencyModel);

        // Act
        Instant result = subject.next().next().getPointInTime();
        // Assert
        assertEquals(timeCalculatedResult, result);
    }



    private static Stream<Arguments> givenFrequencyTimeToCalculateAndExpectedResult_NextShouldResultInExpectedTime() {
        return Stream.of(

                //next_semiweekly_advancesWedToFri
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-24T10:11:12.124Z"),Instant.parse("2021-11-26T14:00:00.000Z")),

                //next_weekly_fridayEvening
                Arguments.of(FridayAt14,Instant.parse("2021-11-26T18:11:12.124Z"),Instant.parse("2021-12-03T14:00:00.000Z")),

                //next_weekly_fridayMorning
                Arguments.of(FridayAt14,Instant.parse("2021-11-26T10:11:12.124Z"),Instant.parse("2021-11-26T14:00:00.000Z")),

                //next_weekly_advancesWedToFri
                Arguments.of(FridayAt14,Instant.parse("2021-11-24T10:11:12.124Z"),Instant.parse("2021-11-26T14:00:00.000Z")),




                //Recalculating before 11 should trigger blue alarm on the same day
                Arguments.of(allWeekAt11,Instant.parse("2021-11-23T10:11:12.124Z"),Instant.parse("2021-11-23T11:00:00.00Z")),
                Arguments.of(allWeekAt11,Instant.parse("2021-11-24T10:11:12.124Z"),Instant.parse("2021-11-24T11:00:00.00Z")),
                Arguments.of(allWeekAt11,Instant.parse("2021-11-25T10:11:12.124Z"),Instant.parse("2021-11-25T11:00:00.00Z")),

                //Recalculating at exactly 11 o'clock should trigger blue alarm the next day
                Arguments.of(allWeekAt11,Instant.parse("2021-11-24T11:00:00.00Z"),Instant.parse("2021-11-25T11:00:00.00Z")),

                //Recalculating after 11 should trigger blue alarm the next day
                Arguments.of(allWeekAt11,Instant.parse("2021-11-23T12:11:12.124Z"),Instant.parse("2021-11-24T11:00:00.00Z")),
                Arguments.of(allWeekAt11,Instant.parse("2021-11-24T12:11:12.124Z"),Instant.parse("2021-11-25T11:00:00.00Z")),
                Arguments.of(allWeekAt11,Instant.parse("2021-11-25T12:11:12.124Z"),Instant.parse("2021-11-26T11:00:00.00Z"))
        );
    }
    @ParameterizedTest
    @MethodSource // arguments comes from a method that is name the same as the test
    public void givenFrequencyTimeToCalculateAndExpectedResult_NextShouldResultInExpectedTime(FrequencyModel frequencyModel, Instant timeOfRecalculate, Instant timeCalculatedResult){
        // Arrange
        FrequencyEnumerator subject = new FrequencyEnumerator(timeOfRecalculate, frequencyModel);

        // Act
        Instant result = subject.next().getPointInTime();
        // Assert
        assertEquals(timeCalculatedResult, result);
    }

    @Test
    public void next_weekdaysOmitted_interpretedAsDaily() {
        // Arrange
        FrequencyModel frequencyModel = buildFrequency(List.of(), LocalTime.parse("14:00"));

        FrequencyEnumerator subject = new FrequencyEnumerator(FRIDAY_AFTERNOON, frequencyModel);

        // Act
        Instant result = subject.next().getPointInTime();

        // Assert
        assertEquals(SATURDAY_AFTERNOON, result);
    }


    private static FrequencyModel buildFrequency(List<Weekday> weekdays, LocalTime timeOfDay) {
        FrequencyModel frequencyModel = new FrequencyModel();

        frequencyModel.setWeekdays(weekdays);
        frequencyModel.setTimeOfDay(timeOfDay);

        return frequencyModel;
    }
}