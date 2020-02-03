package jp.tkms.waffle.conductor;

import jp.tkms.waffle.data.*;
import org.json.JSONObject;

public class TestConductor extends AbstractConductor {
  @Override
  protected void mainProcess(ConductorEntity entity) {
    Conductor conductor = entity.getConductor();
    for ( Simulator simulator : Simulator.getList(conductor.getProject()) ) {
      Run.create(entity, simulator, Host.getInstanceByName(entity.getArgument("host").toString())).start();
    }
  }

  @Override
  protected void eventHandler(ConductorEntity entity, AbstractRun run) {

  }

  @Override
  protected void postProcess(ConductorEntity entity) {

  }

  @Override
  protected void suspendProcess(ConductorEntity entity) {

  }

  @Override
  public String defaultScriptName() {
    return "";
  }

  @Override
  public void prepareConductor(Conductor conductor) {
    conductor.setArguments("{'host':'LOCAL'}");
  }
}
