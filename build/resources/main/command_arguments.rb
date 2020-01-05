def parameter_extract(run)
    run.parameters.each do |key,value|
        unless value.is_group? then
            run.arguments << value
        end
    end
end