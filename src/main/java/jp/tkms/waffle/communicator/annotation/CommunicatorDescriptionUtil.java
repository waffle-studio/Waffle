package jp.tkms.waffle.communicator.annotation;

public class CommunicatorDescriptionUtil {
  public static String getDescription(Class type) {
    return ((CommunicatorDescription)type.getAnnotation(CommunicatorDescription.class)).value();
  }
}
