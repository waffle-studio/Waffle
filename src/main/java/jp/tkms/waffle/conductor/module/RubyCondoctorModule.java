package jp.tkms.waffle.conductor.module;

import jp.tkms.waffle.data.Conductor;
import jp.tkms.waffle.data.ConductorModule;

import java.io.FileWriter;
import java.io.IOException;

public class RubyCondoctorModule {
  public static void prepareModule(ConductorModule module) {
    try {
      FileWriter filewriter = new FileWriter(module.getScriptPath().toFile());

      filewriter.write(
        "def pre_process(entity, store, registry)\n" +
          "end\n" +
          "\n" +
          "def cycle_process(entity, store, registry)\n" +
          "end\n" +
          "\n" +
          "def post_process(entity, store, registry)\n" +
          "end\n");
      filewriter.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
