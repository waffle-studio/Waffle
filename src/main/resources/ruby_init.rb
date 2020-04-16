class Simulator < Java::jp.tkms.waffle.data.Simulator
end

class Host < Java::jp.tkms.waffle.data.Host
end

class Conductor < Java::jp.tkms.waffle.data.Conductor
end

class ConductorArgument
    def initialize(entity)
        @entity = entity
    end

    def [](key)
        @entity.getArgument(key)
    end

    def set_prop(key, value)
        @entity.putArgument(key, value)
    end
end

class ConductorRun < Java::jp.tkms.waffle.data.ConductorRun
end

class SimulatorRun < Java::jp.tkms.waffle.data.SimulatorRun
end

class Registry < Java::jp.tkms.waffle.data.Registry
end

def self.alert(text)
    puts "alert: " + text.to_s
    Java::jp.tkms.waffle.data.BrowserMessage.addMessage("toastr.info('" + text.to_s.gsub("[']", "\"") + "');")
end

def get_store(registry, entity_id)
    serialized_store = registry.get(".S:" + entity_id, "[]")
    if serialized_store == "[]" then
        store = Hash.new()
    else
        store = Marshal.load(serialized_store)
    end
end

class Hub < Java::jp.tkms.waffle.data.util.Hub
    def initialize(conductorRun, run)
        super(conductorRun, run)
        @store = get_store(registry, conductorRun.id)
    end

    def close()
        registry.set(".S:" + conductorRun.id, Marshal.dump(store))
    end
end

def exec_process(conductorRun, run, &block)
    result = true
    hub = Hub.new(conductorRun, run)
    result = block.call(hub)
    hub.close
    return result
end


def exec_conductor_script(conductorRun)
    exec_process conductorRun, conductorRun do | hub |
        return conductor_script(hub, conductorRun)
    end
end

def exec_listener_script(conductorRun, run)
    exec_process conductorRun, run do | hub |
        return listener_script(hub, run)
    end
end

def exec_update_value(entity, value)
    registry = Registry.new(entity.project)
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
