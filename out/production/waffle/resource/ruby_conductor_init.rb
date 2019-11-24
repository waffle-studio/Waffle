
class Simulator
    def self.list(crun)
        return Java::jp.tkms.waffle.data.Simulator.getList(crun.project)
    end

    def self.find(crun, id)
        return Java::jp.tkms.waffle.data.Simulator.getInstance(crun.project, id)
    end

    def self.find_by_name(crun, name)
        return Java::jp.tkms.waffle.data.Simulator.getInstanceByName(crun.project, name)
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

class Conductor < Java::jp.tkms.waffle.data.Conductor
end

class ConductorRun < Java::jp.tkms.waffle.data.ConductorRun
end

class Run
    def initialize(crun, simulator, host)
        @run = Java::jp.tkms.waffle.data.Run.create(crun.conductor, crun.trial, simulator, host)
    end

    def start
        @run.start()
    end
end

class Registry
    def initialize(crun)
        @registry = Java::jp.tkms.waffle.data.Registry.new(crun.project)
    end

    def set(key, value)
        @registry.set(key, value)
    end

    def get_i(key, default)
        return @registry.getInteger(key, default)
    end

    def get_s(key, default)
        return @registry.getString(key, default)
    end
end

def alert_info(text)
    puts "INFO: " + text
    Java::jp.tkms.waffle.data.BrowserMessage.addMessage("toastr.info('" + text.gsub("[']", "\"") + "');")
end

def get_store(registry, crun_id)
    serialized_store = registry.get_s("store:" + crun_id, "[]")
    if serialized_store == "[]" then
        store = Hash.new()
    else
        store = Marshal.load(serialized_store)
    end
end

def exec_pre_process(crun)
    registry = Registry.new(crun)
    store = get_store(registry, crun.id)
    pre_process(registry, store, crun)
    registry.set("store:" + crun.id, Marshal.dump(store))
end

def exec_cycle_process(crun)
    registry = Registry.new(crun)
    store = get_store(registry, crun.id)
    cycle_process(registry, store, crun)
    registry.set("store:" + crun.id, Marshal.dump(store))
end

def exec_post_process(crun)
    registry = Registry.new(crun)
    store = get_store(registry, crun.id)
    cycle_process(registry, store, crun)
    registry.set("store:" + crun.id, nil)
end
