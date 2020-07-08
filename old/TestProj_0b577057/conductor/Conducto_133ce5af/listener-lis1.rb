def listener_script(hub, run)
    if hub.p[:iter] < 2 then
        r = hub.createSimulatorRun("test.sh", "LOCAL")
        r.addFinalizer("lis1")
        r.start
        hub.p[:iter] += 1
        puts "@" + hub.p[:iter].to_s
    end
end
