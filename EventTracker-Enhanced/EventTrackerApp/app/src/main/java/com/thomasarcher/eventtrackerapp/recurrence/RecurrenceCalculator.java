package com.thomasarcher.eventtrackerapp.recurrence;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.time.temporal.ChronoUnit;

/**
 * Calculates occurrence dates from RecurrenceRules object.
 */
public final class RecurrenceCalculator {

    /**
     * Prevent instantiation of this utility class.
     * Access all recurrence calculation methods statically.
     */
    private RecurrenceCalculator() {
    }

    /**
     * Calculate the date of a fixed day within a specified month.
     *
     * Adjust the requested day to the last valid day of the
     * month if the requested day exceeds the month's length.
     *
     * Time complexity: O(1).
     * Auxiliary space complexity: O(1).
     *
     * @param yearMonth the year and month of the occurrence
     * @param dayOfMonth the requested day of the month (1-31)
     * @return the calculated occurrence date
     */
    private static LocalDate calculateFixedDay(
            YearMonth yearMonth,
            int dayOfMonth) {

        // Limit the requested day to the last valid day of
        // the month. YearMonth.lengthOfMonth() automatically
        // accounts for varying month lengths and leap years.
        int day = Math.min(dayOfMonth, yearMonth.lengthOfMonth());

        return yearMonth.atDay(day);
    }

    /**
     * Determine whether a date matches the specified relative day type.
     *
     * Treat weekdays as Monday through Friday and weekend days
     * as Saturday and Sunday.
     *
     * Time complexity: O(1).
     * Auxiliary space complexity: O(1).
     *
     * @param date the date to evaluate
     * @param dayType the type of day to match
     * @return true if the date matches the specified day type;
     *         otherwise, false
     */
    private static boolean matchesDayType(
            LocalDate date,
            RelativeDayType dayType) {

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        switch (dayType) {
            // All days pass this test.
            case DAY:
                return true;

            // Test for weekday by ensuring day is not
            // Saturday and not Sunday.
            case WEEKDAY:
                return (dayOfWeek != DayOfWeek.SATURDAY
                        && dayOfWeek != DayOfWeek.SUNDAY);

            // Test for weekend day by ensuring day is
            // either Saturday or Sunday.
            case WEEKEND_DAY:
                return (dayOfWeek == DayOfWeek.SATURDAY
                        || dayOfWeek == DayOfWeek.SUNDAY);

            // Handle all 7 named days of the week.
            default:
                return dayOfWeek.name().equals(dayType.name());
        }
    }

    /**
     * Calculate the date of a relative day within a specified month.
     *
     * Search backward when the requested position is LAST.
     * Otherwise, search forward and count matching dates until
     * reaching the requested ordinal position.
     *
     * Time complexity: O(1), with at most 31 iterations.
     * Auxiliary space complexity: O(1).
     *
     * @param yearMonth the year and month to search
     * @param position the ordinal position of the matching day
     * @param dayType the type of day to match
     * @return the date matching the specified recurrence criteria
     * @throws IllegalStateException if no matching date is found
     */
    private static LocalDate calculateRelativeDay(
            YearMonth yearMonth,
            RelativePosition position,
            RelativeDayType dayType) {

        if (position == RelativePosition.LAST) {

            // Search backward from the last day of the month and return
            // the first matching date.
            for (int day = yearMonth.lengthOfMonth(); day >= 1; day--) {
                LocalDate date = yearMonth.atDay(day);

                if (matchesDayType(date, dayType)) {
                    return date;
                }
            }
        } else {

            // Convert the zer-based ordinal to a one-based position.
            int target = position.ordinal() + 1;

            // Initialize the number of matching dates.
            int count = 0;

            // Search forward from the first day of the month, counting matching
            // dates until we reach the desired position.
            for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {

                LocalDate date = yearMonth.atDay(day);

                if (matchesDayType(date, dayType)) {

                    // Increment the number of matching dates.
                    count++;

                    // Return the date when the requested position
                    // is reached.
                    if (count == target) {
                        return date;
                    }
                }
            }
        }

        //  If you get this far, we did not find a matching date.
        throw new IllegalStateException("No matching date found.");
    }

