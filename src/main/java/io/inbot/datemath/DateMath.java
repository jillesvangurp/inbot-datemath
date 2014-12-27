package io.inbot.datemath;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to assist with parsing iso timestamps or expressions that manipulate such timestamps to a
 * java.time.Instant.
 */
public class DateMath {
    private static final Pattern DURATION_PATTERN = Pattern.compile("-?\\s*([0-9]+)\\s*([s|h|d|w|m|y])");
    private static final Pattern SUM_PATTERN = Pattern.compile("(.+)\\s*([\\+-])\\s*(.+)");

    public static String formatIsoDateNow() {
        return formatIsoDate(Instant.now());
    }

    public static String formatIsoDate(LocalDate date) {
        LocalDateTime time = LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), 0, 0, 0);
        return formatIsoDate(time.toInstant(ZoneOffset.UTC));
    }

    public static String formatIsoDate(LocalDateTime time) {
        return formatIsoDate(time.toInstant(ZoneOffset.UTC));
    }

    public static String formatIsoDate(OffsetDateTime date) {
        return formatIsoDate(date.toInstant());
    }

    public static String formatIsoDate(Instant date) {
        return date.truncatedTo(ChronoUnit.SECONDS).toString();
    }

    public static String formatIsoDate(long timeInMillisSinceEpoch) {
        return formatIsoDate(Instant.ofEpochMilli(timeInMillisSinceEpoch));
    }

    private static Instant flexibleInstantParse(String text) throws DateTimeParseException {
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException e) {
            // fallback to LocalDate and then pretend is at midnight
            LocalDate parsed = LocalDate.parse(text);
            return Instant.parse(parsed + "T00:00:00Z");
        }
    }

    public static Instant parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("cannot parse empty string");
        }
        text = text.trim();
        try {
            return flexibleInstantParse(text);
        } catch (DateTimeParseException e) {
            return toInstant(parseRelativeTime(text, null));
        }
    }

    private static LocalDateTime parseRelativeTime(String text, String zoneId) {
        if(zoneId == null) {
            zoneId="Z";
        }
        LocalDateTime now=LocalDateTime.ofInstant(Instant.now(), ZoneId.of(zoneId));
        switch (text) {
        case "now":
            return now;
        case "beginning_month":
            return now.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.firstDayOfMonth());
        case "end_month":
            return now.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.firstDayOfNextMonth());
        case "beginning_year":
            return now.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.firstDayOfYear());
        case "end_year":
            return now.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.firstDayOfNextYear());
        case "beginning_week":
            return now.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        case "end_week":
            return now.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        case "tomorrow":
            return now.truncatedTo(ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS);
        case "day_after_tomorrow":
            return now.truncatedTo(ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plus(2, ChronoUnit.DAYS);
        case "yesterday":
            return now.truncatedTo(ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).minus(1, ChronoUnit.DAYS);
        case "day_before_yesterday":
            return now.truncatedTo(ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).minus(2, ChronoUnit.DAYS);
        case "next_month":
            return parseRelativeTime("1m", zoneId);
        case "last_month":
            return parseRelativeTime("-1m",zoneId);
        case "next_year":
            return parseRelativeTime("1y",zoneId);
        case "last_year":
            return parseRelativeTime("-1y",zoneId);
        default:
            Matcher matcher = DURATION_PATTERN.matcher(text);
            if (matcher.matches()) {
                boolean minus = text.startsWith("-");
                int amount = Integer.valueOf(matcher.group(1));
                String unit = matcher.group(2);
                ChronoUnit chronoUnit;
                switch (unit) {
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
                    throw new IllegalArgumentException("illegal time unit. Should be [s|h|d|w|m|y]: " + text);
                }
                if (minus) {
                    return now.minus(amount, chronoUnit);
                } else {
                    return now.plus(amount, chronoUnit);
                }
            } else {
                matcher = SUM_PATTERN.matcher(text);
                if(matcher.matches()) {
                    String left = matcher.group(1);
                    String operator = matcher.group(2);
                    String right = matcher.group(3);
                    Instant offset = parse(left);
                    boolean minus = operator.equals("-");
                    Matcher dm = DURATION_PATTERN.matcher(right);
                    if(dm.matches()) {
                        int amount = Integer.valueOf(dm.group(1));
                        String unit = dm.group(2);
                        ChronoUnit chronoUnit;
                        switch (unit) {
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
                                return LocalDateTime.ofInstant(offset, ZoneId.of(zoneId)).minusMonths(amount);
                            } else {
                                return LocalDateTime.ofInstant(offset, ZoneId.of(zoneId)).plusMonths(amount);
                            }
                        case "y":
                            if (minus) {
                                return LocalDateTime.ofInstant(offset, ZoneId.of(zoneId)).minusYears(amount);
                            } else {
                                return LocalDateTime.ofInstant(offset, ZoneId.of(zoneId)).plusYears(amount);
                            }
                        default:
                            throw new IllegalArgumentException("illegal time unit. Should be [s|h|d|w|m|y]: " + text);
                        }
                        if (minus) {
                            return LocalDateTime.ofInstant(offset, ZoneId.of(zoneId)).minus(amount, chronoUnit);
                        } else {
                            return LocalDateTime.ofInstant(offset, ZoneId.of(zoneId)).plus(amount, chronoUnit);
                        }
                    } else {
                        throw new IllegalArgumentException("illegal duration. Should match ([0-9]+)([s|h|d|w|m|y]): " + right);
                    }
                }
            }
        }
        throw new IllegalArgumentException("illegal timestamp " + text);

    }

    public static Instant toInstant(LocalDate date) {
        return Instant.parse(date.toString() + "T00:00:00Z");
    }

    public static Instant toInstant(LocalDateTime dateTime) {
        return dateTime.toInstant(ZoneOffset.UTC);
    }

}
