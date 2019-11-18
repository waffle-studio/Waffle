class Trial
  def initialize(project, id)
    project = Java::jp.tkms.waffle.data.Project.getInstance(project)
    @trial = Java::jp.tkms.waffle.data.Trial.getInstance(project, id)
    $default_project = project
  end

  def id
    return @trial.getId()
  end
end

class Simulator
    def self
        puts $default_project
        return Java::jp.tkms.waffle.data.Simulator.getList($default_project)
    end

    def self.find(id)
        return Java::jp.tkms.waffle.data.Simulator.getInstance($default_project, id)
    end
end

