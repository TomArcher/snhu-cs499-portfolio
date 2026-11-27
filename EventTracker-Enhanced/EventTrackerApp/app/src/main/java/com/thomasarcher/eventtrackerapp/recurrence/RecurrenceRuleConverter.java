package com.thomasarcher.eventtrackerapp.recurrence;

import androidx.room.TypeConverter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.EnumSet;

/**
 * Convert recurrence rules between their Java object
 * representation and JSON representation for Room
 * database persistence.
 */
public final class RecurrenceRuleConverter {

    /**
     * Convert a recurrence rule into a JSON string
     * for database persistence.
     *
     * @param rule Recurrence rule to serialize
     * @return JSON representation of the recurrence rule
     *
     * Time complexity: O(1)
     * Auxiliary space complexity: O(1)
     */
    @TypeConverter
    public static String fromRecurrenceRule(RecurrenceRule rule) {

        if (rule == null) {
            return null;
        }

        try {
            JSONObject json = new JSONObject();

            // Store the common recurrence properties.
            json.put("frequency", rule.getFrequency().name());
            json.put("interval", rule.getInterval());
            json.put("endType", rule.getEndType().name());

            // Store the selected weekly recurrence days.
            if (rule.getFrequency() == RecurrenceFrequency.WEEKLY) {
                JSONArray weeklyDays = new JSONArray();

                for (DayOfWeek day : rule.getWeeklyDays()) {
                    weeklyDays.put(day.name());
                }

                json.put("weeklyDays", weeklyDays);
            }

            // Store the monthly and yearly recurrence properties.
            if (rule.getFrequency() == RecurrenceFrequency.MONTHLY
                    || rule.getFrequency() == RecurrenceFrequency.YEARLY) {

                json.put("monthlyType", rule.getMonthlyType().name());

                switch (rule.getMonthlyType()) {
                    case DAY_OF_MONTH:
                        json.put("dayOfMonth", rule.getDayOfMonth());
                        break;

                    case RELATIVE_DAY:
                        json.put("relativePosition",
                                rule.getRelativePosition().name());

                        json.put("relativeDayType",
                                rule.getRelativeDayType().name());
                        break;
                }

                // Store the month for yearly recurrence rules.
                if (rule.getFrequency() == RecurrenceFrequency.YEARLY) {
                    json.put("yearlyMonth", rule.getYearlyMonth().name());
                }
            }

            // Store the recurrence termination properties.
            switch (rule.getEndType()) {
                case END_DATE:
                    json.put("endDate", rule.getEndDate().toString());
                    break;

                case OCCURRENCE_COUNT:
                    json.put("occurrenceCount", rule.getOccurrenceCount());
                    break;

                case NO_END:
                    break;
            }

            return json.toString();

        } catch (JSONException e) {
            throw new IllegalStateException(
                    "Unable to serialize recurrence rule.", e);
        }
    }

    /**
     * Reconstruct a recurrence pattern from its persisted
     * JSON properties without applying termination conditions.
     *
     * @param json JSON representation of the recurrence rule
     * @return Reconstructed recurrence pattern
     * @throws JSONException If required JSON properties are missing
     *         or cannot be retrieved
     *
     * Time complexity: O(1)
     * Auxiliary space complexity: O(1)
     */
    private static RecurrenceRule deserializeRecurrencePattern(
            JSONObject json) throws JSONException {

        // Retrieve the common recurrence properties.
        RecurrenceFrequency frequency =
                RecurrenceFrequency.valueOf(
                        json.getString("frequency"));

        int interval = json.getInt("interval");

        // Reconstruct the recurrence rule.
        switch (frequency) {
            case NONE:
                return RecurrenceRule.none();

            case DAILY:
                return RecurrenceRule.daily(interval);

            case WEEKLY:
                // Retrieve the persisted weekly recurrence days.
                JSONArray weeklyDays = json.getJSONArray("weeklyDays");

                // Reconstruct the collection of selected days.
                EnumSet<DayOfWeek> days =
                        EnumSet.noneOf(DayOfWeek.class);

                for (int i = 0; i < weeklyDays.length(); i++) {
                    days.add(DayOfWeek.valueOf(weeklyDays.getString(i)));
                }

                return RecurrenceRule.weekly(interval, days);

            case MONTHLY:
            case YEARLY:

                // Determine whether the recurrence is yearly.
                boolean isYearly =
                        frequency == RecurrenceFrequency.YEARLY;

                // Retrieve the recurrence pattern type.
                MonthlyRecurrenceType monthlyType =
                        MonthlyRecurrenceType.valueOf(
                                json.getString("monthlyType"));

                // Retrieve the month for yearly recurrence rules.
                Month month = isYearly
                        ? Month.valueOf(json.getString("yearlyMonth"))
                        : null;

                // Reconstruct the appropriate recurrence rule.
                switch (monthlyType) {
                    case DAY_OF_MONTH:
                        int dayOfMonth = json.getInt("dayOfMonth");

                        return isYearly
                                ? RecurrenceRule.yearlyByDay(
                                interval, month, dayOfMonth)
                                : RecurrenceRule.monthlyByDay(
                                interval, dayOfMonth);

                    case RELATIVE_DAY:
                        RelativePosition position =
                                RelativePosition.valueOf(
                                        json.getString("relativePosition"));

                        RelativeDayType dayType =
                                RelativeDayType.valueOf(
                                        json.getString("relativeDayType"));

                        return isYearly
                                ? RecurrenceRule.yearlyByRelativeDay(
                                interval, month, position, dayType)
                                : RecurrenceRule.monthlyByRelativeDay(
                                interval, position, dayType);

                    default:
                        throw new IllegalArgumentException(
                                "Unsupported monthly recurrence type.");
                }

            default:
                throw new IllegalArgumentException(
                        "Unsupported recurrence frequency.");
        }
    }

    /**
     * Reconstruct a recurrence rule from its persisted
     * JSON representation.
     *
     * @param value JSON representation of the recurrence rule
     * @return Reconstructed recurrence rule
     *
     * Time complexity: O(1)
     * Auxiliary space complexity: O(1)
     */
    @TypeConverter
    public static RecurrenceRule toRecurrenceRule(String value) {

        if (value == null) {
            return RecurrenceRule.none();
        }

        try {
            JSONObject json = new JSONObject(value);

            // Reconstruct the recurrence pattern.
            RecurrenceRule rule = deserializeRecurrencePattern(json);

            // Retrieve the recurrence termination type.
            RecurrenceEndType endType =
                    RecurrenceEndType.valueOf(
                            json.getString("endType"));

            // Apply the recurrence termination condition.
            switch (endType) {
                case END_DATE:
                    rule.endBy(
                            LocalDate.parse(json.getString("endDate")));
                    break;

                case OCCURRENCE_COUNT:
                    rule.endAfter(json.getInt("occurrenceCount"));
                    break;

                case NO_END:
                    rule.noEnd();
                    break;
            }

            return rule;

        } catch (JSONException e) {
            throw new IllegalStateException(
                    "Unable to deserialize recurrence rule.", e);
        }
    }
}
