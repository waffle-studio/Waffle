#! ruby -Ks
# -*- coding: utf-8 -*-

require "csv"
require "json"
require "complex"

def settime(settimestr)
	strAry = settimestr.split(":")
	timecase =  Time.new(2016, 8, 13, strAry[0].to_i, strAry[1].to_i, strAry[2].to_i, "+09:00")
	return timecase
end

def strtime(timecase)
	timestr = timecase.strftime("%H:%M:%S")
	if (timestr[0,3] == "00:") then
		timestr = "24" + timestr[2,6]
	end
	return timestr
end

## default
genfilename  = "gen_list.csv"
itinfilename = "gen_itinerary.csv"
rtscfilename = "route_scenario.csv"
fm_json = 1
fm_csv  = 0
form = fm_json	#Type of output file format, 1:json, 0:csv
outfilename  = "output_gen.%s"%[(form == fm_json ? "json" : "csv")]
fbfilename = "add_fb.json"
org_outfilename = outfilename
ndest = 0
islog = false

help = "### Make generation file in json or csv form.\n" +
	"$ ruby make_gen_free.rb [option -[fisot]] [setting value]\n" +
	" -f [in filename]  : Set input num of generate agents timetable File name, def: %s .\n"%[genfilename] +
	" -i [in filename]  : Set settings of agents Itinerary file name, def: %s .\n"%[itinfilename] +
	" -s [in filename]  : Set settings of route Scenario timetable file name, def: %s .\n"%[rtscfilename] +
	" -o [out filename] : Set Output scenario file name, def: %s .\n"%[outfilename] +
	" -a [out filename] : Set Output add fallback file name, def: %s .\n"%[fbfilename] +
	" -t [file type]    : Set Type of output file format, 1 or json: json, 0 or csv: csv, def: %d .\n"%[form] +
	" -d [1/0]          : Are Destined agent appear every 1 min? Yes: 1, No: 0, def: %d .\n"%[ndest] +
	" -l [true or 1]    : set true or 1 then output Log; default: false.\n"

if (ARGV.length == 0 || ARGV.length.odd?) then
	print(help)
	exit(1)
end

me = $0
print("# %s\n"%[me]);

opttyp = Array.new(ARGV.length/2)
optval = Array.new(ARGV.length/2)
iop = 0
ARGV.each.with_index do |arg, idx|
	if (idx.even?) then
		if (arg[0, 1] == "-") then
			opttyp[iop] = arg[1, 1]
		else 
			print(help)
			exit(1)
		end
	else
		optval[iop] = arg
		iop += 1
	end
end

opttyp.each.with_index do |opt, idx|
	val = optval[idx]
	case opt
	when "f" then
		genfilename = val
	when "i" then
		itinfilename = val
	when "s" then
		rtscfilename = val
	when "o" then
		outfilename = val
	when "a" then
		fbfilename = val
	when "t" then
		if (val.downcase == "json") then
			val = fm_json.to_s
		elsif (val.downcase == "csv") then
			val = fm_csv.to_s
		end
		form = val.to_i
		if (form != fm_json && form != fm_csv) then
			print("! ERROR, wrong file type = %d !\n"%[form])
			print(help)
			exit(1)
		end
		if (org_outfilename == outfilename) then
			outfilename  = "output_gen.%s"%[(form == fm_json ? "json" : "csv")]
		end
	when "d" then
		ndest = val.to_i
	when "l" then
		if (val == "1" || val.downcase == "true") then
			islog =true
		end
	else
		print("! ERROR, wrong option = %s\n"%[opt])
		print(help)
		exit(1)
	end
end
if (!File.exist?(genfilename)) then
	print("! ERROR, %s is nothing !\n"%[genfilename])
	print(help)
	exit(1)
elsif (!File.exist?(itinfilename)) then
	print("! ERROR, %s is nothing !\n"%[itinfilename])
	print(help)
	exit(1)
elsif (!File.exist?(rtscfilename)) then
	print("! ERROR, %s is nothing !\n"%[rtscfilename])
	print(help)
	exit(1)
end

print("Convert agent generation file %s to json file %s !\n"%[genfilename, outfilename])

duration = 60

gen_headers,  *gen  = CSV.read(genfilename)		# num of generate Agents / 1 min
itin_headers, *itin = CSV.read(itinfilename)	# Agent generation file settings
rtsc_headers, *rtsc = CSV.read(rtscfilename)	# Agent start setting timetable

#make gen.json/csv
json_head = "#\{ \"version\" : 2\}\n[\n"
json_tail = "]\n"
fb_rule = nil
fb_speedModel = nil
fb_className = nil

