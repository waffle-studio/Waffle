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
    def initialize(conductorRun)
        super(conductorRun)
        @store = get_store(registry, conductorRun.id)
    end

    def close()
        registry.set(".S:" + conductorRun.id, Marshal.dump(store))
    end
end

def exec_process(conductorRun, &block)
    result = true
    hub = Hub.new(conductorRun)
    result = block.call(hub)
    hub.close
    return result
end

def exec_register_default_parameters(conductorRun, moduleInstanceName)
    exec_process conductorRun do | hub |
        hub.switchParameterStore(moduleInstanceName)
        register_default_parameters(hub)
        hub.setParameterStore(nil)
        return true
    end
end

def exec_conductor_script(conductorRun)
    exec_process conductorRun do | hub |
        return conductor_script(hub, conductorRun)
    end
end

def exec_post_cycle_process(conductorRun, run)
    exec_process conductorRun do | hub |
        post_cycle_process(hub, run)
        return true
    end
end

def exec_finalize_process(conductorRun)
    exec_process conductorRun do | hub |
        return finalize_process(hub, run)
    end
end

def exec_listener_script(conductorRun, run)
    exec_process conductorRun do | hub |
        return listener_script(hub, run)
    end
end

def exec_module_cycle_process(conductorRun, moduleInstanceName, run)
    exec_process conductorRun do | hub |
        hub.switchParameterStore(moduleInstanceName)
        result = cycle_process(hub, run)
        hub.setParameterStore(nil)
        return result
    end
end

def exec_module_post_cycle_process(conductorRun, moduleInstanceName, run)
    exec_process conductorRun do | hub |
        hub.switchParameterStore(moduleInstanceName)
        post_cycle_process(hub, run)
        return true
    end
end

def exec_module_finalize_process(conductorRun, moduleInstanceName)
    exec_process conductorRun do | hub |
        hub.switchParameterStore(moduleInstanceName)
        result = finalize_process(hub, run)
        hub.setParameterStore(nil)
        return result
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
