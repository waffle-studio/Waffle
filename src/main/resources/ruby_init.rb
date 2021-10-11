module Waffle
    class Executable < Java::jp.tkms.waffle.data.project.executable.Executable
    end

    class Computer < Java::jp.tkms.waffle.data.computer.Computer
    end

    class Conductor < Java::jp.tkms.waffle.data.project.conductor.Conductor
    end

    class Registry < Java::jp.tkms.waffle.data.project.workspace.Registry
    end

    def self.systemRestart()
        Java::jp.tkms.waffle.Main.restart()
    end

    def self.alert(text)
        Java::jp.tkms.waffle.data.log.message.DebugLogMessage.issue("alert: " + text.to_s);
    end

    def self.findRun(id)
        Java::jp.tkms.waffle.data.project.workspace.run.ExecutableRun.find(id)
    end

    class ActorWrapper
        def initialize(actorRun)
            @instance = actorRun
            @store = get_store(@instance.getRegistry, @instance.id)
            #@template_argument =  get_template_argument(@instance.getRegistry, @instance.id)
        end

        def get_store(registry, entity_id)
            serialized_store = registry.get(".S:" + entity_id, "[]")
            if serialized_store == "[]" then
                store = Hash.new()
            else
                store = Marshal.load(serialized_store)
            end
        end

        def close
        #TODO: check with depth
            @instance.getRegistry.set(".S:" + @instance.getId, Marshal.dump(@store))
            #@instance.getRegistry.set(".TA:" + @instance.getId, Marshal.dump(@template_argument))
            @instance.commit
            @instance.getRegistry.set(".S:" + @instance.getId, Marshal.dump(@store))
            #@instance.getRegistry.set(".TA:" + @instance.getId, Marshal.dump(@template_argument))
        end

        def id
            @instance.id
        end

        def createConductorRun(conductor_name, name)
            @instance.createConductorRun(conductor_name, name)
        end

        def createConductorRun(conductor_name)
            @instance.createConductorRun(conductor_name, conductor_name)
        end

        def createExecutableRun(executable_name, computer_name, name)
            @instance.createExecutableRun(executable_name, computer_name, name)
        end

        def createExecutableRun(executable_name, computer_name)
            @instance.createExecutableRun(executable_name, computer_name, executable_name)
        end

        def createSyncExecutableRun(executable_name, computer_name, name)
            @instance.createSyncExecutableRun(executable_name, computer_name, name)
        end

        def createSyncExecutableRun(executable_name, computer_name)
            @instance.createSyncExecutableRun(executable_name, computer_name, executable_name)
        end

        def addFinalizer(name)
            @instance.addFinalizer(name)
        end

        def setAppealHandler(name)
            @instance.setAppealHandler(name)
        end

        def v
            @instance.v
        end
    end
end

class Java::JavaLang::Object
    def is_group?()
        return self.is_a?(Java::JavaUtil::Map)
    end
end

class Numeric
    def is_group?()
        return false
    end

    def to_str
        return to_s
    end
end


def exec_parameter_extract(run)
    Dir.chdir(run.getBasePath().toString()) do
        parameter_extract(run)
    end
end

def exec_result_collect(run, remote)
    Dir.chdir(run.getBasePath().toString()) do
        result_collect(run, remote)
    end
end

def exec_procedure(instance, refs)
    result = true
    #local_instance = Waffle::ActorWrapper.new(instance)
    result = procedure(instance, refs)
    #local_instance.close
    return result
end

def exec_procedure_when_start_or_finished_all(instance, caller)
    result = true
    #local_instance = Waffle::ActorWrapper.new(instance)
    result = procedure_when_start_or_finished_all(instance, caller)
    #local_instance.close
    return result
end

def exec_procedure_when_contain_fault(instance, caller)
    result = true
    #local_instance = Waffle::ActorWrapper.new(instance)
    result = procedure_when_contain_fault(instance, caller)
    #local_instance.close
    return result
end

def exec_procedure_when_result_updated(instance, caller, key, value)
    result = true
    #local_instance = ActorWrapper.new(instance)
    result = procedure_when_result_updated(instance, caller, key, value)
    #local_instance.close
    return result
end

def exec_procedure_when_appealed(instance, caller, appealer, message)
    result = true
    #local_instance = Waffle::ActorWrapper.new(instance)
    result = procedure_when_appealed(instance, caller, appealer, message)
    #local_instance.close
    return result
end