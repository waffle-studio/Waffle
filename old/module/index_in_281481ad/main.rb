def register_default_parameters(hub)
    hub.p[:begin] = 0
    hub.p[:end] = 1
end

def cycle_process(hub, run)
    if run.nil? then
        hub.p[:count] = hub.ps[:begin].to_i
    else
        hub.p[:count] += 1
    end
    return hub.p[:count] <= hub.p[:end].to_i
end

def post_cycle_process(hub, run)
end

def finalize_process(hub)
end