package jp.tkms.waffle.data;

  import jp.tkms.waffle.Constants;

  import java.io.File;
  import java.nio.file.Paths;
  import java.util.UUID;

abstract public class SimulatorData extends Data {
  private Simulator simulator;

  public SimulatorData(Simulator simulator, UUID id, String name) {
    super(id, name);
    this.simulator = simulator;
  }

  public SimulatorData(Simulator simulator) {
    this.simulator = simulator;
  }

  public Simulator getSimulator() {
    return simulator;
  }

  protected Database getDatabase() {
    return Database.getDatabase(Paths.get(simulator.getDirectory() + File.separator + Constants.SIMULATOR_DB_NAME));
  }
}