    /**
     * Calculate an occurrence date within the specified month
     * using a fixed-day or relative-day recurrence rule.
     *
     * Reuse this method for both monthly and yearly recurrence.
     *
     * Time complexity: O(1).
     * Auxiliary space complexity: O(1).
     *
     * @param yearMonth the year and month of the occurrence
     * @param rule a valid monthly or yearly recurrence rule
     * @return the calculated occurrence date
     * @throws IllegalStateException if the monthly recurrence
     *         type is unsupported
     */
    private static LocalDate calculateMonthlyDate(
            YearMonth yearMonth, RecurrenceRule rule) {

        // Select the appropriate date calculation algorithm.
        switch (rule.getMonthlyType()) {
            case DAY_OF_MONTH:
                return calculateFixedDay(
                        yearMonth,
                        rule.getDayOfMonth());

            case RELATIVE_DAY:
                return calculateRelativeDay(
                        yearMonth,
                        rule.getRelativePosition(),
                        rule.getRelativeDayType());

            default:
                throw new IllegalStateException(
                        "Unsupported monthly recurrence type.");
        }
    }

    /**
     * Calculate the date of a monthly recurrence for the
     * specified interval index.
     *
     * Calculate the target month relative to the original
     * start date to prevent cumulative date drift.
     *
     * Time complexity: O(1).
     * Auxiliary space complexity: O(1).
     *
     * @param startDate the original event start date
     * @param rule a valid monthly recurrence rule
     * @param index the zero-based recurrence interval index
     * @return the calculated occurrence date
     */
    private static LocalDate calculateMonthlyOccurrence(
            LocalDate startDate,
            RecurrenceRule rule,
            long index) {

        // Calculate the number of months from the original
        // event start date.
        long monthOffset = index * rule.getInterval();

        // Determine the target year and month.
        YearMonth yearMonth = YearMonth.from(startDate)
                .plusMonths(monthOffset);

        // Apply the fixed-day or relative-day recurrence rule.
        return calculateMonthlyDate(yearMonth, rule);
    }

    /**
     * Calculate the date of a yearly recurrence for the
     * specified interval index.
     *
     * Calculate the target year relative to the original
     * start date to prevent cumulative date drift.
     *
     * Time complexity: O(1).
     * Auxiliary space complexity: O(1).
     *
     * @param startDate the original event start date
     * @param rule a valid yearly recurrence rule
     * @param index the zero-based recurrence interval index
     * @return the calculated occurrence date
     */
    private static LocalDate calculateYearlyOccurrence(
            LocalDate startDate,
            RecurrenceRule rule,
            long index) {

        // Calculate the number of years from the original
        // event start date.
        long yearOffset = index * rule.getInterval();

        // Determine the target year and month.
        YearMonth yearMonth = YearMonth
                .of(startDate.getYear(), rule.getYearlyMonth())
                .plusYears(yearOffset);

        // Apply the fixed-day or relative-day recurrence rule.
        return calculateMonthlyDate(yearMonth, rule);
    }

    /**
     * Calculate the date of a daily recurrence for the
     * specified interval index.
     *
     * Time complexity: O(1).
     * Auxiliary space complexity: O(1).
     *
     * @param startDate the original event start date
     * @param rule a valid daily recurrence rule
     * @param index the zero-based recurrence interval index
     * @return the calculated occurrence date
     */
    private static LocalDate calculateDailyOccurrence(
            LocalDate startDate,
            RecurrenceRule rule,
            long index) {

        // Calculate the number of days from the original
        // event start date.
        long dayOffset = index * rule.getInterval();

        // Add the calculated offset to the original date.
        return startDate.plusDays(dayOffset);
    }

