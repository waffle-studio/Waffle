def parameter_extract(run)
    run.parameters.each do |key,value|
        run.arguments << value
    end
end