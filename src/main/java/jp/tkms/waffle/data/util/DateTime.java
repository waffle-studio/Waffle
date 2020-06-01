package jp.tkms.waffle.data.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTime {
  private ZonedDateTime dateTime;

  public DateTime() {
    dateTime = ZonedDateTime.now();
  }

  public DateTime(long epoch) {
    if (epoch < 0) {
      dateTime = null;
    } else {
      dateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault());
    }
  }

  public ZonedDateTime getDateTime() {
    return dateTime;
  }

  @Override
  public String toString() {
    if (dateTime == null) {
      return "-";
    }
    return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
  }
}
