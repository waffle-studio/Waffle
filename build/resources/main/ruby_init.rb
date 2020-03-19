
class Simulator < Java::jp.tkms.waffle.data.Simulator
    def self.list(entity)
        return Java::jp.tkms.waffle.data.Simulator.getList(entity.project)
    end

    def self.find(entity, id)
        return Java::jp.tkms.waffle.data.Simulator.getInstance(entity.project, id)
    end

    def self.find_by_name(entity, name)
        return Java::jp.tkms.waffle.data.Simulator.getInstanceByName(entity.project, name)
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

class ConductorEntity < Java::jp.tkms.waffle.data.ConductorRun
end

class Run < Java::jp.tkms.waffle.data.SimulatorRun
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

def exec_cycle_process(conductorRun, run)
    exec_process conductorRun do | hub |
        return cycle_process(hub, run)
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
