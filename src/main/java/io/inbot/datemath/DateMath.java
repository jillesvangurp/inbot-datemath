package io.inbot.datemath;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
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

    /**
     * 1st of January 1970 at 00:00 UTC
     * if you are going to use a min date, this is a safe default unless you want to deal with negative epoch millis/seconds
     */
    public static final Instant AT_EPOCH=Instant.ofEpochMilli(0);

    /**
     * 1st of January, 0 AD at 00:00 UTC
     */
    public static final Instant AT_0AD=toInstant(LocalDateTime.of(0, 1, 1, 0, 0));
    /**
     * 1st of January, 2000 at 00:00 UTC
     */
    public static final Instant AT_Y2K=toInstant(LocalDateTime.of(2000, 1, 1, 0, 0));
    /**
     * The latest time that can be represented in Unix's signed 32-bit integer time format is 03:14:07 UTC on Tuesday, 19 January 2038 (2,147,483,647 seconds after 1 January 1970)
     *
     * https://en.wikipedia.org/wiki/Year_2038_problem
     */
    public static final Instant AT_Y2K38=Instant.ofEpochSecond(2_147_483_647);
    // anything after that will have > 4 digits for year; not likely to end well with date formatting
    /**
     * 31st of December, 9999 at 00:00 UTC, gives you some wiggle room to adjust to later timezones without getting 5 digits in the year.
     */
    public static final Instant AT_Y10K=toInstant(LocalDateTime.of(9999, 12, 31, 0, 0));


    private static final Pattern DURATION_PATTERN = Pattern.compile("-?\\s*([0-9]+)\\s*([ms|s|h|d|w|m|y])");
    private static final Pattern SUM_PATTERN = Pattern.compile("(.+)\\s*([\\+-])\\s*(.+)");
    static final Pattern YEAR_MONTH_PATTERN = Pattern.compile("([0-9][0-9][0-9][0-9])([^0-9]([0-9][0-9]))?");

    /**
     * Variant of DateTimeFormatter.ISO_INSTANT that always adds 3 fractionals for the milliseconds instead of 0, 3, 6, or 9.
     */
    private static final DateTimeFormatter CONSISTENT_ISO_INSTANT=new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendInstant(3)
            .toFormatter();

    private static final DateTimeFormatter CONSISTENT_ISO_INSTANT_NOMS=new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendInstant(0)
            .toFormatter();


    private static final DateTimeFormatter SIMPLE_ISO_INSTANT=new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
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

    public static String formatSimpleIsoTimestamp(Instant instant) {
        return SIMPLE_ISO_INSTANT.format(instant.atZone(ZoneOffset.UTC));
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
        return CONSISTENT_ISO_INSTANT.format(date.atZone(ZoneOffset.UTC));
    }

    public static String formatIsoDateNoMs(Instant date) {
        return CONSISTENT_ISO_INSTANT_NOMS.format(date.atZone(ZoneOffset.UTC));
    }

    public static String formatIsoDate(long timeInMillisSinceEpoch) {
        return formatIsoDate(Instant.ofEpochMilli(timeInMillisSinceEpoch));
    }

    private static final ZoneId UTC = ZoneId.of("Z");
    private static Instant flexibleInstantParse(String text, ZoneId zoneId) throws DateTimeParseException {
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException e) {

            if(zoneId == null) {
                zoneId=UTC;
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

    public static boolean isValid(String text) {
        try {
            parse(text);
            return true;
        } catch (RuntimeException e) {
            return false;
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

    private static Instant parseYearMonth(String text, ZoneId zoneId) {
        Matcher matcher = YEAR_MONTH_PATTERN.matcher(text);
        if(matcher.matches()) {
            String year = matcher.group(1);
            String month = matcher.group(3);
            int yearInt = Integer.valueOf(year);
            int monthInt = 1;
            if(month!=null) {
                monthInt=Integer.valueOf(month);
            }
            int day=1;

            LocalDate localDate = LocalDate.of(yearInt, monthInt, day);
            LocalDateTime localDateTime = LocalDateTime.of(localDate, LocalTime.MIDNIGHT);
            return localDateTime.toInstant(ZoneOffset.of(zoneId.getId()));
        }

        return null;
    }

    private static Instant parse(String text, ZoneId zone) {
        if (text == null) {
            throw new IllegalArgumentException("cannot parse empty string");
        }
        text = text.trim();

        try {
            return flexibleInstantParse(text, zone);
        } catch (DateTimeParseException e) {
            Instant ym = parseYearMonth(text,zone);
            if(ym!=null) {
                return ym;
            } else {
                return toInstant(parseRelativeTime(text, zone));
            }
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
            return LocalDateTime.of(9999, 12, 31, 0, 0);
        case "distant past":
            return LocalDateTime.ofInstant(Instant.EPOCH, zoneId);
        case "distant future":
            return LocalDateTime.of(9999, 12, 31, 0, 0);
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

    public static String renderWeekYear(Instant t, ZoneId zoneId, Locale locale) {
        LocalDateTime ld = LocalDateTime.ofInstant(t, zoneId);
        int week = ld.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
        int year = ld.get(ChronoField.YEAR);
        return week + ", " + year;
    }

    public static String renderMonthYear(Instant t, ZoneId zoneId, Locale locale) {
        LocalDateTime ld = LocalDateTime.ofInstant(t, zoneId);
        int month = ld.get(ChronoField.MONTH_OF_YEAR);
        int year = ld.get(ChronoField.YEAR);
        return Month.of(month).getDisplayName(TextStyle.FULL, locale) + ", " + year;
    }
}
