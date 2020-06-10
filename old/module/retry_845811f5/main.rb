def register_default_parameters(hub)
    hub.p[:max_rerun] = 5
end

def cycle_process(hub, run)
    unless run.nil? then
        if run.success? 
            return true
        elsif run.getRestartCount < hub.p[:max_rerun]
            run.restart
        end
        return false
    end
    return true
end

def post_cycle_process(hub, run)
end

def finalize_process(hub)
end
