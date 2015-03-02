[![Build Status](https://travis-ci.org/Inbot/inbot-datemath.svg)](https://travis-ci.org/Inbot/inbot-datemath)

# Introduction

Inbot-datemath allows you to parse expressions such as "now - 1w" into an Instant timestamp. This is useful when querying data indexed with UTC normalized timestamps
from localized environments where it is easier to deal with relative time expressions.

This class was loosely inspired by the datemath functionality in elasticsearch that allows you to use simple date expressions when querying date fields. However, it is different in several ways:

 - It depends on java 8 java.time package so you don't need something like joda time.
 - It is not specific to elasticsearch. Their implementation is internal to elasticsearch and kind of tricky to untangle from it.
 - It is somewhat more flexible in interpreting different expressions.
 - It can optionally support timezones and interpret any expression in that timezone.
 - It has a now() function that you can statically influence with setCustomTime(String expression) and disableCustomTime(). This allows you to set an
 expression such as "now-1y" as the custom time and makes DateMath.now() return something that is always exactly 1 year in the past. This feature is intended for testing and a drop in replacement for using Instant.now()

# Install from maven cental

```xml
<dependency>
  <groupId>io.inbot</groupId>
  <artifactId>inbot-datemath</artifactId>
  <version>1.1</version>
</dependency>
```

# License

This code is [licensed](https://github.com/Inbot/inbot-datemath/blob/master/LICENSE) under the MIT license.

# Examples

Look at [DateMathTest](https://github.com/Inbot/inbot-datemath/blob/master/src/test/java/io/inbot/datemath/DateMathTest.java) for examples of expressions that are currently supported.

# Future work

We plan to support more complex and rich expressions over time. Pull requests welcome of course.

# Changelog

 - 1.1 - now() method that acts as a drop in replacement for Instant.now() that can be influenced with two static methods so you can globally set the time in your tests.
 - 1.0 - Initial release
