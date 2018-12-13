#!/bin/bash
umask 002

## input data
exid=$1
cycno=$2
#islandno=$3
testno=$3
# now temporary
#exid="TEST01"
#islandno=0
#cycno=0
#testno=0

## base data dir
datadir="/fs1/groups1/gaa50073/moji/work/${exid}/"
#datadir="/tmp/abci/work/${exid}/"

## cycle dir
cycdir="${datadir}cyc/CMA_test/"

## input file
testfile="${cycdir}cyc${cycno}/test${testno}.csv"
evacfile="${cycdir}evacuatedAgent.csv"

# execute dir
exedir="${datadir}exe/"
#crowdwalkdir="/projects/g-nairobi/social_intelligence/tools/CrowdWalk/crowdwalk"
#crowdwalkdir="${HOME}/CrowdWalk/crowdwalk/"
crowdwalkdir="/fs1/groups1/gaa50073/moji/crowdwalk/"
#crowdwalkdir="/mnt/abci/fs1/groups1/gaa50073/moji/crowdwalk/"

## scenario data
scendir="${datadir}scen/"

## output data
layersdir="cyc${cycno}/test${testno}/"
simdir="${datadir}sim/${layersdir}"
anadir="${datadir}ana/${layersdir}"
pydir="${datadir}exe/python/"
#happydir="${anadir}"

## in ~/oacis_work/XXXXXXXX/
currentdir="`pwd`/"
simlocaldir="${currentdir}sim/"

echo "${testno} ${cycno}" > input.txt 

#\cp -v ${testdir}/prop.json .
cat << _PROP_ > prop.json
{
  "__0":"NetmasCuiSimulator",
  "debug":true,
  "io_handler_type":"none",
  "map_file":"${scendir}moji_24.xml",
  "generation_file":"./gen.json",
  "scenario_file":"./scen.json",
  "fallback_file":"${scendir}fb.json",
  "node_appearance_file":"${scendir}node_appearance.json",
  "camera_file":"${scendir}camera.json",

  "randseed":2524,
  "random_navigation":true,

  "interval":0,
  "loop_count":1,
  "exit_count":0,
  "all_agent_speed_zero_break":true,

  "create_log_dirs":true,
  "agent_movement_history_file":"${simlocaldir}agent_movement_history.csv",
  "checkpoints_of_agent_movement_history_log": "R0-1,J1,G1-3,G1-2,R1-3,R2-1,R2-2,X_R2-R3,J2,G2-2,R2-3,G3-2,G3-1,G4,R1R2-R3_JUNCTION",
  "individual_pedestrians_log_dir":"${simlocaldir}",
  "evacuated_agents_log_file":"${simlocaldir}evacuatedAgent.csv",
  "node_order_of_evacuated_agents_log":"EXIT_R1,EXIT_R2,EXIT_R3",

  "clear_screenshot_dir":false,
  "screenshot_image_type":"png",

  "vertical_scale":2.0,
  "agent_size":1.6,
  "zoom":1.6,
  "hide_links":false,
  "change_agent_color_depending_on_speed":true,
  "show_status":"Bottom",
  "show_logo":false,
  "show_3D_polygon":true,

  "simulation_window_open":false,
  "auto_simulation_start":true,
  "exit_with_simulation_finished":true,

  "use_ruby": true,
  "ruby_load_path": "${scendir}",
  "ruby_simulation_wrapper_class":"GateOperation",
  "ruby_init_script":"
    require 'GateOperation'
    \$settings = {
      monitor: true,
      gate_node_tag: 'EXIT_STATION_ROOT',
      count_by_entering: true,
      counting_positions: [
      {
        link_tag: 'GL_R1',
        node_tag: 'EXIT_STATION_ROOT'
      },
      {
        link_tag: 'GL_R2',
        node_tag: 'EXIT_STATION_ROOT'
      },
      {
        link_tag: 'GL_R3',
        node_tag: 'EXIT_STATION_ROOT'
      }
    ],
    delay_time: 60,
    diagram_file: '${scendir}diagram_20160813.csv'
  }"
}
_PROP_

echo `date +"%Y%m%d%k%M%S"`" start of ${exid} ; test-cyc= "`cat input.txt`
echo ${datadir}
echo

if [ ! -e ${datadir} ]; then
    echo "Nothing data dir error: ${datadir}" 2>&1
    exit -1
fi
if [ ! -e ${exedir} ]; then
    echo "Nothing exe dir error: ${exedir}" 1>&2
    exit -1
fi
if [ ! -e ${scendir} ]; then
    echo "Nothing scen dir error: ${scendir}" 1>&2
    exit -1
fi
if [ ! -e ${testfile} ]; then
    echo "Nothing test file error: ${testfile}" 1>&2
    exit -1
fi
if [ ! -e ${evacfile} ]; then
    echo "Nothing evac file error: ${evacfile}" 1>&2
    exit -1
fi

mkdir -p ${simlocaldir}
mkdir -p ${simdir}
mkdir -p ${anadir}
#mkdir -p ${happydir}

\cp -v ${evacfile} evacuatedAgent_base.csv
\cp -v ${testfile} CMA_test.csv

sh ${exedir}make_gen_mdcyc.sh evacuatedAgent_base.csv gen.json ${exedir} ${scendir} > /dev/null
stt=$?
if [ ${stt} != 0 ]; then
    echo `date`" make_gen_mdcyc.sh error ${stt}" 1>&2
    exit ${stt}
