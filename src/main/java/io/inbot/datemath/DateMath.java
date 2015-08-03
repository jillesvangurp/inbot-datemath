package io.inbot.datemath;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to assist with parsing iso timestamps or expressions that manipulate such timestamps to a
 * java.time.Instant.
 */
public class DateMath {
    private static final Pattern DURATION_PATTERN = Pattern.compile("-?\\s*([0-9]+)\\s*([ms|s|h|d|w|m|y])");
    private static final Pattern SUM_PATTERN = Pattern.compile("(.+)\\s*([\\+-])\\s*(.+)");

    /**
     * Variant of DateTimeFormatter.ISO_INSTANT that always adds 3 fractionals for the milliseconds instead of 0, 3, 6, or 9.
     */
    private static final DateTimeFormatter CONSISTENT_ISO_INSTANT=new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendInstant(3)
            .toFormatter();

    /**
     * @return now or the parsed Instant for whatever custom expression is configured
     */
    public static Instant now() {
        return Instant.now();
    }

    public static String formatIsoDate(OffsetDateTime date) {
        return formatIsoDate(date.toInstant());
    }

    public static String formatIsoDateNow() {
        return formatIsoDate(now());
    }

    public static String formatIsoDate(LocalDate date) {
        LocalDateTime time = LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), 0, 0, 0);
        return formatIsoDate(time.toInstant(ZoneOffset.UTC));
    }

    public static String formatIsoDate(LocalDateTime time) {
        return formatIsoDate(time.toInstant(ZoneOffset.UTC));
    }

    /**
     * Use this instead of Instant.toString() to ensure you always end up with the same pattern instead of 'smartly'
     * loosing fractionals depending on what time it is.
     *
     * @param date
     *            an instant
     * @return iso instant of the form "1974-10-20T00:00:00.000Z" with 3 fractionals, always.
     */
    public static String formatIsoDate(Instant date) {
        return CONSISTENT_ISO_INSTANT.format(date);
    }

    public static String formatIsoDate(long timeInMillisSinceEpoch) {
        return formatIsoDate(Instant.ofEpochMilli(timeInMillisSinceEpoch));
    }

    private static Instant flexibleInstantParse(String text, ZoneId zoneId) throws DateTimeParseException {
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException e) {

            if(zoneId == null) {
                zoneId=ZoneId.of("Z");
            }
            try {
                // try LocalDate
                LocalDate localDate = LocalDate.parse(text);
                LocalDateTime localDateTime = LocalDateTime.of(localDate, LocalTime.MIDNIGHT);
                return localDateTime.toInstant(ZoneOffset.of(zoneId.getId()));
            } catch (DateTimeParseException e1) {
                // try LocalTime
                LocalTime localTime = LocalTime.parse(text);
                LocalDate today = LocalDate.now(zoneId);
                LocalDateTime localDateTime = LocalDateTime.of(today, localTime);
                return localDateTime.toInstant(ZoneOffset.of(zoneId.getId()));
            }
        }
    }

    /**
     * @param text
     * @return Instant; any relative expressions are interpreted to be in the UTC timezone.
     */
    public static Instant parse(String text) {
        return parse(text, ZoneOffset.UTC);
    }

    public static Instant parse(String text, String zoneId) {
        ZoneId zone = ZoneId.of(zoneId, ZoneId.SHORT_IDS);
        return parse(text, zone);
    }

    private static Instant parse(String text, ZoneId zone) {
        if (text == null) {
            throw new IllegalArgumentException("cannot parse empty string");
        }
        text = text.trim();

        try {
            return flexibleInstantParse(text, zone);
        } catch (DateTimeParseException e) {
            return toInstant(parseRelativeTime(text, zone));
        }
    }

    private static LocalDateTime parseRelativeTime(String text, ZoneId zoneId) {
        if(zoneId == null) {
            zoneId=ZoneOffset.UTC;
        }
        LocalDateTime now=LocalDateTime.ofInstant(Instant.now(), zoneId);
        switch (text.replace('_', ' ').toLowerCase(Locale.ENGLISH)) {
        case "min":
            return LocalDateTime.of(0, 01, 01, 0, 0);
        case "max":
            return LocalDateTime.of(10000, 01, 01, 0, 0);
        case "distant past":
            return LocalDateTime.ofInstant(Instant.MIN, zoneId);
        case "distant future":
            return LocalDateTime.ofInstant(Instant.MAX, zoneId);
        case "morning":
            return parseRelativeTime("09:00", zoneId);
        case "midnight":
            return parseRelativeTime("00:00", zoneId);
        case "noon":
            return parseRelativeTime("12:00", zoneId);
        case "now":
            return now;
        case "beginning month":
            return now.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.firstDayOfMonth());
        case "end month":
            return now.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.firstDayOfNextMonth());
        case "beginning year":
            return now.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.firstDayOfYear());
        case "end year":
            return now.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.firstDayOfNextYear());
        case "beginning week":
            return now.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        case "end week":
            return now.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        case "tomorrow":
            return now.truncatedTo(ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS);
        case "day after tomorrow":
            return now.truncatedTo(ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(2, ChronoUnit.DAYS);
        case "yesterday":
            return now.truncatedTo(ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).minus(1, ChronoUnit.DAYS);
        case "day before yesterday":
            return now.truncatedTo(ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).minus(2, ChronoUnit.DAYS);
        case "next month":
            return parseRelativeTime("1m", zoneId);
        case "last month":
            return parseRelativeTime("-1m",zoneId);
        case "next year":
            return parseRelativeTime("1y",zoneId);
        case "last year":
            return parseRelativeTime("-1y",zoneId);
        default:
            Matcher durationMatcher = DURATION_PATTERN.matcher(text);
            if (durationMatcher.matches()) {
                // relative to now
                boolean minus = text.startsWith("-");
                int amount = Integer.valueOf(durationMatcher.group(1));
                String unit = durationMatcher.group(2);
                return adjust(now, minus, amount, unit);
            } else {

                Matcher sumMatcher = SUM_PATTERN.matcher(text);
                if(sumMatcher.matches()) {
                    String left = sumMatcher.group(1);
                    String operator = sumMatcher.group(2);
                    String right = sumMatcher.group(3);
                    Instant offset = parse(left);
                    boolean minus = operator.equals("-");
                    Matcher rightHandSideMatcher = DURATION_PATTERN.matcher(right);
                    if(rightHandSideMatcher.matches()) {
                        int amount = Integer.valueOf(rightHandSideMatcher.group(1));
                        String unit = rightHandSideMatcher.group(2);

                        return adjust( LocalDateTime.ofInstant(offset, zoneId), minus, amount, unit);
                    } else {
                        throw new IllegalArgumentException("illegal duration. Should match ([0-9]+)([s|h|d|w|m|y]): " + right);
                    }
                }
            }
        }
        throw new IllegalArgumentException("illegal time expression " + text);

    }

    private static LocalDateTime adjust(LocalDateTime now, boolean minus, int amount, String unit) {
        ChronoUnit chronoUnit;
        switch (unit) {
        case "ms":
            chronoUnit = ChronoUnit.MILLIS;
            break;
        case "s":
            chronoUnit = ChronoUnit.SECONDS;
            break;
        case "h":
            chronoUnit = ChronoUnit.HOURS;
            break;
        case "d":
            chronoUnit = ChronoUnit.DAYS;
            break;
        case "w":
            chronoUnit = ChronoUnit.DAYS;
            amount = amount * 7;
            break;
        case "m":
            if (minus) {
                return now.minusMonths(amount);
            } else {
                return now.plusMonths(amount);
            }
        case "y":
            if (minus) {
                return now.minusYears(amount);
            } else {
                return now.plusYears(amount);
            }
        default:
            throw new IllegalArgumentException("illegal time unit. Should be [ms|s|h|d|w|m|y]: ");
        }
        if (minus) {
            return now.minus(amount, chronoUnit);
        } else {
            return now.plus(amount, chronoUnit);
        }
    }

    public static Instant toInstant(LocalDate date) {
        return toInstant(LocalDateTime.of(date, LocalTime.MIDNIGHT));
    }

    public static Instant toInstant(LocalDateTime dateTime) {
        return dateTime.toInstant(ZoneOffset.UTC);
    }
}
