class Trial
  def initialize(project, id)
    project = Java::jp.tkms.waffle.data.Project.getInstance(project)
    @trial = Java::jp.tkms.waffle.data.Trial.getInstance(project, id)

    $default_trial = @trial
  end

  def id
    return @trial.getId()
  end
end

class Simulator
    def self.list
        return Java::jp.tkms.waffle.data.Simulator.getList($default_project)
    end

    def self.find(id)
        return Java::jp.tkms.waffle.data.Simulator.getInstance($default_project, id)
    end

    def self.find_by_name(name)
        return Java::jp.tkms.waffle.data.Simulator.getInstanceByName($default_project, name)
    end
end

class Host
    def self.list
        return Java::jp.tkms.waffle.data.Host.getList()
    end

    def self.find(id)
        return Java::jp.tkms.waffle.data.Host.getInstance(id)
    end

    def self.find_by_name(name)
        return Java::jp.tkms.waffle.data.Host.getInstanceByName(name)
    end

    def self.local
        return self.find_by_name("LOCAL")
    end
end

class Run
    def initialize(simulator, host)
        @run = Java::jp.tkms.waffle.data.Run.create($default_conductor, $default_trial, simulator, host)
    end

    def start
        @run.start()
    end
end

class Registry
    def initialize
        @registry = Java::jp.tkms.waffle.data.Registry.new($default_project)
    end

    def set(key, value)
        @registry.set(key, value)
    end

    def get_i(key, default)
        return @registry.getInteger(key, default).to_i
    end
end
