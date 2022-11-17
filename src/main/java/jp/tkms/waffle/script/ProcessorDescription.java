package jp.tkms.waffle.script;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface ProcessorDescription {
  String value();
}