    /**
     * Calculate the first day of a weekly recurrence interval.
     *
     * Use Monday as the beginning of each calendar week.
     * Calculate the target week relative to the original
     * event start date.
     *
     * Time complexity: O(1).
     * Auxiliary space complexity: O(1).
     *
     * @param startDate the original event start date
     * @param rule a valid weekly recurrence rule
     * @param index the zero-based recurrence interval index
     * @return the first day of the calculated recurrence week
     */
    private static LocalDate calculateWeeklyIntervalStart(
            LocalDate startDate,
            RecurrenceRule rule,
            long index) {

        // Determine the Monday of the original event's week.
        LocalDate firstWeek = startDate.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // Calculate the number of weeks from the original week.
        long weekOffset = index * rule.getInterval();

        // Return the first day of the target recurrence week.
        return firstWeek.plusWeeks(weekOffset);
    }

    /**
     * Calculate the occurrence dates within a weekly recurrence
     * interval based on the selected days of the week.
     *
     * Return the dates in chronological order.
     *
     * Time complexity: O(1), with at most seven iterations.
     * Auxiliary space complexity: O(1).
     *
     * @param startDate the original event start date
     * @param rule a valid weekly recurrence rule
     * @param index the zero-based recurrence interval index
     * @return the occurrence dates within the specified week
     */
    private static List<LocalDate> calculateWeeklyOccurrences(
            LocalDate startDate,
            RecurrenceRule rule,
            long index) {

        // Determine the first day of the recurrence week.
        LocalDate weekStart = calculateWeeklyIntervalStart(
                startDate, rule, index);

        List<LocalDate> occurrences = new ArrayList<>();

        // Calculate an occurrence date for each selected weekday.
        for (DayOfWeek day : rule.getWeeklyDays()) {

            // Convert the one-based day value to a zero-based
            // offset relative to Monday.
            int dayOffset = day.getValue() - 1;

            occurrences.add(weekStart.plusDays(dayOffset));
        }

        return occurrences;
    }

    /**
     * Calculate the candidate occurrence dates within the
     * specified recurrence interval.
     *
     * Delegate to the appropriate calculation algorithm
     * based on the recurrence frequency.
     *
     * Time complexity: O(1).
     * Auxiliary space complexity: O(1).
     *
     * @param startDate the original event start date
     * @param rule the recurrence rule
     * @param index the zero-based recurrence interval index
     * @return the candidate dates in chronological order
     */
    private static List<LocalDate> calculateIntervalOccurrences(
            LocalDate startDate,
            RecurrenceRule rule,
            long index) {

        switch (rule.getFrequency()) {
            case NONE:
                // Return the original date only once.
                return index == 0
                        ? Collections.singletonList(startDate)
                        : Collections.emptyList();

            case DAILY:
                return Collections.singletonList(
                        calculateDailyOccurrence(
                                startDate, rule, index));

            case WEEKLY:
                return calculateWeeklyOccurrences(
                        startDate, rule, index);

            case MONTHLY:
                return Collections.singletonList(
                        calculateMonthlyOccurrence(
                                startDate, rule, index));

            case YEARLY:
                return Collections.singletonList(
                        calculateYearlyOccurrence(
                                startDate, rule, index));

            default:
                throw new IllegalStateException(
                        "Unsupported recurrence frequency.");
        }
    }

    /**
     * Calculate the starting recurrence index for a daily
     * recurrence based on the requested date range.
     *
     * Skip unnecessary calculations for recurrence dates
     * preceding the requested range.
     *
     * Time complexity: O(1).
     * Auxiliary space complexity: O(1).
     *
     * @param startDate the original event start date
     * @param rangeStart the beginning of the requested date range
     * @param rule a valid daily recurrence rule
     * @return the zero-based starting recurrence index
     */
    private static long calculateDailyStartIndex(
            LocalDate startDate,
            LocalDate rangeStart,
            RecurrenceRule rule) {

        // Calculate the number of days between the original
        // event start date and the requested range start.
        long days = ChronoUnit.DAYS.between(startDate, rangeStart);

        // Divide by the recurrence interval and prevent
        // negative recurrence indices.
        return Math.max(0, days / rule.getInterval());
    }

