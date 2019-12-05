package jp.tkms.waffle.collector;

import jp.tkms.waffle.data.ResultCollector;
import jp.tkms.waffle.data.Run;
import jp.tkms.waffle.submitter.AbstractSubmitter;

public class JsonResultCollector extends AbstractResultCollector {
  @Override
  public void collect(Run run, ResultCollector collector) {
    try {
      AbstractSubmitter submitter = AbstractSubmitter.getInstance(run.getHost());
      String json = submitter.exec(run, "cat " + collector.getContents().replaceAll("[\n\r\t]", ""));
      run.putResults(json);
    } catch (Exception e) { e.printStackTrace(); }
  }

  @Override
  public String contentsTemplate() {
    return "_output.json";
  }
}
