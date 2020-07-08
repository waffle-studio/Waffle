def result_collect(run, remote)
  json = remote.getFileContents("_output.json")
  run.putResultsByJson(json)
end