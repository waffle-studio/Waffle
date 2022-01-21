package jp.tkms.util;

import java.util.function.Function;

@FunctionalInterface
public interface Editor<T> extends Function<T, T> {
}
