def pre_process(entity, store, registry)
end

def cycle_process(entity, store, registry)
    entity.getTrial.getChildRunList.each do |run|
        if run.getRestartCount < store[:max_rerun]:
            run.restart
        end
    end
end

def post_process(entity, store, registry)
end