    /**
     * Calculate the starting recurrence index for a weekly
     * recurrence based on the requested date range.
     *
     * Normalize both dates to the beginning of their respective
     * calendar weeks before calculating the difference.
     *
     * Time complexity: O(1).
     * Auxiliary space complexity: O(1).
     *
     * @param startDate the original event start date
     * @param rangeStart the beginning of the requested date range
     * @param rule a valid weekly recurrence rule
     * @return the zero-based starting recurrence index
     */
    private static long calculateWeeklyStartIndex(
            LocalDate startDate,
            LocalDate rangeStart,
            RecurrenceRule rule) {

        // Determine the Monday of the original event's week.
        LocalDate firstWeek = startDate.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // Determine the Monday of the requested range's first week.
        LocalDate rangeWeek = rangeStart.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // Calculate the number of weeks between the two dates.
        long weeks = ChronoUnit.WEEKS.between(firstWeek, rangeWeek);

        // Divide by the recurrence interval and prevent
        // negative recurrence indices.
        return Math.max(0, weeks / rule.getInterval());
    }

    /**
     * Calculate the starting recurrence index for a monthly
     * recurrence based on the requested date range.
     *
     * Convert both dates to YearMonth objects to calculate
     * the difference independently of the day of the month.
     *
     * Time complexity: O(1).
     * Auxiliary space complexity: O(1).
     *
     * @param startDate the original event start date
     * @param rangeStart the beginning of the requested date range
     * @param rule a valid monthly recurrence rule
     * @return the zero-based starting recurrence index
     */
    private static long calculateMonthlyStartIndex(
            LocalDate startDate,
            LocalDate rangeStart,
            RecurrenceRule rule) {

        // Extract the year and month from each date.
        YearMonth firstMonth = YearMonth.from(startDate);
        YearMonth rangeMonth = YearMonth.from(rangeStart);

        // Calculate the number of calendar months between them.
        long months = ChronoUnit.MONTHS.between(
                firstMonth, rangeMonth);

        // Divide by the recurrence interval and prevent
        // negative recurrence indices.
        return Math.max(0, months / rule.getInterval());
    }

    /**
     * Calculate the starting recurrence index for a yearly
     * recurrence based on the requested date range.
     *
     * Calculate the difference between calendar years
     * independently of the month and day.
     *
     * Time complexity: O(1).
     * Auxiliary space complexity: O(1).
     *
     * @param startDate the original event start date
     * @param rangeStart the beginning of the requested date range
     * @param rule a valid yearly recurrence rule
     * @return the zero-based starting recurrence index
     */
    private static long calculateYearlyStartIndex(
            LocalDate startDate,
            LocalDate rangeStart,
            RecurrenceRule rule) {

        // Calculate the difference between calendar years.
        long years = (long) rangeStart.getYear()
                - startDate.getYear();

        // Divide by the recurrence interval and prevent
        // negative recurrence indices.
        return Math.max(0, years / rule.getInterval());
    }

    /**
     * Calculate the starting recurrence index for the
     * requested date range.
     *
     * Delegate to the appropriate starting-index calculation
     * based on the recurrence frequency.
     *
     * Time complexity: O(1).
     * Auxiliary space complexity: O(1).
     *
     * @param startDate the original event start date
     * @param rangeStart the beginning of the requested date range
     * @param rule the recurrence rule
     * @return the zero-based starting recurrence index
     */
    private static long calculateStartIndex(
            LocalDate startDate,
            LocalDate rangeStart,
            RecurrenceRule rule) {

        // Select the appropriate starting-index algorithm.
        switch (rule.getFrequency()) {
            case NONE:
                return 0;

            case DAILY:
                return calculateDailyStartIndex(
                        startDate, rangeStart, rule);

            case WEEKLY:
                return calculateWeeklyStartIndex(
                        startDate, rangeStart, rule);

            case MONTHLY:
                return calculateMonthlyStartIndex(
                        startDate, rangeStart, rule);

            case YEARLY:
                return calculateYearlyStartIndex(
                        startDate, rangeStart, rule);

            default:
                throw new IllegalStateException(
                        "Unsupported recurrence frequency.");
        }
    }