fi

ruby ${exedir}make_scen.rb -f CMA_test.csv -o scen.csv -s 19:00:00 -e 23:00:00 -t 60.000000 > /dev/null
stt=$?
if [ ${stt} != 0 ]; then
    echo `date`" make_scen.rb error ${stt}" 1>&2
    exit ${stt}
fi

ruby ${exedir}conv_scen_csv2json.rb -f scen.csv -o scen.json > /dev/null
stt=$?
if [ ${stt} != 0 ]; then
    echo `date`" make_scen.rb error ${stt}" 1>&2
    exit ${stt}
fi

echo `date +"%Y%m%d%k%M%S"`" pre end ${stt} & sim start"

echo "sh ${crowdwalkdir}quickstart.sh prop.json -c -lError"
sh ${crowdwalkdir}quickstart.sh prop.json -c -lError
stt=$?
if [ ${stt} != 0 ]; then
    echo `date`" sim error ${stt}" 1>&2
    exit ${stt}
fi
#\cp -v ${simdir}evacuatedAgent.csv .

echo `date +"%Y%m%d%k%M%S"`" sim end ${stt} & ana start"

echo rm -rf ${simdir:0:-1}
rm -rf ${simdir:0:-1}

echo "ruby ${exedir}extract_route.rb ${simlocaldir}"
ruby ${exedir}extract_route.rb ${simlocaldir}
stt=$?
if [ ${stt} != 0 ]; then
    echo `date`" extract_route.rb error ${stt}" 1>&2
    exit ${stt}
fi

echo cp -r ${simlocaldir:0:-1} ${simdir:0:-1}
cp -r ${simlocaldir:0:-1} ${simdir:0:-1}

\cp -v ${simdir}analyzeAgent.csv ${simdir}route*.csv ${simdir}agent_movement_history.csv .
\cp -v ${simdir}analyzeAgent.csv ${simdir}route*.csv ${simdir}agent_movement_history.csv ${anadir}

cd ${exedir}java
echo "java CMA_Analyze_map24 ${simdir} ${anadir} ${datadir}ana/cyc${cycno}/ ${pydir} < ${currentdir}input.txt"
java CMA_Analyze_map24 ${simdir} ${anadir} ${datadir}ana/cyc${cycno}/ ${pydir} < ${currentdir}input.txt
stt=$?
if [ ${stt} != 0 ]; then
    cd -
    echo `date`" CMA_Analyze error ${stt}" 1>&2
    exit ${stt}
fi
cd -

echo cp ${simdir}evacuatedAgent.csv ${anadir}evacuatedAgentO.csv
cp ${simdir}evacuatedAgent.csv ${anadir}evacuatedAgentO.csv

echo "ruby ${exedir}rough_plot_Happy_cma.rb ${exedir} ${anadir} ${scendir} ${cycno} ${testno}"
ruby ${exedir}rough_plot_happy_cma.rb ${exedir} ${anadir} ${scendir} ${cycno} ${testno}
stt=$?
if [ ${stt} != 0 ]; then
    echo `date`" rough_plot.rb error ${stt}" 1>&2
    exit ${stt}
fi

cd ${anadir}
tar --remove-files -czf Route.tar.gz Route?_*.csv ???_Ave_Var.csv goalagent_raw.* goalagent_filt.* move_line.png *_bar.png agent_movement_history.csv route.csv route_all.csv
cd -

tail -1 Happy.csv | sed -E 's/^(.*),(.*),(.*),(.*),(.*),(.*),(.*)$/{ "no":"\1", "stop_pow":\2, "stop":\3, "ave-sum":\4, "diff-sum":\5, "ave-max":\6, "diff-max":\7}/' > _output.json
#tail -1 Happy.csv | sed -E 's/^(.*),(.*),(.*),(.*),(.*),(.*)$/{ "no":"\1","stop":\2, "ave-sum":\3, "diff-sum":\4, "ave-max":\5, "diff-max":\6}/' > _output.json
#cat Result.csv | sed -E 's/^(.*),(.*),(.*),(.*),(.*),(.*),(.*),(.*),(.*,.*)$/{ "no":"\1", "ave-sum":\2, "diff-sum":\3, "ave-max":\4, "diff-max":\5, "flow":\6, "r1":\7, "r2":\8|\9/'| sed -E 's/^(.*)\|(.*),(.*)$/\1, "r3":\2, "sum":\3}/' > _output.json
#cat Result.csv | sed -E 's/^(.*),(.*),(.*),(.*),(.*),(.*),(.*),(.*),(.*,.*)$/{ "no":"\1", "flow":\2, "r1":\3, "r2":\4, "r3":\5, "sum":\6, "ave-max":\7, "diff-max":\8|\9/'| sed -E 's/^(.*)\|(.*),(.*)$/\1, "ave-sum":\2, "diff-sum":\3}/' > _output.json


ls -ltr

#tar czf run_c${cycno}-t${testno}.tar.gz *
#rm -f prop.json gen_rate.csv gen_list_pure.csv gen_list.csv gen.json add_fb.json scen.csv scen.json goalagent_raw.png goalagent.csv route_all.csv agent_movement_history.csv goalagent_filt.png move_line.png happy_process.png

echo `date +"%Y%m%d%k%M%S"`" end ${stt}"

exit ${stt}
