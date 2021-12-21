package jp.tkms.waffle.data;

public interface HasNote extends DataDirectory {
  String NOTE_TXT = "NOTE.txt";

  default void setNote(String text) {
    createNewFile(NOTE_TXT);
    updateFileContents(NOTE_TXT, text);
  }

  default void appendNote(String text) {
    createNewFile(NOTE_TXT);
    appendFileContents(NOTE_TXT, text);
  }

  default String getNote() {
    return getFileContents(NOTE_TXT);
  }
}
