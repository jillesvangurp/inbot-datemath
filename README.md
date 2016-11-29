# Introduction

Inbot-datemath allows you to parse expressions such as "now - 1w" into an Instant timestamp. This is useful when querying data indexed with UTC normalized timestamps from localized environments where it is easier to deal with relative time expressions.

This class was loosely inspired by the datemath functionality in elasticsearch that allows you to use simple date expressions when querying date fields. However, it is different in several ways:

 - It depends on java 8 java.time package so you don't need something like joda time.
 - It is not specific to elasticsearch. Their implementation is internal to elasticsearch and kind of tricky to untangle from it.
 - It is somewhat more flexible in interpreting different expressions.
 - It can optionally support timezones and interpret any expression in that timezone.
 - It comes with a few handy constants for tricky dates that are useful for testing as well as coming up with more sane MIN and MAX than Java's Instant provides (hint you'll have trouble parsing and formatting those).

# Install from maven cental

[![Build Status](https://travis-ci.org/Inbot/inbot-datemath.svg)](https://travis-ci.org/Inbot/inbot-datemath)

```xml
<dependency>
  <groupId>io.inbot</groupId>
  <artifactId>inbot-datemath</artifactId>
  <version>1.14</version>
</dependency>
```

# License

This code is [licensed](https://github.com/Inbot/inbot-datemath/blob/master/LICENSE) under the MIT license.

# Examples

Look at [DateMathTest](https://github.com/Inbot/inbot-datemath/blob/master/src/test/java/io/inbot/datemath/DateMathTest.java) for examples of expressions that are currently supported.

# Future work

We plan to support more complex and rich expressions over time. Pull requests welcome of course.

# Changelog
 - 1.14
   - Add constants for various sigificant instants: AT_EPOCH, AT_0AD, AT_Y2K, AT_Y2K38 (epochocalypse), AT_Y10K (after this 5 digits for year with some wiggle room to adjust into timezones without running over the limit)
   - Tweak meaning of min/max.
 - 1.13
   - Fix issue with isValid
 - 1.12
   - add parser support for partial yyyy-mm and yyyy timestamps
 - 1.11
   - add validate method so that users can easily validate date expressions
 - 1.9
   - add iso timestamp format method without ms precision
 - 1.8
   - Explicitly render iso timestamps in UTC
   - Add simple formatter tha supports YYYYMMDDHHMMSS for use in things like filenames
 - 1.7 - change max date year to 9999 to avoid the + sign that Java's formatter adds before the year for the year 10000 and beyond
 - 1.6 - add convenience methods for turning instants into month + year or weeknr + year
 - 1.5
   - always format instants with 3 fractionals and don't rely on the annoying defaults for ISO_INSTANT
   - strip out the custom precision stuff everywhere
   - get rid of the unused and somewhat wonky way of globally setting the time
   - use ENGLISH locale on String.toLowerCase instead of relying on platform dependent defaults
 - 1.4 - Reimplement min and max to not depend on Instant.MIN or MAX since those dates are so far away that you get all sort of weird long overflows and other exceptions. Now returns LocalDateTime.of(0,1,1,0,0).toInstant(zoneId) or LocalDateTime.of(10000,1,1,0,0).toInstant(zoneId). ZoneId defaults to UTC unless you use a custom zoneId on the parse method.
 - 1.3 - support output with ms precision. Allows for a generic global precision to truncate dates.
 - 1.2 - support "min", "max", "distant past", and "distant future" expressions using Instant.MIN and Instant.MAX
 - 1.1 - now() method that acts as a drop in replacement for Instant.now() that can be influenced with two static methods so you can globally set the time in your tests.
 - 1.0 - Initial release
