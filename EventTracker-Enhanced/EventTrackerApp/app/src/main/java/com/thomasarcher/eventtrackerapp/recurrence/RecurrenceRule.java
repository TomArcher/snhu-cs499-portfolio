package com.thomasarcher.eventtrackerapp.recurrence;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.time.Month;
import java.time.LocalDate;

/**
 * Represents and validates the recurrence pattern and termination
 * conditions associated with an event.
 *
 * Supports daily, weekly, monthly, and yearly recurrence patterns,
 * as well as nonrecurring events.
 *
 * Uses static factory methods to construct valid recurrence rules
 * and fluent instance methods to configure termination conditions.
 *
 * Stores recurrence rules rather than individual occurrence dates.
 * Delegate occurrence generation to RecurrenceCalculator.
 */
public class RecurrenceRule {
    // Rrecurrence frequency.
    private RecurrenceFrequency frequency;
    public RecurrenceFrequency getFrequency() { return frequency; }

    // Spacing between repetitions of the recurrence pattern.
    private int interval;
    public int getInterval() { return interval; }

    // Distinguish fixed-date and relative-day recurrence patterns.
    // Apply to monthly and yearly recurrence rules.
    private MonthlyRecurrenceType monthlyType;
    public MonthlyRecurrenceType getMonthlyType() { return monthlyType; }

    // Requested day of the month for fixed-date patterns.
    private int dayOfMonth;
    public int getDayOfMonth() { return dayOfMonth; }

    // Ordinal position for relative-day patterns.
    private RelativePosition relativePosition;
    public RelativePosition getRelativePosition() { return relativePosition; }

    // Type of day to match for relative-day patterns.
    private RelativeDayType relativeDayType;
    public RelativeDayType getRelativeDayType() { return relativeDayType; }

    // Selected weekdays for weekly recurrence.
    private EnumSet<DayOfWeek> weeklyDays = EnumSet.noneOf(DayOfWeek.class);
    /**
     * Return a defensive copy of the selected weekdays to prevent
     * callers from modifying the internal recurrence rule.
     *
     * @return a copy of the selected weekdays
     */
    public EnumSet<DayOfWeek> getWeeklyDays() {
        return EnumSet.copyOf(weeklyDays);
    }

    // Month for yearly recurrence.
    private Month yearlyMonth;
    public Month getYearlyMonth() { return yearlyMonth; }

    // How the recurrence series terminates.
    private RecurrenceEndType endType;
    public RecurrenceEndType getEndType() { return endType; }

    // Inclusive termination date for END_DATE rules.
    private LocalDate endDate;
    public LocalDate getEndDate() { return endDate; }

    // Maximum number of occurrences for OCCURRENCE_COUNT rules.
    private int occurrenceCount;
    public int getOccurrenceCount() { return  occurrenceCount; }

    /**
     * Initialize the common properties of a recurrence rule.
     *
     * Restrict direct instantiation to ensure callers construct
     * recurrence rules through the appropriate factory methods.
     *
     * Default to a recurrence series with no termination condition.
     *
     * @param frequency the recurrence frequency
     * @param interval the spacing between repetitions
     * @throws IllegalArgumentException if the frequency is null
     *         or the interval is less than one
     */
    private RecurrenceRule(RecurrenceFrequency frequency, int interval) {
        if (frequency == null) {
            throw new IllegalArgumentException("Frequency cannot be null.");
        }

        if (interval < 1) {
            throw new IllegalArgumentException("Interval must be at least 1.");
        }

        this.frequency = frequency;
        this.interval = interval;
        this.endType = RecurrenceEndType.NO_END;
    }

    /**
     * Create a daily recurrence rule.
     *
     * @param interval the number of days between occurrences
     * @return the configured recurrence rule
     * @throws IllegalArgumentException if the interval is less than one
     */
    public static RecurrenceRule daily(int interval) {
        return new RecurrenceRule(RecurrenceFrequency.DAILY, interval);
    }

    /**
     * Create a weekly recurrence rule for one or more selected weekdays.
     *
     * @param interval the number of weeks between repetitions
     * @param days the weekdays on which occurrences are scheduled
     * @return the configured recurrence rule
     * @throws IllegalArgumentException if the interval is less than one
     *         or no weekdays are specified
     */
    public static RecurrenceRule weekly(int interval, EnumSet<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            throw new IllegalArgumentException("Specify at least one day of the week.");
        }

        RecurrenceRule rule = new RecurrenceRule(RecurrenceFrequency.WEEKLY, interval);

        // Store a defensive copy to prevent external modification.
        rule.weeklyDays = EnumSet.copyOf(days);

