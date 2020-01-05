
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

class ConductorEntity < Java::jp.tkms.waffle.data.ConductorEntity
end

class Run < Java::jp.tkms.waffle.data.Run
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

def exec_pre_process(entity)
    registry = Registry.new(entity.project)
    store = get_store(registry, entity.id)
    pre_process(entity, store, registry)
    registry.set(".S:" + entity.id, Marshal.dump(store))
end

def exec_cycle_process(entity)
    registry = Registry.new(entity.project)
    store = get_store(registry, entity.id)
    cycle_process(entity, store, registry)
    registry.set(".S:" + entity.id, Marshal.dump(store))
end

def exec_post_process(entity)
    registry = Registry.new(entity.project)
    store = get_store(registry, entity.id)
    cycle_process(entity, store, registry)
    registry.set(".S:" + entity.id, nil)
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
