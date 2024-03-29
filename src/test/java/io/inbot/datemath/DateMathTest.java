package io.inbot.datemath;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.regex.Matcher;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test
public class DateMathTest {

    public void shouldFormatLocalDate() {
        String isoDate = DateMath.formatIsoDate(LocalDate.of(1974, 10, 20));
        assertThat(isoDate).isEqualTo("1974-10-20T00:00:00.000Z");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void shouldNotParseDateAfterY10K() {
        DateMath.parse("10000-01-01");
    }

    public void shouldHandleY2K38() {
        Instant instant = DateMath.AT_Y2K38.plus(10,ChronoUnit.DAYS);
        assertThat(DateMath.parse(DateMath.formatIsoDate(instant))).isEqualTo(instant);
    }

    public void shouldHandle0AD() {
        Instant instant = DateMath.AT_0AD;
        assertThat(DateMath.parse(DateMath.formatIsoDate(instant))).isEqualTo(instant);
    }

    public void shouldHandlePrehistoric() {
        Instant instant = DateMath.AT_0AD.minus(1000,ChronoUnit.DAYS);
        System.err.println(DateMath.formatIsoDate(instant));
        assertThat(DateMath.parse(DateMath.formatIsoDate(instant))).isEqualTo(instant);
    }

    public void shouldBeValid() {
        assertThat(DateMath.isValid("now-1m")).isTrue();
    }

    public void shouldBeInValid() {
        assertThat(DateMath.isValid("xxx")).isFalse();
    }

    public void shouldFormatSimpleTimestamp() {
        assertThat(DateMath.formatIsoDate(DateMath.parse("1974-10-20"))).isEqualTo("1974-10-20T00:00:00.000Z");
        assertThat(DateMath.formatIsoDateNoMs(DateMath.parse("1974-10-20"))).isEqualTo("1974-10-20T00:00:00Z");
        assertThat(DateMath.formatSimpleIsoTimestamp(DateMath.parse("1974-10-20"))).isEqualTo("19741020000000");
    }

    @DataProvider
    Object[][] yearMonthPatterns() {
        return new Object[][] {
            {"2014",true},
            {"2014-05",true},
            {"2014/02",true},
            {"201402",false},
            {"14-02",false}
        };
    }

    @Test(dataProvider="yearMonthPatterns")
    public void shouldMatchYearMonthPattern(String input, boolean expectMatch) {
        Matcher matcher = DateMath.YEAR_MONTH_PATTERN.matcher(input);
        assertThat(matcher.matches()).isEqualTo(expectMatch);
    }

    public void shouldParseYearMonthPattern() {
        assertThat(DateMath.formatIsoDate(DateMath.parse("2014-02"))).isEqualTo("2014-02-01T00:00:00.000Z");
        assertThat(DateMath.formatIsoDate(DateMath.parse("2014"))).isEqualTo("2014-01-01T00:00:00.000Z");
    }

    public void shouldFormatLocalDateTime() {
        LocalDateTime time = LocalDateTime.of(1984, 12, 1, 0, 0, 0);
        assertThat(DateMath.formatIsoDate(time)).isEqualTo("1984-12-01T00:00:00.000Z");
    }

    public void shouldParseIsoTimeStamp() {
        assertThat(DateMath.parse("1974-10-20T00:00:00Z")).isEqualTo(Instant.parse("1974-10-20T00:00:00Z").truncatedTo(ChronoUnit.SECONDS));
    }

    public void shouldSupportIsoDate() {
        assertThat(DateMath.parse("1974-10-20")).isEqualTo(Instant.parse("1974-10-20T00:00:00Z").truncatedTo(ChronoUnit.SECONDS));
    }

    @DataProvider
    public Object[][] samples() {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneId.of("Z"));
        return new Object[][] {
                {"tomorrow",now.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS).toInstant(ZoneOffset.UTC)},
                {"yesterday",now.truncatedTo(ChronoUnit.DAYS).minus(1, ChronoUnit.DAYS).toInstant(ZoneOffset.UTC)},
                {"Day_Before_Yesterday",now.truncatedTo(ChronoUnit.DAYS).minus(2, ChronoUnit.DAYS).toInstant(ZoneOffset.UTC)},
                {"day_after_tomorrow",now.truncatedTo(ChronoUnit.DAYS).plus(2, ChronoUnit.DAYS).toInstant(ZoneOffset.UTC)},
                {"day before yesterday",now.truncatedTo(ChronoUnit.DAYS).minus(2, ChronoUnit.DAYS).toInstant(ZoneOffset.UTC)},
                {"day after tomorrow",now.truncatedTo(ChronoUnit.DAYS).plus(2, ChronoUnit.DAYS).toInstant(ZoneOffset.UTC)},
                {"-10s",Instant.now().minus(10, ChronoUnit.SECONDS)},
                {"1d",Instant.now().plus(1, ChronoUnit.DAYS)},
                {"-1d",Instant.now().minus(1, ChronoUnit.DAYS)},
                {"-1w",Instant.now().minus(7, ChronoUnit.DAYS)},
                {"-100m",now.minusMonths(100).toInstant(ZoneOffset.UTC)},
                {"100m",now.plusMonths(100).toInstant(ZoneOffset.UTC)},
                {"  -  100  y  ",now.minusYears(100).toInstant(ZoneOffset.UTC)},
                {"100y",now.plusYears(100).toInstant(ZoneOffset.UTC)},
                {"10:00",LocalDate.now().atTime(LocalTime.of(10, 00)).toInstant(ZoneOffset.UTC)},
                {"10:00 -1d",LocalDate.now().atTime(LocalTime.of(10, 00)).toInstant(ZoneOffset.UTC).minus(1, ChronoUnit.DAYS)},
                {"10:00 +1d",LocalDate.now().atTime(LocalTime.of(10, 00)).toInstant(ZoneOffset.UTC).plus(1, ChronoUnit.DAYS)},
                {"2015-01-01",LocalDate.of(2015, 01, 01).atTime(LocalTime.MIDNIGHT).toInstant(ZoneOffset.UTC)},
                {"yesterday+100y",now.minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plusYears(100).toInstant(ZoneOffset.UTC)},
                {"yesterday - 100y",now.minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).minusYears(100).toInstant(ZoneOffset.UTC)},
                {"yesterday + 100y",now.minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).plusYears(100).toInstant(ZoneOffset.UTC)},
                {"yesterday\t-\t100y",now.minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).minusYears(100).toInstant(ZoneOffset.UTC)},
                {"min",LocalDateTime.of(0,1,1,0,0).toInstant(ZoneOffset.UTC)},
                {"max",LocalDateTime.of(9999,12,31,0,0).toInstant(ZoneOffset.UTC)}
        };
    }

    @Test(dataProvider="samples")
    public void shouldDoDateMath(String text, Instant expected) {
        // allow for a few milliseconds difference between now during parsing and calculation of expected
        assertThat(differenceInMillis(DateMath.parse(text), expected)).isLessThan(500);
    }

    public void shouldHandleTimeZoneCorrectly() {
        Instant ts = DateMath.parse("16:30","EST");
        assertThat(ts.toString()).contains("21:30");
    }

    public void shouldRenderMonthYear() {
        Instant ts = DateMath.parse("1974-10-20T00:00:00Z");
        String rendered = DateMath.renderMonthYear(ts, ZoneOffset.UTC, Locale.ENGLISH);
        assertThat(rendered).isEqualTo("October, 1974");
    }

    public void shouldRenderWeekYear() {
        Instant ts = DateMath.parse("1974-10-20T00:00:00Z");
        String rendered = DateMath.renderWeekYear(ts, ZoneOffset.UTC, Locale.ENGLISH);
        assertThat(rendered).isEqualTo("42, 1974");
    }

    private long differenceInMillis(Instant one, Instant two) {
        return Math.abs(one.toEpochMilli()-two.toEpochMilli());
    }

    public void shouldNotFuckUpDates() {
        Instant now = DateMath.now();
        String formatted = DateMath.formatIsoDate(now);
        Instant parsed = DateMath.parse(formatted);
        assertThat(parsed.toEpochMilli()).isEqualTo(now.toEpochMilli());
    }
}
