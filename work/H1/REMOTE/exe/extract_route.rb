#! ruby -Ks
# -*- coding: utf-8 -*-

require "csv"
require "json"
require "open3"

logpath = "."
inlogindfn = "log_individual_pedestrians.csv"
amovhisfn  = "agent_movement_history.csv" #all Agents
outroutefn = "route.csv"
midgoalfn  = "goal_logind.csv"
anaagentfn = "analyzeAgent.csv"
routeallfn = "route_all.csv"
type = 3

help = "### Extract route data of each Agents\n" +
	"ruby extract_route.rb [log path, def: %s] [output route, def: %d] [output filename, def: %s] [filename of %s] [filename of %s] [filename of %s] [filename of %s]\n"%[logpath, type, outroutefn, inlogindfn, amovhisfn, anaagentfn, routeallfn] +
	"output route to 2nd column; 1: destiny agents only, 2: free agents only, 3: both agents.\n"

if (ARGV.length == 0) then
	print(help)
end
if (ARGV[0] == "--help" || ARGV[0] == "-h") then
	print(help)
	exit(0)
end

me = $0
print("# %s\n"%[me])

if (ARGV.length >= 2) then
	type = ARGV[1].to_i
end
if (ARGV.length >= 3) then
	outroutefn = ARGV[2]
end
if (ARGV.length >= 4) then
	inlogindfn = ARGV[3]
end
if (ARGV.length >= 5) then
	amovhisfn  = ARGV[4]
end
if (ARGV.length >= 6) then
	anaagentfn = ARGV[5]
end
if (ARGV.length >= 7) then
	routeallfn = ARGV[6]
end
if (ARGV.length >= 1) then
	logpath = ARGV[0]
	inlogindfn = "%s/%s"%[logpath, inlogindfn]
	amovhisfn  = "%s/%s"%[logpath, amovhisfn]
	outroutefn = "%s/%s"%[logpath, outroutefn]
	midgoalfn  = "%s/%s"%[logpath, midgoalfn]
	anaagentfn = "%s/%s"%[logpath, anaagentfn]
	routeallfn = "%s/%s"%[logpath, routeallfn]
end
if (!File.exist?(inlogindfn)) then
	print("! %s is nothing !\n"%[inlogindfn])
	exit(1)
elsif (!File.exist?(amovhisfn)) then
	print("! %s is nothing !\n"%[amovhisfn])
	exit(1)
end

gidx_id      = 0
gidx_time    = 1
gidx_texit   = 4
gidx_journey = 7

hidx_gen    = 0
hidx_id     = 1
hidx_tstartf= 2 # start time h:mm:ss
hidx_tstart = 3 # start time
hidx_texitf = 4 # exit time h:mm:ss
hidx_texit  = 5 # exit time
hidx_mvtf   = 6 # real move time h:mm:ss
hidx_mvt    = 7 # real move time

route_len = 3

anastart_node = "R0-1"
anaend_node   = "R1R2-R3_JUNCTION"
eachroute_node = [ "G1-2", "G2-2", "G3-2" ]

o, e, s = Open3.capture3("grep \",0.0,0.0,\" %s | sort > %s"%[inlogindfn, midgoalfn])
print("grep and sort %s :"%[inlogindfn], o, ", ", e, ", ", s, "\n")

goalc = CSV.read(midgoalfn, "r")
agent_num = goalc.length-1

id      = Array.new(agent_num) # Agent ID
time    = Array.new(agent_num) # exit time
journey = Array.new(agent_num, Array.new) # ex) [INDEX_R1]
route   = Array.new(agent_num) # whitch route R1~R3
start   = Array.new(agent_num) # start Link
mvt     = Array.new(agent_num) # move time [s]
anamvt  = Array.new(agent_num) # analyze move time [s]
endtimes= Array.new(agent_num) # exit time from simulation start [s]

goalc.each.with_index do |a_goal, i|
	id[i] = a_goal[gidx_id]
	time[i] = a_goal[gidx_time]
	endtimes[i] = a_goal[gidx_journey].to_i
	journey[i] = a_goal[gidx_journey]
	route[i] = ""
	if (type >= 2) then
		for w in 1..route_len
			chk_Rw = "R%1d"%[w]
			if (journey[i][journey[i].length-1].include?(chk_Rw)) then
				route[i] = "R%1d"%[w]
				break
			else
				next
			end
		end
	end
