package dk.kvalitetsit.hjemmebehandling.service.frequency;

import dk.kvalitetsit.hjemmebehandling.model.FrequencyModel;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.List;
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
        FrequencyEnumerator frequencyEnumerator = new FrequencyEnumerator(fm);
//        FrequencyEnumerator winterTimeFrequencyEnumerator = new FrequencyEnumerator(winterTime, fm);
//        FrequencyEnumerator daylightSavingTimeFrequencyEnumerator = new FrequencyEnumerator(daylightSavingTime, fm);

        // Act
        //Instant winterTimePointInTime = frequencyEnumerator.getDeadline(winterTime) winterTimeFrequencyEnumerator.getPointInTime();
        Instant winterTimeNext = frequencyEnumerator.getSatisfiedUntilForInitialization(winterTime);

        //Instant daylightSavingTimePointInTime = daylightSavingTimeFrequencyEnumerator.getPointInTime();
        Instant daylightSavingTimeNext = frequencyEnumerator.getSatisfiedUntilForInitialization(daylightSavingTime);


        // Assert
        assertTrue(zoneRules.isDaylightSavings(daylightSavingTime));
        assertFalse(zoneRules.isDaylightSavings(winterTime));

        assertNotEquals(winterTime.atZone(ZoneOffset.UTC).getHour(), daylightSavingTime.atZone(ZoneOffset.UTC).getHour());
        assertNotEquals(winterTimeNext.atZone(ZoneOffset.UTC).getHour(), daylightSavingTimeNext.atZone(ZoneOffset.UTC).getHour());
        assertEquals(winterTimeNext.atZone(zoneId).getHour(), daylightSavingTimeNext.atZone(zoneId).getHour());
        assertEquals(fm.getTimeOfDay().getHour(), winterTimeNext.atZone(zoneId).getHour());

    }


    private static final LocalTime ellevenOClock = LocalTime.of(11, 0);
    private static final LocalTime fourteenOClock = LocalTime.of(14, 0);

    private static final FrequencyModel allWeekAt11 = buildFrequency(List.of(Weekday.MON,Weekday.TUE,Weekday.WED, Weekday.THU, Weekday.FRI,Weekday.SAT,Weekday.SUN),ellevenOClock);
    private static final FrequencyModel tuesdayAndFridayAt14 = buildFrequency(List.of(Weekday.TUE, Weekday.FRI),fourteenOClock);
    private static final FrequencyModel FridayAt14 = buildFrequency(List.of(Weekday.FRI),fourteenOClock);

    private static Stream<Arguments> givenFrequencyTimeToCalculateAndExpectedResult_Initialization_ShouldResultInExpectedTime() {
        return Stream.of(
                // Instant is in UTC
                // Recalculating as clinician always advances to the following scheduled weekday, time of day is irrelevant
                Arguments.of(FridayAt14,Instant.parse("2021-11-22T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //monday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-22T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //monday,  after deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-23T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //tuesday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-23T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //tuesday, after deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-24T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //wednesday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-24T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //wednesday, after deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-25T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //thursday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-25T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //thursday, after deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-26T10:11:12.124Z"),Instant.parse("2021-12-03T13:00:00.00Z")), // friday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-26T15:11:12.124Z"),Instant.parse("2021-12-03T13:00:00.00Z")), // friday, after deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-27T10:11:12.124Z"),Instant.parse("2021-12-03T13:00:00.00Z")), //saturday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-27T15:11:12.124Z"),Instant.parse("2021-12-03T13:00:00.00Z")), //saturday, after deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-28T10:11:12.124Z"),Instant.parse("2021-12-03T13:00:00.00Z")), //sunday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-28T15:11:12.124Z"),Instant.parse("2021-12-03T13:00:00.00Z")), //sunday, after deadline


                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-22T10:11:12.124Z"),Instant.parse("2021-11-23T13:00:00.00Z")), //monday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-22T15:11:12.124Z"),Instant.parse("2021-11-23T13:00:00.00Z")), //monday,  after deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-23T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //tuesday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-23T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //tuesday, after deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-24T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //wednesday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-24T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //wednesday, after deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-25T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //thursday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-25T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //thursday, after deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-26T10:11:12.124Z"),Instant.parse("2021-11-30T13:00:00.00Z")), // friday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-26T15:11:12.124Z"),Instant.parse("2021-11-30T13:00:00.00Z")), // friday, after deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-27T10:11:12.124Z"),Instant.parse("2021-11-30T13:00:00.00Z")), //saturday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-27T15:11:12.124Z"),Instant.parse("2021-11-30T13:00:00.00Z")), //saturday, after deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-28T10:11:12.124Z"),Instant.parse("2021-11-30T13:00:00.00Z")), //sunday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-28T15:11:12.124Z"),Instant.parse("2021-11-30T13:00:00.00Z")), //sunday, after deadline

                // recalculating on a scheduled weekday before deadline should advance SatisfiedUntil to the following day (Instant is UTC)
                Arguments.of(allWeekAt11,Instant.parse("2021-11-22T09:11:12.124Z"),Instant.parse("2021-11-23T10:00:00.00Z")), //monday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-22T10:11:12.124Z"),Instant.parse("2021-11-23T10:00:00.00Z")), //monday,  after deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-23T09:11:12.124Z"),Instant.parse("2021-11-24T10:00:00.00Z")), //tuesday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-23T10:11:12.124Z"),Instant.parse("2021-11-24T10:00:00.00Z")), //tuesday, after deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-24T09:11:12.124Z"),Instant.parse("2021-11-25T10:00:00.00Z")), //wednesday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-24T10:11:12.124Z"),Instant.parse("2021-11-25T10:00:00.00Z")), //wednesday, after deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-25T09:11:12.124Z"),Instant.parse("2021-11-26T10:00:00.00Z")), //thursday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-25T10:11:12.124Z"),Instant.parse("2021-11-26T10:00:00.00Z")), //thursday, after deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-26T09:11:12.124Z"),Instant.parse("2021-11-27T10:00:00.00Z")), // friday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-26T10:11:12.124Z"),Instant.parse("2021-11-27T10:00:00.00Z")), // friday, after deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-27T09:11:12.124Z"),Instant.parse("2021-11-28T10:00:00.00Z")), //saturday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-27T10:11:12.124Z"),Instant.parse("2021-11-28T10:00:00.00Z")), //saturday, after deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-28T09:11:12.124Z"),Instant.parse("2021-11-29T10:00:00.00Z")), //sunday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-28T10:11:12.124Z"),Instant.parse("2021-11-29T10:00:00.00Z")) //sunday, after deadline
        );
    }
    @ParameterizedTest
    @MethodSource // arguments comes from a method that is name the same as the test
    public void givenFrequencyTimeToCalculateAndExpectedResult_Initialization_ShouldResultInExpectedTime(FrequencyModel frequencyModel, Instant timeOfRecalculate, Instant timeCalculatedResult){
        // Arrange
        FrequencyEnumerator subject = new FrequencyEnumerator(frequencyModel);
        // Act
        Instant result = subject.getSatisfiedUntilForInitialization(timeOfRecalculate);

        LocalDate now = LocalDate.now();
        //now.adjustInto()


        // Assert
        assertEquals(timeCalculatedResult, result);
    }

    private static Stream<Arguments> givenFrequencyTimeToCalculateAndExpectedResult_AlarmRemoval_ShouldResultInExpectedTime() {
        return Stream.of(
                // Instant is in UTC
                // Recalculating as clinician always advances to the following scheduled weekday, time of day is irrelevant
                Arguments.of(FridayAt14,Instant.parse("2021-11-22T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //monday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-22T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //monday,  after deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-23T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //tuesday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-23T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //tuesday, after deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-24T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //wednesday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-24T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //wednesday, after deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-25T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //thursday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-25T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //thursday, after deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-26T10:11:12.124Z"),Instant.parse("2021-12-03T13:00:00.00Z")), // friday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-26T15:11:12.124Z"),Instant.parse("2021-12-03T13:00:00.00Z")), // friday, after deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-27T10:11:12.124Z"),Instant.parse("2021-12-03T13:00:00.00Z")), //saturday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-27T15:11:12.124Z"),Instant.parse("2021-12-03T13:00:00.00Z")), //saturday, after deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-28T10:11:12.124Z"),Instant.parse("2021-12-03T13:00:00.00Z")), //sunday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-28T15:11:12.124Z"),Instant.parse("2021-12-03T13:00:00.00Z")), //sunday, after deadline


                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-22T10:11:12.124Z"),Instant.parse("2021-11-23T13:00:00.00Z")), //monday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-22T15:11:12.124Z"),Instant.parse("2021-11-23T13:00:00.00Z")), //monday,  after deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-23T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //tuesday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-23T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //tuesday, after deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-24T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //wednesday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-24T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //wednesday, after deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-25T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //thursday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-25T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //thursday, after deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-26T10:11:12.124Z"),Instant.parse("2021-11-30T13:00:00.00Z")), // friday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-26T15:11:12.124Z"),Instant.parse("2021-11-30T13:00:00.00Z")), // friday, after deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-27T10:11:12.124Z"),Instant.parse("2021-11-30T13:00:00.00Z")), //saturday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-27T15:11:12.124Z"),Instant.parse("2021-11-30T13:00:00.00Z")), //saturday, after deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-28T10:11:12.124Z"),Instant.parse("2021-11-30T13:00:00.00Z")), //sunday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-28T15:11:12.124Z"),Instant.parse("2021-11-30T13:00:00.00Z")), //sunday, after deadline

                // recalculating on a scheduled weekday before deadline should advance SatisfiedUntil to the following day (Instant is UTC)
                Arguments.of(allWeekAt11,Instant.parse("2021-11-22T09:11:12.124Z"),Instant.parse("2021-11-23T10:00:00.00Z")), //monday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-22T10:11:12.124Z"),Instant.parse("2021-11-23T10:00:00.00Z")), //monday,  after deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-23T09:11:12.124Z"),Instant.parse("2021-11-24T10:00:00.00Z")), //tuesday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-23T10:11:12.124Z"),Instant.parse("2021-11-24T10:00:00.00Z")), //tuesday, after deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-24T09:11:12.124Z"),Instant.parse("2021-11-25T10:00:00.00Z")), //wednesday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-24T10:11:12.124Z"),Instant.parse("2021-11-25T10:00:00.00Z")), //wednesday, after deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-25T09:11:12.124Z"),Instant.parse("2021-11-26T10:00:00.00Z")), //thursday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-25T10:11:12.124Z"),Instant.parse("2021-11-26T10:00:00.00Z")), //thursday, after deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-26T09:11:12.124Z"),Instant.parse("2021-11-27T10:00:00.00Z")), // friday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-26T10:11:12.124Z"),Instant.parse("2021-11-27T10:00:00.00Z")), // friday, after deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-27T09:11:12.124Z"),Instant.parse("2021-11-28T10:00:00.00Z")), //saturday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-27T10:11:12.124Z"),Instant.parse("2021-11-28T10:00:00.00Z")), //saturday, after deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-28T09:11:12.124Z"),Instant.parse("2021-11-29T10:00:00.00Z")), //sunday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-28T10:11:12.124Z"),Instant.parse("2021-11-29T10:00:00.00Z")) //sunday, after deadline
        );
    }
    @ParameterizedTest
    @MethodSource // arguments comes from a method that is name the same as the test
    public void givenFrequencyTimeToCalculateAndExpectedResult_AlarmRemoval_ShouldResultInExpectedTime(FrequencyModel frequencyModel, Instant timeOfRecalculate, Instant timeCalculatedResult){
        // Arrange
        FrequencyEnumerator subject = new FrequencyEnumerator(frequencyModel);
        // Act
        Instant result = subject.getSatisfiedUntilForAlarmRemoval(timeOfRecalculate);

        LocalDate now = LocalDate.now();
        //now.adjustInto()


        // Assert
        assertEquals(timeCalculatedResult, result);
    }

    private static Stream<Arguments> givenFrequencyTimeToCalculateAndExpectedResult_FrequencyChange_ShouldResultInExpectedTime() {
        return Stream.of(
                // Instant is in UTC
                Arguments.of(FridayAt14,Instant.parse("2021-11-22T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //monday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-22T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //monday,  after deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-23T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //tuesday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-23T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //tuesday, after deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-24T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //wednesday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-24T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //wednesday, after deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-25T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //thursday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-25T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //thursday, after deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-26T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), // friday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-26T15:11:12.124Z"),Instant.parse("2021-12-03T13:00:00.00Z")), // friday, after deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-27T10:11:12.124Z"),Instant.parse("2021-12-03T13:00:00.00Z")), //saturday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-27T15:11:12.124Z"),Instant.parse("2021-12-03T13:00:00.00Z")), //saturday, after deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-28T10:11:12.124Z"),Instant.parse("2021-12-03T13:00:00.00Z")), //sunday, before deadline
                Arguments.of(FridayAt14,Instant.parse("2021-11-28T15:11:12.124Z"),Instant.parse("2021-12-03T13:00:00.00Z")), //sunday, after deadline


                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-22T10:11:12.124Z"),Instant.parse("2021-11-23T13:00:00.00Z")), //monday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-22T15:11:12.124Z"),Instant.parse("2021-11-23T13:00:00.00Z")), //monday,  after deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-23T10:11:12.124Z"),Instant.parse("2021-11-23T13:00:00.00Z")), //tuesday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-23T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //tuesday, after deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-24T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //wednesday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-24T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //wednesday, after deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-25T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //thursday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-25T15:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), //thursday, after deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-26T10:11:12.124Z"),Instant.parse("2021-11-26T13:00:00.00Z")), // friday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-26T15:11:12.124Z"),Instant.parse("2021-11-30T13:00:00.00Z")), // friday, after deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-27T10:11:12.124Z"),Instant.parse("2021-11-30T13:00:00.00Z")), //saturday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-27T15:11:12.124Z"),Instant.parse("2021-11-30T13:00:00.00Z")), //saturday, after deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-28T10:11:12.124Z"),Instant.parse("2021-11-30T13:00:00.00Z")), //sunday, before deadline
                Arguments.of(tuesdayAndFridayAt14,Instant.parse("2021-11-28T15:11:12.124Z"),Instant.parse("2021-11-30T13:00:00.00Z")), //sunday, after deadline

                Arguments.of(allWeekAt11,Instant.parse("2021-11-22T09:11:12.124Z"),Instant.parse("2021-11-22T10:00:00.00Z")), //monday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-22T10:11:12.124Z"),Instant.parse("2021-11-23T10:00:00.00Z")), //monday,  after deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-23T09:11:12.124Z"),Instant.parse("2021-11-23T10:00:00.00Z")), //tuesday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-23T10:11:12.124Z"),Instant.parse("2021-11-24T10:00:00.00Z")), //tuesday, after deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-24T09:11:12.124Z"),Instant.parse("2021-11-24T10:00:00.00Z")), //wednesday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-24T10:11:12.124Z"),Instant.parse("2021-11-25T10:00:00.00Z")), //wednesday, after deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-25T09:11:12.124Z"),Instant.parse("2021-11-25T10:00:00.00Z")), //thursday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-25T10:11:12.124Z"),Instant.parse("2021-11-26T10:00:00.00Z")), //thursday, after deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-26T09:11:12.124Z"),Instant.parse("2021-11-26T10:00:00.00Z")), // friday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-26T10:11:12.124Z"),Instant.parse("2021-11-27T10:00:00.00Z")), // friday, after deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-27T09:11:12.124Z"),Instant.parse("2021-11-27T10:00:00.00Z")), //saturday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-27T10:11:12.124Z"),Instant.parse("2021-11-28T10:00:00.00Z")), //saturday, after deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-28T09:11:12.124Z"),Instant.parse("2021-11-28T10:00:00.00Z")), //sunday, before deadline
                Arguments.of(allWeekAt11,Instant.parse("2021-11-28T10:11:12.124Z"),Instant.parse("2021-11-29T10:00:00.00Z")) //sunday, after deadline
        );
    }
    @ParameterizedTest
    @MethodSource // arguments comes from a method that is name the same as the test
    public void givenFrequencyTimeToCalculateAndExpectedResult_FrequencyChange_ShouldResultInExpectedTime(FrequencyModel frequencyModel, Instant timeOfRecalculate, Instant timeCalculatedResult){
        // Arrange
        FrequencyEnumerator subject = new FrequencyEnumerator(frequencyModel);
        // Act
        Instant result = subject.getSatisfiedUntilForFrequencyChange(timeOfRecalculate);

        LocalDate now = LocalDate.now();
        //now.adjustInto()


        // Assert
        assertEquals(timeCalculatedResult, result);
    }

    @Test
    void tester() {
        LocalDate today = LocalDate.now();
        System.out.printf("%s\n",DayOfWeek.of(today.get(ChronoField.DAY_OF_WEEK)));

        List list = new ArrayList();
        list.add(DayOfWeek.MONDAY);
        //list.add(DayOfWeek.TUESDAY);
        //list.add(DayOfWeek.WEDNESDAY);


        Instant pointInTime = Instant.now();
        LocalDate date = LocalDate.ofInstant(pointInTime, ZoneId.of("Europe/Copenhagen"));


        System.out.println();
        System.out.printf("%s\n", date.with(TemporalAdjusters.next(DayOfWeek.TUESDAY)));


        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (list.contains(dayOfWeek)) {
            DayOfWeek successiveDayOfWeek = getSuccessiveDayOfWeek(dayOfWeek, list);
            System.out.printf("%s -> %s\n",dayOfWeek, successiveDayOfWeek);
            //date = date.with(TemporalAdjusters.next(successiveDayOfWeek));
            System.out.printf("%s -> %s\n",date, date.with(TemporalAdjusters.next(successiveDayOfWeek)));
        }

        LocalTime time = LocalTime.of(11, 0);
        System.out.printf("%s\n", time);

        LocalDateTime.of(date, time);

        System.out.printf("%s\n", DayOfWeek.MONDAY.compareTo(DayOfWeek.TUESDAY));
        System.out.printf("%s\n", DayOfWeek.MONDAY.compareTo(DayOfWeek.SUNDAY));

        list.add(DayOfWeek.WEDNESDAY);


        System.out.println("######");
        System.out.println(ChronoUnit.DAYS.between(Instant.parse("2021-11-22T10:00:00.000Z"), Instant.parse("2021-11-23T09:59:59.999Z")));
        System.out.println(ChronoUnit.DAYS.between(Instant.parse("2021-11-22T10:00:00.000Z"), Instant.parse("2021-11-23T10:00:00.000Z")));
        System.out.println(ChronoUnit.DAYS.between(Instant.parse("2021-11-22T10:00:00.000Z"), Instant.parse("2021-11-23T10:00:00.001Z")));
        System.out.println(ChronoUnit.DAYS.between(Instant.parse("2021-11-22T10:00:00.000Z"), Instant.parse("2021-11-24T09:59:59.999Z")));
        System.out.println(ChronoUnit.DAYS.between(Instant.parse("2021-11-22T10:00:00.000Z"), Instant.parse("2021-11-24T10:00:00.000Z")));

        Instant i1 = Instant.parse("2021-11-22T10:00:00.000Z");
        Instant i2 = Instant.parse("2021-11-23T00:00:00.000Z");
        Instant i3 = Instant.parse("2021-11-23T10:00:00.000Z");
        Instant i4 = Instant.parse("2021-11-23T23:00:00.000Z");

        ZonedDateTime now1 = ZonedDateTime.ofInstant(i1, ZoneId.of("Europe/Copenhagen"));
        ZonedDateTime now2 = ZonedDateTime.ofInstant(i2, ZoneId.of("Europe/Copenhagen"));
        ZonedDateTime now3 = ZonedDateTime.ofInstant(i3, ZoneId.of("Europe/Copenhagen"));
        ZonedDateTime now4 = ZonedDateTime.ofInstant(i4, ZoneId.of("Europe/Copenhagen"));
        System.out.println("====");
        System.out.println(ChronoUnit.DAYS.between(now1.toLocalDate(), now2.toLocalDate()));
        System.out.println(ChronoUnit.DAYS.between(now1.toLocalDate(), now3.toLocalDate()));
        System.out.println(ChronoUnit.DAYS.between(now1.toLocalDate(), now4.toLocalDate()));



    }

    @Test
    void test() {
        List<DayOfWeek> list = new ArrayList();
        list.add(DayOfWeek.MONDAY);
        list.add(DayOfWeek.WEDNESDAY);
        list.add(DayOfWeek.FRIDAY);

        DayOfWeek today = DayOfWeek.MONDAY;
        Stream.of(DayOfWeek.values()).forEach(dayOfWeek -> {
            System.out.printf("%s -> %s\n", dayOfWeek, getNext(list, dayOfWeek));
        });
    }

    DayOfWeek getNext(List<DayOfWeek> list, DayOfWeek day) {
        return list.stream()
            .filter(dayOfWeek -> dayOfWeek.compareTo(day) > 0)
            .findFirst()
            .orElseGet(() -> list.get(0));

    }

    private DayOfWeek getSuccessiveDayOfWeek(DayOfWeek currentDayOfWeek, List<DayOfWeek> weekDays) {
        int i = weekDays.indexOf(currentDayOfWeek);
        if (i+1 < weekDays.size()) {
            return weekDays.get(i+1);
        }
        else {
            return weekDays.get(0);
        }

    }




    @Test
    public void next_weekdaysOmitted_interpretedAsNoDeadline() {
        // Arrange
        FrequencyModel frequencyModel = buildFrequency(List.of(), LocalTime.parse("14:00"));

        FrequencyEnumerator subject = new FrequencyEnumerator(frequencyModel);

        // Act
        Instant result1 = subject.getSatisfiedUntilForAlarmRemoval(FRIDAY_AFTERNOON);
        Instant result2 = subject.getSatisfiedUntilForFrequencyChange(FRIDAY_AFTERNOON);
        Instant result3 = subject.getSatisfiedUntilForInitialization(FRIDAY_AFTERNOON);

        // Assert
        assertEquals(Instant.MAX, result1);
        assertEquals(Instant.MAX, result2);
        assertEquals(Instant.MAX, result3);
    }


    private static FrequencyModel buildFrequency(List<Weekday> weekdays, LocalTime timeOfDay) {
        FrequencyModel frequencyModel = new FrequencyModel();

        frequencyModel.setWeekdays(weekdays);
        frequencyModel.setTimeOfDay(timeOfDay);

        return frequencyModel;
    }
}