package jp.tkms.waffle.data;

  import jp.tkms.waffle.Constants;

  import java.io.File;
  import java.nio.file.Path;
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

  @Override
  protected Path getPropertyStorePath() {
    return simulator.getDirectory().resolve(Simulator.KEY_SIMULATOR + Constants.EXT_JSON);
  }
}