end
print("read %s , "%[midgoalfn])

histcf = File.open(amovhisfn, "r")
amvh = Array.new
amvh_header = Array.new
histcf.each.with_index do |a_hist, i|
	if (i == 0) then
		amvh_header = a_hist.scrub('?').chomp.split(",")
		next
	end
	amvhl = a_hist.chomp.split(",")
	amvh.push(amvhl)
end
histcf.close
amvh.sort_by!{ |_, b| b } #sort for 2 column (id)
print("read and sort %s !\n"%[amovhisfn])

amvhcol_num = amvh[0].length
allagent_num = amvh.length

all_id    = Array.new(allagent_num)
all_atype = Array.new(allagent_num)
all_start = Array.new(allagent_num)
all_goal  = Array.new(allagent_num)
all_atag  = Array.new(allagent_num, Array.new)
all_route = Array.new(allagent_num)

all_tstartf = Array.new(allagent_num)
all_tstart  = Array.new(allagent_num)
all_texitf  = Array.new(allagent_num)
all_texit   = Array.new(allagent_num)
all_mvtf    = Array.new(allagent_num)
all_mvt     = Array.new(allagent_num)

isnewamvh = false
tot_route = eachroute_node.length
hidx_route = Array.new(tot_route+1)
issetroute = true
if (amvhcol_num >= 9) then # New agent_movement_history.csv add nodes passed time (2018/06)
	isnewamvh = true

	# Set time of arrival the point where 3 routes join
	all_tanast = Array.new(allagent_num)          # time of start of analyze from simulation start time [s]
	hidx_anast = amvh_header.index(anastart_node) # analyze start time column
	if (hidx_anast == nil) then
		print("## Error! the point where analyze start : " + anastart_node + " is nothing! check it!\n")
		isnewamvh = false
	end
	all_tjoin  = Array.new(allagent_num)          # time of arrival of analyze from simulation start time [s]
	hidx_join  = amvh_header.index(anaend_node)   # analyze end time column
	if (hidx_join == nil) then
		print("## Error! the point where 3 routes join : " + anaend_node + " is nothing! check it!\n")
	end
	all_anamvt = Array.new(allagent_num)
	for i in 1..tot_route
		hidx_route[i] = amvh_header.index(eachroute_node[i-1])
		if (hidx_route[i] == nil) then
			print("## Error! the point of route " + i + " : " + eachroute_node[i-1] + " is nothing! check it!\n")
			issetroute = false
			break
		end
	end
end

amvh.each.with_index do |a_amvh, i|
	all_id[i] = a_amvh[hidx_id]
	all_tstartf[i] = a_amvh[hidx_tstartf]
	all_tstart[i]  = a_amvh[hidx_tstart].to_i
	all_texitf[i]  = a_amvh[hidx_texitf]
	all_texit[i]   = a_amvh[hidx_texit].to_i
	all_mvtf[i]    = a_amvh[hidx_mvtf]
	all_mvt[i]     = a_amvh[hidx_mvt].to_i
	if ((isnewamvh) && (hidx_join >= 9)) then
		isbyway = false # if extra route into byway then true
		if ((a_amvh[hidx_anast] != "") && (a_amvh[hidx_anast] != nil)) then
			all_tanast[i] = a_amvh[hidx_anast].to_i
		else
			isbyway = true
			all_tanast[i] = all_tstart[i]
		end
		all_tjoin[i]  = a_amvh[hidx_join].to_i
		all_anamvt[i] = all_tjoin[i] - all_tanast[i]

		# judge and set ROUTE
		if (issetroute) then
			all_route[i] = "R?"
			for j in 1..tot_route
				if ((a_amvh[hidx_route[j]] != "") && (a_amvh[hidx_route[j]] != nil)) then
					all_route[i] = "R" + j.to_s
					break
				end
			end
			if (isbyway) then
				all_route[i] = "X" + all_route[i]
			end
		end
	end
	jsonl = a_amvh[hidx_gen]
	jsonl.gsub!(/ /,",")
	hash = JSON.parse(jsonl)
	athash = hash["agentType"]
	all_atype[i] = athash["className"]
	all_start[i] = hash["startPlace"]
	all_goal[i] = hash["goal"]
	all_atag[i] = hash["conditions"]
	if (type == 1 || type >= 3) then
		if (all_atype[i] == "NaiveAgent") then
			if (all_atag[i].length > 0) then
				all_route[i] = all_atag[i][0].gsub(/INDEXA_/,"") #if IndexAgent then extract route from INDEXA_R?
			else
				all_route[i] = all_goal[i].gsub(/EXIT_/,"") #if NaiveAgent then extract route from EXIT_Rx
			end
		end
	end
	#print("%d, %s : %s, %s, %d - %s -> %s, %d\n"%[i, all_id[i], all_atype[i], all_start[i], all_tstart[i], all_route[i],  all_goal[i], all_mvt[i]])
