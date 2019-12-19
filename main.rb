def pre_process(entity, store, registry)
  store[:count] = 0
end

def cycle_process(entity, store, registry)
  alert(store[:count])
  if store[:count] < 3 then
    host = Host.find_by_name("localhost")
    sim = Simulator.list(entity).first
    run = Run.create(entity, sim, host)
    run.start
  end      
  store[:count] += 1
end

def post_process(entity, store, registry)
end

