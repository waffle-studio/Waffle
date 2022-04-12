package jp.tkms.waffle.communicator.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface CommunicatorDescription {
  String value();
}
