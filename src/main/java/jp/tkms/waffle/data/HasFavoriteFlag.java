package jp.tkms.waffle.data;

import jp.tkms.waffle.Constants;

import java.io.IOException;
import java.nio.file.Files;

public interface HasFavoriteFlag extends DataDirectory {
  default void isFavorite(boolean favorite) {
    if (favorite) {
      if (!isFavorite()) {
        createNewFile(Constants.DOT_FAVORITE);
      }
    } else {
      try {
        Files.deleteIfExists(getPath().resolve(Constants.DOT_FAVORITE));
      } catch (IOException e) {
        //NOP
      }
    }
  }

  default boolean isFavorite() {
    return Files.exists(getPath().resolve(Constants.DOT_FAVORITE));
  }
}
