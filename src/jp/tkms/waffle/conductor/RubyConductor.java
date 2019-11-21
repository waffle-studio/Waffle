package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.*;
import org.jruby.Ruby;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RubyConductor extends CycleConductor {
  @Override
  protected void preProcess(ConductorRun run) {
    ScriptingContainer container = new ScriptingContainer();
    try {
      container.runScriptlet(getInitScript(run));
      container.runScriptlet(PathType.ABSOLUTE, run.getConductor().getScriptPath().toString());
      container.runScriptlet("pre_process($registry, $store, Trial.new($default_project.id,\"" + run.getTrial().getId() + "\"))");
      container.runScriptlet("$registry.set(\"store:" + run.getId() + "\", Marshal.dump($store))");
    } catch (EvalFailedException e) {
      e.printStackTrace();
      BrowserMessage.addMessage("toastr.error('pre_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
  }

  @Override
  protected void cycleProcess(ConductorRun run) {
    ScriptingContainer container = new ScriptingContainer();
    try {
      container.runScriptlet(getInitScript(run));
      container.runScriptlet(PathType.ABSOLUTE, run.getConductor().getScriptPath().toString());
      container.runScriptlet("cycle_process($registry, $store, Trial.new(\"" + run.getProject().getId() + "\",\"" + run.getTrial().getId() + "\"))");
      container.runScriptlet("$registry.set(\"store:" + run.getId() + "\", Marshal.dump($store))");
    } catch (EvalFailedException e) {
      BrowserMessage.addMessage("toastr.error('cycle_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
  }

  @Override
  protected void postProcess(ConductorRun run) {
    ScriptingContainer container = new ScriptingContainer();
    try {
      container.runScriptlet(getInitScript(run));
      container.runScriptlet(PathType.ABSOLUTE, run.getConductor().getScriptPath().toString());
      container.runScriptlet("post_process($registry, $store, Trial.new(\"" + run.getProject().getId() + "\",\"" + run.getTrial().getId() + "\"))");
      container.runScriptlet("$registry.set(\"store:" + run.getId() + "\", nil)");
    } catch (EvalFailedException e) {
      BrowserMessage.addMessage("toastr.error('post_process: " + e.getMessage().replaceAll("['\"\n]","\"") + "');");
    }
  }

  @Override
  public String defaultScriptName() {
    return "main.rb";
  }

  @Override
  public void prepareConductor(Conductor conductor) {
    try {
      FileWriter filewriter = new FileWriter(conductor.getScriptPath().toFile());

      filewriter.write(
        "def pre_process(registry, store, trial)\n" +
          "end\n" +
          "\n" +
          "def cycle_process(registry, store, trial)\n" +
          "end\n" +
          "\n" +
          "def post_process(registry, store, trial)\n" +
          "end\n");
      filewriter.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private String getInitScript(ConductorRun run) {
    String script = "";
    script += "$conductor_run_id = \"" + run.getId() + "\"\n";
    script += "$default_project = Java::jp.tkms.waffle.data.Project.getInstance(\"" + run.getProject().getId() + "\")\n";
    script += "$default_conductor = Java::jp.tkms.waffle.data.Conductor.getInstance($default_project, \"" + run.getConductor().getId() + "\")\n";
    InputStream in = getClass().getResourceAsStream("/ruby_conductor_init.rb");
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    String data;
    try {
      while (true) {
        if (!((data = reader.readLine()) != null)) break;
        script += data + '\n';
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return script;
  }
}
