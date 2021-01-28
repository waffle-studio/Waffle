package jp.tkms.waffle.data.util;

import jp.tkms.waffle.Main;
import java.util.Objects;

public class WaffleId {
  private static long currentTime = getCurrentTime();
  private static long serialNumber = 0;
  private long id;

  public WaffleId(long id) {
    this.id = id;
  }

  public WaffleId(String reversedHexCode) {
    this.id = Long.decode("0x" + new StringBuffer(reversedHexCode).reverse().toString());
  }

  public WaffleId() {
    long time = getCurrentTime();
    if (time != currentTime) {
      serialNumber = 0;
    }
    currentTime = time;
    this.id = (time * 100000) + serialNumber;
  }

  public long getId() {
    return id;
  }

  public String getReversedHexCode() {
    return new StringBuffer(Long.toHexString(id)).reverse().toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    WaffleId waffleId = (WaffleId) o;
    return id == waffleId.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  private static long getCurrentTime() {
    return Long.valueOf(Main.DATE_FORMAT_FOR_WAFFLE_ID.format(System.currentTimeMillis()));
  }

  public static WaffleId newId() {
    return new WaffleId();
  }
}