    /**
     * Calculate the number of valid occurrences preceding
     * the specified recurrence index.
     *
     * Exclude candidate dates that precede the original
     * event start date.
     *
     * Time complexity: O(1).
     * Auxiliary space complexity: O(1).
     *
     * @param startDate the original event start date
     * @param rule the recurrence rule
     * @param index the zero-based recurrence index
     * @return the number of preceding occurrences
     */
    private static long calculatePrecedingOccurrences(
            LocalDate startDate,
            RecurrenceRule rule,
            long index) {

        // No occurrences precede the first recurrence index.
        if (index == 0) {
            return 0;
        }

        // Handle nonrecurring events separately.
        if (rule.getFrequency() == RecurrenceFrequency.NONE) {
            return 1;
        }

        // Determine the number of occurrences per repetition
        // of the recurrence pattern.
        int occurrencesPerRepetition =
                rule.getFrequency() == RecurrenceFrequency.WEEKLY
                        ? rule.getWeeklyDays().size()
                        : 1;

        long count = index * occurrencesPerRepetition;

        // Exclude candidate dates in the first repetition
        // that precede the original event start date.
        for (LocalDate date :
                calculateIntervalOccurrences(startDate, rule, 0)) {

            if (date.isBefore(startDate)) {
                count--;
            }
        }

        return count;
    }

    /**
     * Calculate the effective end date for occurrence generation.
     *
     * Limit the requested date range to the recurrence end date
     * when the rule specifies an end-date termination condition.
     *
     * Time complexity: O(1).
     * Auxiliary space complexity: O(1).
     *
     * @param rangeEnd the end of the requested date range
     * @param rule the recurrence rule
     * @return the last permissible date for occurrence generation
     */
    private static LocalDate calculateEffectiveEndDate(
            LocalDate rangeEnd,
            RecurrenceRule rule) {

        // Use the recurrence end date if it precedes
        // the requested range end date.
        if (rule.getEndType() == RecurrenceEndType.END_DATE
                && rule.getEndDate().isBefore(rangeEnd)) {

            return rule.getEndDate();
        }

        // Otherwise, use the requested range end date.
        return rangeEnd;
    }

    /**
     * Calculate the effective start date for occurrence generation.
     *
     * Prevent occurrence generation before the original event
     * start date or the beginning of the requested date range.
     *
     * Time complexity: O(1).
     * Auxiliary space complexity: O(1).
     *
     * @param startDate the original event start date
     * @param rangeStart the beginning of the requested date range
     * @return the earliest permissible occurrence date
     */
    private static LocalDate calculateEffectiveStartDate(
            LocalDate startDate,
            LocalDate rangeStart) {

        // Select the later of the two lower date boundaries.
        return startDate.isAfter(rangeStart)
                ? startDate
                : rangeStart;
    }

    /**
     * Validate the arguments required for occurrence generation.
     *
     * Time complexity: O(1).
     * Auxiliary space complexity: O(1).
     *
     * @param startDate the original event start date
     * @param rangeStart the beginning of the requested date range
     * @param rangeEnd the end of the requested date range
     * @param rule the recurrence rule
     * @throws IllegalArgumentException if an argument is invalid
     */
    private static void validateArguments(
            LocalDate startDate,
            LocalDate rangeStart,
            LocalDate rangeEnd,
            RecurrenceRule rule) {

        // Ensure all required arguments are provided.
        if (startDate == null || rangeStart == null
                || rangeEnd == null || rule == null) {

            throw new IllegalArgumentException(
                    "Arguments cannot be null.");
        }

        // Ensure the requested date range is chronological.
        if (rangeEnd.isBefore(rangeStart)) {
            throw new IllegalArgumentException(
                    "Range end cannot precede range start.");
        }

        // Ensure the recurrence end date does not precede
        // the original event start date.
        if (rule.getFrequency() != RecurrenceFrequency.NONE
                && rule.getEndType() == RecurrenceEndType.END_DATE
                && rule.getEndDate().isBefore(startDate)) {

            throw new IllegalArgumentException(
                    "Recurrence end date cannot precede event start date.");
        }
    }

