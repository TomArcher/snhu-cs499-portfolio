package com.thomasarcher.eventtrackerapp.recurrence;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.Month;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import static org.junit.Assert.assertThrows;

/**
 * Verify the correctness of recurrence occurrence generation
 * across supported recurrence frequencies, termination
 * conditions, and calendar boundary conditions.
 */
public class RecurrenceCalculatorTest {

    /**
     * Verify that a nonrecurring event generates exactly
     * one occurrence when its date falls within the
     * requested date range.
     */
    @Test
    public void nonRecurringEventWithinRange() {

        // Arrange: Define the event and requested date range.
        LocalDate startDate = LocalDate.of(2027, 1, 15);
        LocalDate rangeStart = LocalDate.of(2027, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2027, 1, 31);

        RecurrenceRule rule = RecurrenceRule.none();

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence count and date.
        assertEquals(1, occurrences.size());
        assertEquals(startDate, occurrences.get(0));
    }

    /**
     * Verify that a nonrecurring event generates no occurrences
     * when its date falls outside the requested date range.
     */
    @Test
    public void nonRecurringEventOutsideRange() {

        // Arrange: Define an event preceding the requested range.
        LocalDate startDate = LocalDate.of(2027, 1, 15);
        LocalDate rangeStart = LocalDate.of(2027, 2, 1);
        LocalDate rangeEnd = LocalDate.of(2027, 2, 28);

        RecurrenceRule rule = RecurrenceRule.none();

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify that no occurrences are returned.
        assertTrue(occurrences.isEmpty());
    }

