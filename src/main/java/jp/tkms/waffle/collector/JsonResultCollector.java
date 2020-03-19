package jp.tkms.waffle.collector;

import jp.tkms.waffle.data.ResultCollector;
import jp.tkms.waffle.data.SimulatorRun;
import jp.tkms.waffle.submitter.AbstractSubmitter;

public class JsonResultCollector extends AbstractResultCollector {
  @Override
  public void collect(AbstractSubmitter submitter, SimulatorRun run, ResultCollector collector) {
    try {
      String json = submitter.getFileContents(run, collector.getContents().replaceAll("[\n\r\t]", ""));
      run.putParametersByJson(json);
    } catch (Exception e) { e.printStackTrace(); }
  }

  @Override
  public String contentsTemplate() {
    return "_output.json";
  }
}