    /**
     * Generate occurrence dates within an inclusive date range
     * according to the specified recurrence rule.
     *
     * Calculate the starting recurrence index directly to avoid
     * traversing preceding repetitions of the recurrence pattern.
     *
     * Time complexity: O(n), where n is the number of recurrence
     * pattern repetitions examined.
     *
     * Auxiliary space complexity: O(1), excluding the returned
     * collection.
     *
     * Output space complexity: O(k), where k is the number of
     * occurrence dates returned.
     *
     * @param startDate the original event start date
     * @param rangeStart the beginning of the requested date range
     * @param rangeEnd the end of the requested date range
     * @param rule the recurrence rule
     * @return a chronologically ordered list of occurrence dates
     * @throws IllegalArgumentException if an argument is invalid
     */
    public static List<LocalDate> generateOccurrences(
            LocalDate startDate,
            LocalDate rangeStart,
            LocalDate rangeEnd,
            RecurrenceRule rule) {

        // Validate the supplied arguments.
        validateArguments(startDate, rangeStart, rangeEnd, rule);

        // Initialize the collection of occurrence dates.
        List<LocalDate> occurrences = new ArrayList<>();

        // Calculate the effective date boundaries.
        LocalDate effectiveStart =
                calculateEffectiveStartDate(startDate, rangeStart);

        LocalDate effectiveEnd =
                rule.getFrequency() == RecurrenceFrequency.NONE
                        ? rangeEnd
                        : calculateEffectiveEndDate(rangeEnd, rule);

        // Return an empty list if the date ranges do not intersect.
        if (effectiveStart.isAfter(effectiveEnd)) {
            return occurrences;
        }

        // Calculate the starting recurrence index to avoid
        // processing unnecessary repetitions of the pattern.
        long index = calculateStartIndex(
                startDate, effectiveStart, rule);

        // Determine whether the recurrence has an occurrence-count limit.
        boolean hasOccurrenceLimit =
                rule.getFrequency() != RecurrenceFrequency.NONE
                        && rule.getEndType()
                        == RecurrenceEndType.OCCURRENCE_COUNT;

        // Account for valid occurrences preceding the starting index.
        long occurrenceCount = hasOccurrenceLimit
                ? calculatePrecedingOccurrences(startDate, rule, index)
                : 0;

        // Return an empty list if the recurrence has already terminated.
        if (hasOccurrenceLimit
                && occurrenceCount >= rule.getOccurrenceCount()) {
            return occurrences;
        }

        while (true) {

            // Generate candidate dates for the current repetition
            // of the recurrence pattern.
            List<LocalDate> candidates =
                    calculateIntervalOccurrences(startDate, rule, index);

            // Stop if no additional candidate dates exist.
            if (candidates.isEmpty()) {
                break;
            }

            // Stop if the earliest candidate date exceeds
            // the effective end date.
            if (candidates.get(0).isAfter(effectiveEnd)) {
                break;
            }

            // Examine each candidate date individually.
            for (LocalDate candidate : candidates) {

                // Exclude dates preceding the original event start date.
                if (candidate.isBefore(startDate)) {
                    continue;
                }

                // Terminate generation when the candidate exceeds
                // the effective end date.
                if (candidate.isAfter(effectiveEnd)) {
                    return occurrences;
                }

                // Count valid occurrences, including those that
                // precede the requested date range.
                occurrenceCount++;

                // Include only dates within the effective date range.
                if (!candidate.isBefore(effectiveStart)) {
                    occurrences.add(candidate);
                }

                // Terminate generation when the maximum number
                // of permitted occurrences has been reached.
                if (hasOccurrenceLimit
                        && occurrenceCount >= rule.getOccurrenceCount()) {
                    return occurrences;
                }
            }

            // Advance to the next repetition of the recurrence pattern.
            index++;
        }

        return occurrences;
    }
}