File.open(outfilename, 'w') do |out|
	if (form == fm_json) then
		out.print(json_head)
		if (islog) then
			print(json_head)
		end
	end

	gen.each.with_index do |genline, idx|
		startTime = settime(genline[1])
		target = settime(gen[0][1])
		#read route_scenario.csv
		rtsc.each do |a_rtsc|
			rtsctime = settime(a_rtsc[0]) #time_start [hh:mm:ss]
			if (rtsctime > startTime) then
				break
			end
			if (rtsctime > target) then
				target = rtsctime
			end
		end
		alloc = rtsc.select { |a_rtsc| settime(a_rtsc[0]) == target }
		alloc.each.with_index do |a_alloc, jdx|
			route = a_alloc[1].to_i #route [0:discretionAgent, 1~3:route for destinyAgent, -3~-1:route(*-1) for indexAgent]
			if (ndest == 0 && route < 0) then
				next
			end
			startPlace = a_alloc[2] #start_link [SL_?]
			a_itin = itin.find{ |a_itin| a_itin[0] == startPlace && a_itin[1].to_i == route }
			rule = a_itin[2]
			className = a_itin[3]
			speedModel = a_itin[4]
			goal = a_itin[5]
			plannedRoute = a_itin[6]
			agentTag = nil
			if (a_itin.length > 7) then
				agentTag = a_itin[7]
			end
			rate = a_alloc[3].to_f #rate_AB [0.0~1.0]
			if (route >= 0) then
				gen_agent = genline[2+route].to_f
				alloc_gen = (gen_agent * rate).round(0).to_i
			else
				alloc_gen =  rate.round(0).to_i
			end
			if ((alloc.length > 4) && (a_alloc[4] != nil) && (a_alloc[4] != "")) then
				if ((a_alloc[4] =~ /^[0-9]+$/) && (a_alloc[4].to_i > 0)) then
					duration = a_alloc[4].to_i #duration [s]
				end
			end

			if (form == fm_csv) then
				outline = Array.new
				outline.push(rule)					#RULE
				outline.push(startPlace)			#START TAG
				outline.push(strtime(startTime))	#START TIME
				if (rule == "TIMEEVERY") then
					outline.push(strtime(startTime + duration.to_f))	#END TIME
					outline.push(duration + 1)		#EVERY
				end
				outline.push(duration)				#DURATION
				outline.push(alloc_gen)				#TOTAL
				outline.push(speedModel)			#SPEED MODEL (add NEW)
				if (rule == "EACHRANDOM") then
					outline.push(alloc_gen)			#EACH ?
				end
				outline.push(goal)					#EXIT TAG
				outline.push(plannedRoute)			#ROUTE
				outline.push(agentTag)				#AGENT TAG
				outline_len = outline.length
				for k in 0..outline_len-2
					out.print(outline[k],",")
					if (islog) then
						print(outline[k],",")
					end
				end
				out.print(outline[outline_len-1],"\n")
				if (islog) then
					print(outline[outline_len-1],"\n")
				end
			elsif (form == fm_json) then
				hash_agentType = { "className" => className }
				#hash_agentType.store("rubyAgentClass", rubyAgentClass)	#if RubyAgent
				hash = { "rule" => rule}
				hash.store("agentType", hash_agentType)
				hash.store("startTime", strtime(startTime))
				if (rule == "TIMEEVERY") then
					hash.store("everyEndTime", strtime(startTime + duration.to_f))
					hash.store("everySeconds", duration + 1)
				end
				hash.store("total", alloc_gen)
				#hash.store("speedModel", speedModel)	#move to fallback
				#hash.store("conditions", nil)			#move to fallback
				hash.store("duration", duration)
				hash.store("startPlace", startPlace)
				hash.store("goal", goal)
				if (plannedRoute != nil) then
					jsonpr = plannedRoute.split(",")
					hash.store("plannedRoute", jsonpr)
				end
				if (agentTag != nil) then
					jsonat = agentTag.split(",")
					hash.store("conditions", jsonat)
				end
				outline = "  " + JSON.dump(hash)
				out.print(outline)
				if (islog) then
					print(outline)
				end
				if (idx != gen.length-1 || jdx != alloc.length-1) then
					out.print(",")
					if (islog) then
						print(",")
					end
					fb_rule = rule
					fb_speedModel = speedModel
					fb_className = className
				end
				out.print("\n")
				if (islog) then
					print("\n")
				end
			end
		end
	end

	if (form == fm_json) then
		out.print(json_tail)
		if (islog) then
			print(json_tail)
		end
	end
end

#make add_fb.json
if (form == fm_json) then
	hash = { "agentHandler" => { "logAgentsOfIndividualPedestrians" => { "tags" => ["/^INDEXA/"], "exclusion" => false } },
			 "generation" => { "rule" => fb_rule, "conditions" => nil, "speedModel" => fb_speedModel, "_" => nil },
			 "agent" => { "className" => fb_className,  "weight" => -0.0, "trail" => 0.0, "margin" => 0.0 } }
	File.open(fbfilename, 'w') do |outfb|
		outfb.puts JSON.pretty_generate(hash)
	end
end

print("# finished %s !\n"%[me])