        return rule;
    }

    /**
     * Create a monthly recurrence rule based on a fixed day of the month.
     *
     * Preserve the requested day so the occurrence calculator can
     * accommodate months with different lengths without introducing
     * date drift.
     *
     * @param interval the number of months between repetitions
     * @param dayOfMonth the requested day of the month (1 through 31)
     * @return the configured recurrence rule
     * @throws IllegalArgumentException if the interval or day is invalid
     */
    public static RecurrenceRule monthlyByDay(int interval, int dayOfMonth) {
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new IllegalArgumentException("Day of month must be between 1 and 31.");
        }

        RecurrenceRule rule = new RecurrenceRule(
                RecurrenceFrequency.MONTHLY,
                interval);
        rule.monthlyType = MonthlyRecurrenceType.DAY_OF_MONTH;
        rule.dayOfMonth = dayOfMonth;

        return rule;
    }

    /**
     * Create a monthly recurrence rule based on a relative day pattern,
     * such as the second Tuesday or last weekday of the month.
     *
     * @param interval the number of months between repetitions
     * @param position the ordinal position of the matching day
     * @param dayType the type of day to match
     * @return the configured recurrence rule
     * @throws IllegalArgumentException if the interval is invalid
     *         or either relative-day argument is null
     */
    public static RecurrenceRule monthlyByRelativeDay(
            int interval,
            RelativePosition position,
            RelativeDayType dayType) {

        if (position == null || dayType == null) {
            throw new IllegalArgumentException("Both position and day type are required.");
        }

        RecurrenceRule rule = new RecurrenceRule(RecurrenceFrequency.MONTHLY, interval);
        rule.monthlyType = MonthlyRecurrenceType.RELATIVE_DAY;
        rule.relativePosition = position;
        rule.relativeDayType = dayType;

        return rule;
    }

    /**
     * Create a yearly recurrence rule based on a specified month
     * and day of the month.
     *
     * Permit February 29 so the occurrence calculator can accommodate
     * both leap years and non-leap years.
     *
     * @param interval the number of years between repetitions
     * @param month the month in which the occurrence is scheduled
     * @param dayOfMonth the requested day of the month
     * @return the configured recurrence rule
     * @throws IllegalArgumentException if the interval, month,
     *         or day is invalid
     */
    public static RecurrenceRule yearlyByDay(
            int interval,
            Month month,
            int dayOfMonth) {

        if (month == null) {
            throw new IllegalArgumentException("Month is required.");
        }

        // Use the maximum possible month length to accommodate
        // February 29 in leap years.
        if (dayOfMonth < 1 || dayOfMonth > month.maxLength()) {
            throw new IllegalArgumentException("Day of month not valid for specified month.");
        }

        RecurrenceRule rule = new RecurrenceRule(
                RecurrenceFrequency.YEARLY,
                interval);
        rule.yearlyMonth = month;
        rule.monthlyType = MonthlyRecurrenceType.DAY_OF_MONTH;
        rule.dayOfMonth = dayOfMonth;

        return rule;
    }

    /**
     * Create a yearly recurrence rule based on a relative day pattern
     * within a specified month, such as the fourth Thursday of November.
     *
     * @param interval the number of years between repetitions
     * @param month the month in which the occurrence is scheduled
     * @param position the ordinal position of the matching day
     * @param dayType the type of day to match
     * @return the configured recurrence rule
     * @throws IllegalArgumentException if the interval is invalid
     *         or any required argument is null
     */
    public static RecurrenceRule yearlyByRelativeDay(
            int interval,
            Month month,
            RelativePosition position,
            RelativeDayType dayType) {

        if (month == null || position == null || dayType == null) {
            throw new IllegalArgumentException("Month, position, and day type are required.");
        }

        RecurrenceRule rule = new RecurrenceRule(
                RecurrenceFrequency.YEARLY,
                interval);
        rule.yearlyMonth = month;
        rule.monthlyType = MonthlyRecurrenceType.RELATIVE_DAY;
        rule.relativePosition = position;
        rule.relativeDayType = dayType;

        return rule;
    }

    /**
     * Create a rule representing a nonrecurring event.
     *
     * @return a recurrence rule with a frequency of NONE
     */
    public static RecurrenceRule none() {
        return new RecurrenceRule(
                RecurrenceFrequency.NONE,
                1);
    }

    /*
     * Support method chaining through the endBy(), endAfter(),
     * and noEnd() instance methods.
     *
     * Construct a recurrence rule using a static factory method
     * and configure its termination condition in a single expression.
     *
     * Replace any previously configured termination condition
     * when invoking one of these methods.
     *
     * For example, create a rule that repeats on the 15th of every
     * month through December 31, 2027:
     *
     * RecurrenceRule rule = RecurrenceRule
     *         .monthlyByDay(1, 15)
     *         .endBy(LocalDate.of(2027, 12, 31));
     */

    /**
     * Configure the recurrence series to terminate on an inclusive date.
     *
     * Clear any previously configured occurrence-count limit.
     *
     * @param endDate the last permissible occurrence date
     * @return the current recurrence rule for method chaining
     * @throws IllegalArgumentException if the end date is null
     */
    public RecurrenceRule endBy(LocalDate endDate) {
        if (endDate == null) {
            throw new IllegalArgumentException("End date is required.");
        }

        this.endType = RecurrenceEndType.END_DATE;
        this.endDate = endDate;
        this.occurrenceCount = 0;

        return this;
    }

    /**
     * Configure the recurrence series to terminate after a specified
     * number of valid occurrences.
     *
     * Clear any previously configured termination date.
     *
     * @param occurrenceCount the maximum number of occurrences
     * @return the current recurrence rule for method chaining
     * @throws IllegalArgumentException if the occurrence count
     *         is less than one
     */
    public RecurrenceRule endAfter(int occurrenceCount) {
        if (occurrenceCount < 1) {
            throw new IllegalArgumentException("Occurrence count must be at least 1.");
        }

        this.endType = RecurrenceEndType.OCCURRENCE_COUNT;
        this.endDate = null;
        this.occurrenceCount = occurrenceCount;

        return this;
    }

    /**
     * Remove any previously configured termination condition.
     *
     * Allow the recurrence series to continue indefinitely,
     * subject to the requested date range during occurrence generation.
     *
     * @return the current recurrence rule for method chaining
     */
    public RecurrenceRule noEnd() {
        this.endType = RecurrenceEndType.NO_END;
        this.endDate = null;
        this.occurrenceCount = 0;

        return this;
    }
}