end

# make anaagent.csv that new evacuateAgent.csv of new goal
anaagentf = CSV.open(anaagentfn, "wb")
outc = Array.new
for j in 1..tot_route
	outc.push("EXIT|EXIT_R%d|EXIT_STATION"%[j])
end
anaagentf.puts(outc)

max_tjoin = all_tjoin.max
routegoal = Array.new(tot_route+1, 0)
sec = 0
while (sec <= max_tjoin) do
	for i in 0..allagent_num-1
		if (all_tjoin[i] == sec) then
			routegoal[0] = routegoal[0] + 1
			# each route count
			tmp = all_route[i].gsub(/^R|^XR/,"")
			#print("<%s:%s>, "%[all_route[i], tmp])
			if (tmp =~ /^[0-9]+$/)
				routenum = tmp.to_i
			else
				print("# Warning! Route : " + all_route[i] + " -> 1")
				routenum = 1
			end
			if (routenum > tot_route) then
				print("# Warning! Route : " + all_route[i] + " -> 1")
				routenum = 1
			end
			routegoal[routenum] = routegoal[routenum] + 1
		end
	end
	#print("%5d : %5d ; %5d, %5d, %5d\n"%[sec, routegoal[0], routegoal[1], routegoal[2], routegoal[3]])
	outc = Array.new
	for j in 1..tot_route
		outc.push(routegoal[j])
	end
	anaagentf.puts(outc)
	sec = sec + 1
end
anaagentf.close

# make and output route.csv
routecf = CSV.open(outroutefn, "wb")
for i in 0..goalc.length-1
	j = all_id.index(id[i])
	if (route[i] == "") then
		route[i] = all_route[j]
	end
	start[i] = all_start[j]
	mvt[i] = all_mvt[j]
	if (isnewamvh) then
		anamvt[i] = all_anamvt[j]  # set analyze journey time by New agent_movement_history.csv
		if (hidx_join >= 9) then
			endtimes[i] = all_tjoin[j] # analyze goal time
		else
			endtimes[i] = all_texit[j] # exit time
		end
		#AgentID:q,route,arriveTime,analyzeMoveTime,startLink,moveTime,goaltime
		outc = [id[i], route[i], time[i], anamvt[i], start[i], mvt[i], endtimes[i]]
	else
		#AgentID,route,arriveTime,agentLog,startLink,moveTime,exittime
		outc = [id[i], route[i], time[i], journey[i], start[i], mvt[i], endtimes[i]]
	end
	routecf.puts(outc)
end
routecf.close

routeallcf = CSV.open(routeallfn, "wb")
outc = ["#agentId", "agentType", "route", "startLink", "startTimeF", "startTime", "anaStartTime", "goalNode", "goalTimeF", "goalTime", "anaGoalTime", "moveTimeF", "moveTime", "anaMoveTime"]
routeallcf.puts(outc)
for i in 0..allagent_num-1
	outc = [all_id[i], all_atype[i], all_route[i], all_start[i], all_tstartf[i], all_tstart[i], all_tanast[i], all_goal[i], all_texitf[i], all_texit[i], all_tjoin[i], all_mvtf[i], all_mvt[i], all_anamvt[i]]
	routeallcf.puts(outc)
end
routeallcf.close

print("# finished %s !\n"%[me])
