
class Simulator < Java::jp.tkms.waffle.data.Simulator
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

class Host < Java::jp.tkms.waffle.data.Host
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

class ConductorArgument
    def initialize(crun)
        @crun = crun
    end

    def [](key)
        @crun.getArgument(key)
    end

    def set_prop(key, value)
        @crun.putArgument(key, value)
    end
end

class ConductorRun < Java::jp.tkms.waffle.data.ConductorRun
end

class Run < Java::jp.tkms.waffle.data.Run
end

class Registry < Java::jp.tkms.waffle.data.Registry
end

def self.alert(text)
    puts "alert: " + text.to_s
    Java::jp.tkms.waffle.data.BrowserMessage.addMessage("toastr.info('" + text.to_s.gsub("[']", "\"") + "');")
end

def get_store(registry, crun_id)
    serialized_store = registry.get(".S:" + crun_id, "[]")
    if serialized_store == "[]" then
        store = Hash.new()
    else
        store = Marshal.load(serialized_store)
    end
end

def exec_pre_process(crun)
    registry = Registry.new(crun.project)
    store = get_store(registry, crun.id)
    pre_process(registry, store, crun)
    registry.set(".S:" + crun.id, Marshal.dump(store))
end

def exec_cycle_process(crun)
    registry = Registry.new(crun.project)
    store = get_store(registry, crun.id)
    cycle_process(registry, store, crun)
    registry.set(".S:" + crun.id, Marshal.dump(store))
end

def exec_post_process(crun)
    registry = Registry.new(crun.project)
    store = get_store(registry, crun.id)
    cycle_process(registry, store, crun)
    registry.set(".S:" + crun.id, nil)
end

def exec_update_value(crun, value)
    registry = Registry.new(crun.project)
    v = update_value(value, registry)
    return v
end

def parameter_extract(run)
end

class Java::JavaLang::Object
    def is_group?()
        return self.is_a?(Java::JavaUtil::HashMap)
    end
end

class Numeric
    def is_group?()
        return false
    end
end
