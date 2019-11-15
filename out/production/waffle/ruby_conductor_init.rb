class Trial
  @@trial = nil
  def initialize(project, id)
    project = Java::jp.tkms.waffle.data.Project.getInstance(project)
    @@trial = Java::jp.tkms.waffle.data.Trial.getInstance(project, id)
  end

  def id
    return @@trial.getId()
  end
end
