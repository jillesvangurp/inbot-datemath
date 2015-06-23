package io.inbot.datemath;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test
public class DateMathTest {

    public void shouldSupportMilliSecondPrecision() {
        LocalDateTime time = LocalDateTime.of(1984, 12, 1, 0, 0, 0, 123000000);
        assertThat(DateMath.formatIsoDate(time,
                        DateMath.PRECISION.ms)).isEqualTo("1984-12-01T00:00:00.123Z");

    }

    public void shouldRemoveMillisecondsIfPrecisionIsSeconds() {
        LocalDateTime time = LocalDateTime.of(1984, 12, 1, 0, 0, 0, 123000000);
	assertThat(DateMath.formatIsoDate(time,
				DateMath.PRECISION.s)).isEqualTo("1984-12-01T00:00:00Z");
    }

    public void shouldAllowSettingDefaultPrecision() {
        DateMath.setDefaultPrecision(DateMath.PRECISION.d);
        LocalDateTime time = LocalDateTime.of(1984, 12, 1, 4, 0, 0, 123000000);
        assertThat(DateMath.formatIsoDate(time
                                         )).isEqualTo("1984-12-01T00:00:00Z");
    }

    public void shouldFormatLocalDate() {
        assertThat(DateMath.formatIsoDate(LocalDate.of(1974, 10, 20))).isEqualTo("1974-10-20T00:00:00Z");
    }

    public void shouldFormatLocalDateTime() {
        LocalDateTime time = LocalDateTime.of(1984, 12, 1, 0, 0, 0);
        assertThat(DateMath.formatIsoDate(time)).isEqualTo("1984-12-01T00:00:00Z");
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
                {"max",LocalDateTime.of(10000,1,1,0,0).toInstant(ZoneOffset.UTC)}
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

    private long differenceInMillis(Instant one, Instant two) {
        return Math.abs(one.toEpochMilli()-two.toEpochMilli());
    }

    public void shouldChangeNow() {
        Instant now = DateMath.now();
        DateMath.setCustomTime("now-1y");
        Instant lastYear = DateMath.now();
        assertThat(lastYear.isBefore(now));
        DateMath.disableCustomTime();
        assertThat(DateMath.now().isAfter(now));
    }
}
