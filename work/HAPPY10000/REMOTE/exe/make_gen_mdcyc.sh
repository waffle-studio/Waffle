#!/bin/bash

if [ $# -ge 5 ] && [ -n ${5} ]; then
    year="_${5}"
else
    year=""
fi

# make gen_rate.csv from 
ruby ${3}conv_evac2rate.rb ${1} gen_rate.csv 19:00:00 23:59:00

# make gen_list_pure.csv from gen_rate.csv
ruby ${3}moji_gen_1min.rb -t ${4}arraival_triptime${year}.csv -a ${4}real_arraival_agent_1min${year}.csv -r gen_rate.csv -o gen_list_pure.csv -s 19:00:00 -e 23:59:00

# filtering to gen_list.csv from gen_list_pure.csv
ruby ${3}filter.rb -f gen_list_pure.csv -c 2 -s 1.5 -w 4 -o fil2.csv
ruby ${3}filter.rb -f fil2.csv -c 3 -s 1.5 -w 4 -o fil23.csv
ruby ${3}filter.rb -f fil23.csv -c 4 -s 1.5 -w 4 -o fil234.csv
ruby ${3}filter.rb -f fil234.csv -c 5 -s 1.5 -w 4 -o gen_list.csv
rm -f fil2.csv fil23.csv fil234.csv

# make gen.json from gen_list.csv
ruby ${3}make_gen_free.rb -f gen_list.csv -i ${4}gen_itinerary.csv -s ${4}route_scenario.csv -o ${2} -a add_fb.json -t json -d 1

