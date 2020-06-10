def conductor_script(hub, run)
    hub.p[:iter] = 0
    hub.invokeListener("lis1");
    run.addFinalizer("lis2")
end
