class Trial
  def initialize(project, id)
    project = Java::jp.tkms.waffle.data.Project.getInstance(project)
    @trial = Java::jp.tkms.waffle.data.Trial.getInstance(project, id)
    Simulator.default_project = project
  end

  def id
    return @trial.getId()
  end
end

class Simulator
    @@default_project = nil

    def self.find(id)
        return new Simulator(@@default_project, id)
end

