def parameter_extract(run)
    run.parameters.keySet.each do |key|
        unless run.parameters.get(key).is_a?(Java::OrgJson::JSONObject) then
            run.addArgument(run.parameters.get(key))
        end
    end
end