    /**
     * Verify that a daily recurrence generates one occurrence
     * for each day within the requested date range.
     */
    @Test
    public void dailyRecurrenceEveryDay() {

        // Arrange: Define a daily recurrence with no end date.
        LocalDate startDate = LocalDate.of(2027, 1, 1);
        LocalDate rangeStart = LocalDate.of(2027, 1, 3);
        LocalDate rangeEnd = LocalDate.of(2027, 1, 7);

        RecurrenceRule rule = RecurrenceRule.daily(1);

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2027, 1, 3),
                LocalDate.of(2027, 1, 4),
                LocalDate.of(2027, 1, 5),
                LocalDate.of(2027, 1, 6),
                LocalDate.of(2027, 1, 7));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a daily recurrence with a multiday interval
     * preserves the original recurrence pattern when the
     * requested range begins between occurrences.
     */
    @Test
    public void dailyRecurrenceEveryThreeDays() {

        // Arrange: Define a recurrence that repeats every three days.
        LocalDate startDate = LocalDate.of(2027, 1, 1);
        LocalDate rangeStart = LocalDate.of(2027, 1, 5);
        LocalDate rangeEnd = LocalDate.of(2027, 1, 14);

        RecurrenceRule rule = RecurrenceRule.daily(3);

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2027, 1, 7),
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 13));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a weekly recurrence generates occurrences
     * on the specified day of the week within the requested
     * date range.
     */
    @Test
    public void weeklyRecurrenceEveryMonday() {

        // Arrange: Define a recurrence that repeats every Monday.
        LocalDate startDate = LocalDate.of(2027, 1, 4);
        LocalDate rangeStart = LocalDate.of(2027, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2027, 1, 31);

        RecurrenceRule rule = RecurrenceRule.weekly(
                1, EnumSet.of(DayOfWeek.MONDAY));

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2027, 1, 4),
                LocalDate.of(2027, 1, 11),
                LocalDate.of(2027, 1, 18),
                LocalDate.of(2027, 1, 25));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a weekly recurrence generates occurrences
     * on multiple selected days while excluding candidate
     * dates preceding the original event start date.
     */
    @Test
    public void weeklyRecurrenceMultipleDays() {

        // Arrange: Define a recurrence that repeats every
        // Monday, Wednesday, and Friday.
        LocalDate startDate = LocalDate.of(2027, 1, 6);
        LocalDate rangeStart = LocalDate.of(2027, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2027, 1, 17);

        RecurrenceRule rule = RecurrenceRule.weekly(
                1, EnumSet.of(
                        DayOfWeek.MONDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.FRIDAY));

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2027, 1, 6),
                LocalDate.of(2027, 1, 8),
                LocalDate.of(2027, 1, 11),
                LocalDate.of(2027, 1, 13),
                LocalDate.of(2027, 1, 15));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a weekly recurrence with a multiweek interval
     * generates occurrences only during the appropriate weeks
     * while preserving the original recurrence pattern.
     */
    @Test
    public void weeklyRecurrenceEveryTwoWeeks() {

        // Arrange: Define a recurrence that repeats on
        // Monday and Friday every two weeks.
        LocalDate startDate = LocalDate.of(2027, 1, 4);
        LocalDate rangeStart = LocalDate.of(2027, 1, 12);
        LocalDate rangeEnd = LocalDate.of(2027, 2, 7);

        RecurrenceRule rule = RecurrenceRule.weekly(
                2, EnumSet.of(
                        DayOfWeek.MONDAY,
                        DayOfWeek.FRIDAY));

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2027, 1, 18),
                LocalDate.of(2027, 1, 22),
                LocalDate.of(2027, 2, 1),
                LocalDate.of(2027, 2, 5));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a monthly recurrence generates occurrences
     * on the specified day of each month within the requested
     * date range.
     */
    @Test
    public void monthlyRecurrenceFixedDay() {

        // Arrange: Define a recurrence that repeats on
        // the 15th of every month.
        LocalDate startDate = LocalDate.of(2027, 1, 15);
        LocalDate rangeStart = LocalDate.of(2027, 2, 1);
        LocalDate rangeEnd = LocalDate.of(2027, 5, 31);

        RecurrenceRule rule = RecurrenceRule.monthlyByDay(1, 15);

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2027, 2, 15),
                LocalDate.of(2027, 3, 15),
                LocalDate.of(2027, 4, 15),
                LocalDate.of(2027, 5, 15));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a monthly recurrence accommodates shorter
     * months without introducing date drift into subsequent
     * occurrences.
     */
    @Test
    public void monthlyRecurrenceVaryingMonthLengths() {

        // Arrange: Define a recurrence that repeats on
        // the 31st of every month.
        LocalDate startDate = LocalDate.of(2027, 1, 31);
        LocalDate rangeStart = LocalDate.of(2027, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2027, 5, 31);

        RecurrenceRule rule = RecurrenceRule.monthlyByDay(1, 31);

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2027, 1, 31),
                LocalDate.of(2027, 2, 28),
                LocalDate.of(2027, 3, 31),
                LocalDate.of(2027, 4, 30),
                LocalDate.of(2027, 5, 31));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a monthly recurrence with a multimonth
     * interval preserves the original recurrence pattern
     * across calendar year boundaries.
     */
    @Test
    public void monthlyRecurrenceEveryThreeMonths() {

        // Arrange: Define a recurrence that repeats on
        // the 15th of every third month.
        LocalDate startDate = LocalDate.of(2027, 1, 15);
        LocalDate rangeStart = LocalDate.of(2027, 5, 1);
        LocalDate rangeEnd = LocalDate.of(2028, 2, 29);

        RecurrenceRule rule = RecurrenceRule.monthlyByDay(3, 15);

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2027, 7, 15),
                LocalDate.of(2027, 10, 15),
                LocalDate.of(2028, 1, 15));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a monthly recurrence based on a relative
     * weekday generates the correct occurrence dates
     * across multiple months.
     */
    @Test
    public void monthlyRecurrenceSecondTuesday() {

        // Arrange: Define a recurrence that repeats on
        // the second Tuesday of every month.
        LocalDate startDate = LocalDate.of(2027, 1, 12);
        LocalDate rangeStart = LocalDate.of(2027, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2027, 5, 31);

        RecurrenceRule rule = RecurrenceRule.monthlyByRelativeDay(
                1,
                RelativePosition.SECOND,
                RelativeDayType.TUESDAY);

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2027, 1, 12),
                LocalDate.of(2027, 2, 9),
                LocalDate.of(2027, 3, 9),
                LocalDate.of(2027, 4, 13),
                LocalDate.of(2027, 5, 11));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a monthly recurrence based on the last
     * matching weekday generates the correct occurrence
     * dates across months of varying lengths.
     */
    @Test
    public void monthlyRecurrenceLastFriday() {

        // Arrange: Define a recurrence that repeats on
        // the last Friday of every month.
        LocalDate startDate = LocalDate.of(2027, 1, 29);
        LocalDate rangeStart = LocalDate.of(2027, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2027, 5, 31);

        RecurrenceRule rule = RecurrenceRule.monthlyByRelativeDay(
                1,
                RelativePosition.LAST,
                RelativeDayType.FRIDAY);

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2027, 1, 29),
                LocalDate.of(2027, 2, 26),
                LocalDate.of(2027, 3, 26),
                LocalDate.of(2027, 4, 30),
                LocalDate.of(2027, 5, 28));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a monthly recurrence based on the last
     * weekday correctly excludes Saturdays and Sundays.
     */
    @Test
    public void monthlyRecurrenceLastWeekday() {

        // Arrange: Define a recurrence that repeats on
        // the last weekday of every month.
        LocalDate startDate = LocalDate.of(2027, 1, 29);
        LocalDate rangeStart = LocalDate.of(2027, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2027, 5, 31);

        RecurrenceRule rule = RecurrenceRule.monthlyByRelativeDay(
                1,
                RelativePosition.LAST,
                RelativeDayType.WEEKDAY);

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2027, 1, 29),
                LocalDate.of(2027, 2, 26),
                LocalDate.of(2027, 3, 31),
                LocalDate.of(2027, 4, 30),
                LocalDate.of(2027, 5, 31));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a monthly recurrence based on the first
     * weekend day correctly matches Saturdays and Sundays
     * while excluding weekdays.
     */
    @Test
    public void monthlyRecurrenceFirstWeekendDay() {

        // Arrange: Define a recurrence that repeats on
        // the first weekend day of every month.
        LocalDate startDate = LocalDate.of(2027, 1, 2);
        LocalDate rangeStart = LocalDate.of(2027, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2027, 8, 31);

        RecurrenceRule rule = RecurrenceRule.monthlyByRelativeDay(
                1,
                RelativePosition.FIRST,
                RelativeDayType.WEEKEND_DAY);

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2027, 1, 2),
                LocalDate.of(2027, 2, 6),
                LocalDate.of(2027, 3, 6),
                LocalDate.of(2027, 4, 3),
                LocalDate.of(2027, 5, 1),
                LocalDate.of(2027, 6, 5),
                LocalDate.of(2027, 7, 3),
                LocalDate.of(2027, 8, 1));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a monthly recurrence based on the last
     * calendar day correctly handles varying month lengths
     * and leap years.
     */
    @Test
    public void monthlyRecurrenceLastCalendarDay() {

        // Arrange: Define a recurrence that repeats on
        // the last calendar day of every month.
        LocalDate startDate = LocalDate.of(2027, 12, 31);
        LocalDate rangeStart = LocalDate.of(2027, 12, 1);
        LocalDate rangeEnd = LocalDate.of(2028, 4, 30);

        RecurrenceRule rule = RecurrenceRule.monthlyByRelativeDay(
                1,
                RelativePosition.LAST,
                RelativeDayType.DAY);

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2027, 12, 31),
                LocalDate.of(2028, 1, 31),
                LocalDate.of(2028, 2, 29),
                LocalDate.of(2028, 3, 31),
                LocalDate.of(2028, 4, 30));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a yearly recurrence based on a fixed
     * month and day generates the correct occurrence dates
     * across multiple calendar years.
     */
    @Test
    public void yearlyRecurrenceFixedDay() {

        // Arrange: Define a recurrence that repeats on
        // July 15 of every year.
        LocalDate startDate = LocalDate.of(2027, 7, 15);
        LocalDate rangeStart = LocalDate.of(2028, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2031, 12, 31);

        RecurrenceRule rule = RecurrenceRule.yearlyByDay(
                1, Month.JULY, 15);

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2028, 7, 15),
                LocalDate.of(2029, 7, 15),
                LocalDate.of(2030, 7, 15),
                LocalDate.of(2031, 7, 15));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a yearly recurrence scheduled for
     * February 29 accommodates non-leap years without
     * introducing date drift into subsequent occurrences.
     */
    @Test
    public void yearlyRecurrenceLeapDay() {

        // Arrange: Define a recurrence that repeats on
        // February 29 of every year.
        LocalDate startDate = LocalDate.of(2028, 2, 29);
        LocalDate rangeStart = LocalDate.of(2028, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2032, 12, 31);

        RecurrenceRule rule = RecurrenceRule.yearlyByDay(
                1, Month.FEBRUARY, 29);

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2028, 2, 29),
                LocalDate.of(2029, 2, 28),
                LocalDate.of(2030, 2, 28),
                LocalDate.of(2031, 2, 28),
                LocalDate.of(2032, 2, 29));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a yearly recurrence based on a relative
     * weekday within a specified month generates the
     * correct occurrence dates across multiple years.
     */
    @Test
    public void yearlyRecurrenceFourthThursday() {

        // Arrange: Define a recurrence that repeats on
        // the fourth Thursday of November every year.
        LocalDate startDate = LocalDate.of(2027, 11, 25);
        LocalDate rangeStart = LocalDate.of(2027, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2031, 12, 31);

        RecurrenceRule rule = RecurrenceRule.yearlyByRelativeDay(
                1,
                Month.NOVEMBER,
                RelativePosition.FOURTH,
                RelativeDayType.THURSDAY);

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2027, 11, 25),
                LocalDate.of(2028, 11, 23),
                LocalDate.of(2029, 11, 22),
                LocalDate.of(2030, 11, 28),
                LocalDate.of(2031, 11, 27));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a yearly recurrence with a multiyear
     * interval preserves the original recurrence pattern
     * when the requested range begins during an inactive year.
     */
    @Test
    public void yearlyRecurrenceEveryThreeYears() {

        // Arrange: Define a recurrence that repeats on
        // July 15 of every third year.
        LocalDate startDate = LocalDate.of(2027, 7, 15);
        LocalDate rangeStart = LocalDate.of(2031, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2040, 12, 31);

        RecurrenceRule rule = RecurrenceRule.yearlyByDay(
                3, Month.JULY, 15);

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2033, 7, 15),
                LocalDate.of(2036, 7, 15),
                LocalDate.of(2039, 7, 15));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a recurrence with an explicit end date
     * includes an occurrence on that date but excludes
     * subsequent occurrences.
     */
    @Test
    public void dailyRecurrenceWithEndDate() {

        // Arrange: Define a daily recurrence that
        // terminates on January 5.
        LocalDate startDate = LocalDate.of(2027, 1, 1);
        LocalDate rangeStart = LocalDate.of(2027, 1, 3);
        LocalDate rangeEnd = LocalDate.of(2027, 1, 10);

        RecurrenceRule rule = RecurrenceRule.daily(1)
                .endBy(LocalDate.of(2027, 1, 5));

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2027, 1, 3),
                LocalDate.of(2027, 1, 4),
                LocalDate.of(2027, 1, 5));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a recurrence with an occurrence limit
     * accounts for occurrences preceding the requested
     * date range.
     */
    @Test
    public void dailyRecurrenceWithOccurrenceCount() {

        // Arrange: Define a daily recurrence that
        // terminates after five occurrences.
        LocalDate startDate = LocalDate.of(2027, 1, 1);
        LocalDate rangeStart = LocalDate.of(2027, 1, 3);
        LocalDate rangeEnd = LocalDate.of(2027, 1, 10);

        RecurrenceRule rule = RecurrenceRule.daily(1)
                .endAfter(5);

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2027, 1, 3),
                LocalDate.of(2027, 1, 4),
                LocalDate.of(2027, 1, 5));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a weekly recurrence with multiple
     * recurrence days correctly accounts for preceding
     * occurrences and a partial first week.
     */
    @Test
    public void weeklyRecurrenceWithOccurrenceCount() {

        // Arrange: Define a weekly recurrence that repeats
        // on Monday, Wednesday, and Friday and terminates
        // after seven occurrences.
        LocalDate startDate = LocalDate.of(2027, 1, 6);
        LocalDate rangeStart = LocalDate.of(2027, 1, 18);
        LocalDate rangeEnd = LocalDate.of(2027, 1, 31);

        RecurrenceRule rule = RecurrenceRule.weekly(
                        1,
                        EnumSet.of(
                                DayOfWeek.MONDAY,
                                DayOfWeek.WEDNESDAY,
                                DayOfWeek.FRIDAY))
                .endAfter(7);

        List<LocalDate> expected = Arrays.asList(
                LocalDate.of(2027, 1, 18),
                LocalDate.of(2027, 1, 20));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify the occurrence dates and chronological order.
        assertEquals(expected, occurrences);
    }

    /**
     * Verify that a recurrence returns no occurrences
     * when its occurrence limit has been reached before
     * the requested date range begins.
     */
    @Test
    public void dailyRecurrenceExhaustedOccurrenceCount() {

        // Arrange: Define a daily recurrence that
        // terminates after five occurrences.
        LocalDate startDate = LocalDate.of(2027, 1, 1);
        LocalDate rangeStart = LocalDate.of(2027, 1, 10);
        LocalDate rangeEnd = LocalDate.of(2027, 1, 31);

        RecurrenceRule rule = RecurrenceRule.daily(1)
                .endAfter(5);

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify that no occurrences are returned.
        assertTrue(occurrences.isEmpty());
    }

    /**
     * Verify that a recurrence returns no occurrences
     * when its termination date precedes the requested
     * date range.
     */
    @Test
    public void dailyRecurrenceExpiredEndDate() {

        // Arrange: Define a daily recurrence that
        // terminates before the requested date range.
        LocalDate startDate = LocalDate.of(2027, 1, 1);
        LocalDate rangeStart = LocalDate.of(2027, 1, 10);
        LocalDate rangeEnd = LocalDate.of(2027, 1, 31);

        RecurrenceRule rule = RecurrenceRule.daily(1)
                .endBy(LocalDate.of(2027, 1, 5));

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify that no occurrences are returned.
        assertTrue(occurrences.isEmpty());
    }

    /**
     * Verify that a recurrence returns no occurrences
     * when the requested date range precedes the
     * event start date.
     */
    @Test
    public void dailyRecurrenceBeforeEventStartDate() {

        // Arrange: Define a daily recurrence that begins
        // after the requested date range.
        LocalDate startDate = LocalDate.of(2027, 2, 1);
        LocalDate rangeStart = LocalDate.of(2027, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2027, 1, 31);

        RecurrenceRule rule = RecurrenceRule.daily(1);

        // Act: Generate the occurrence dates.
        List<LocalDate> occurrences =
                RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule);

        // Assert: Verify that no occurrences are returned.
        assertTrue(occurrences.isEmpty());
    }

    /**
     * Verify that occurrence generation rejects a
     * requested date range whose ending date precedes
     * its starting date.
     */
    @Test
    public void invalidRequestedDateRange() {

        // Arrange: Define a requested date range
        // with an ending date before its starting date.
        LocalDate startDate = LocalDate.of(2027, 1, 1);
        LocalDate rangeStart = LocalDate.of(2027, 1, 31);
        LocalDate rangeEnd = LocalDate.of(2027, 1, 1);

        RecurrenceRule rule = RecurrenceRule.daily(1);

        // Act and Assert: Verify that occurrence generation
        // throws the expected exception type.
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule));

        // Assert: Verify the reason for the exception.
        assertEquals(
                "Range end cannot precede range start.",
                exception.getMessage());
    }

    /**
     * Verify that occurrence generation rejects a
     * recurrence termination date that precedes the
     * event start date.
     */
    @Test
    public void invalidRecurrenceEndDate() {

        // Arrange: Define a recurrence that terminates
        // before the event start date.
        LocalDate startDate = LocalDate.of(2027, 1, 15);
        LocalDate rangeStart = LocalDate.of(2027, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2027, 1, 31);

        RecurrenceRule rule = RecurrenceRule.daily(1)
                .endBy(LocalDate.of(2027, 1, 10));

        // Act and Assert: Verify that occurrence generation
        // throws the expected exception type.
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule));

        // Assert: Verify the reason for the exception.
        assertEquals(
                "Recurrence end date cannot precede event start date.",
                exception.getMessage());
    }

    /**
     * Verify that occurrence generation rejects a
     * null recurrence rule.
     */
    @Test
    public void invalidNullRecurrenceRule() {

        // Arrange: Define a valid date range and
        // a null recurrence rule.
        LocalDate startDate = LocalDate.of(2027, 1, 1);
        LocalDate rangeStart = LocalDate.of(2027, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2027, 1, 31);

        RecurrenceRule rule = null;

        // Act and Assert: Verify that occurrence generation
        // throws the expected exception type.
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> RecurrenceCalculator.generateOccurrences(
                        startDate, rangeStart, rangeEnd, rule));

        // Assert: Verify the reason for the exception.
        assertEquals(
                "Arguments cannot be null.",
                exception.getMessage());
    }
}