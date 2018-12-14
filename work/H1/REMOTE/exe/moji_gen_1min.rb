#! ruby -Ks
# -*- coding: utf-8 -*-

require "csv"
require "complex"

def settime(settimestr)
	strAry = settimestr.split(":")
	if (strAry[0].to_i < 24) then
		timecase =  Time.new(2016, 8, 13, strAry[0].to_i, strAry[1].to_i, strAry[2].to_i, "+09:00")
	else
		timecase =  Time.new(2016, 8, 14, strAry[0].to_i-24, strAry[1].to_i, strAry[2].to_i, "+09:00")
	end
	return timecase
end

def addtime(timecase, addtimestr)
	strAry = addtimestr.split(":")
	timecase += strAry[0].to_f * 60.0*60.0 + strAry[1].to_f * 60.0 + strAry[2].to_f
	return timecase
end

def subtime(timecase, subtimestr)
	strAry = subtimestr.split(":")
	timecase -= strAry[0].to_f * 60.0*60.0 + strAry[1].to_f * 60.0 + strAry[2].to_f
	return timecase
end

def strtime(timecase)
	timestr = timecase.strftime("%H:%M:%S")
	if (timecase.mday > 13) then
		timestr = "%02d:%02d:%02d"%[timecase.hour+(timecase.mday-13)*24, timecase.min, timecase.sec]
	end
	return timestr
end

def strtime_pri(timecase, istime)
	if (istime) then
		return timecase.strftime("%H:%M:%S")
	else
		return timecase.to_s
	end
end

def f2strtime(timecase_flort)
	timestr = (Time.at(timecase_flort)).strftime("%H:%M:%S")
	return timestr
end

def f2strtime_pri(timecase_flort, istime)
	if (istime) then
		return (Time.at(timecase_flort)).strftime("%H:%M:%S")
	else
		return timecase_flort.to_s
	end
end

def sectime(timestr)
	strAry = timestr.split(":")
	timesec = strAry[0].to_f * 60.0*60.0 + strAry[1].to_f * 60.0 + strAry[2].to_f
	return timesec
end

def cumulative_dist(x, s)
	sum = 0.5*(1.0 + Math.erf(x / Math.sqrt(2.0*s*s)))
	return sum
end

def roundoff_sec(timecase)
	if (timecase.sec > 29) then
		timecase += (60 - timecase.sec)
	else
		timecase -= timecase.sec
	end
	return timecase
end

def roundingoff_sec(timecase)
	timecase -= timecase.sec
end

## default
rlen = 3
ctriptfilename = "arraival_triptime.csv"
cflowfilename  = "real_arraival_agent_1min.csv"
cratefilename  = "gen_rate.csv"
outfilename = "output.csv"
starttime   = settime("19:00:00")
endtime     = settime("23:59:00")
sigma = 2.5

intervaltime = 60.0
istime = false
whendup = 0
islog = false

jrnew = Array.new(rlen)
for i in 0..rlen-1
    jrnew[i] = -1.0
end

help = "### Generator to assign startTime-numberOfAgent.\n" +
 "$ ruby moji_gen_1min.rb [option -[tarose]] [setting value]\n" +
 " -t [in filename]  : Set arraivaltime-Triptime data file name; default: %s.\n"%[ctriptfilename] +
 " -a [in filename]  : Set number of observed arraival Agents data file name; default: %s.\n"%[cflowfilename] +
 " -r [in filename]  : Set each routes Rate of arraival agents data file name; default: %s.\n"%[cratefilename] +
 " -o [out filename] : Set Output data file name; default: %s.\n"%[outfilename] +
 " -s [time]         : Set Start time; format 00:00:00 ; default: %s.\n"%[strtime(starttime)] +
 " -e [time]         : Set End time; format 00:00:00 ; default: %s.\n"%[strtime(endtime)] +
 " -l [true or 1]    : set true or 1 then output Log; default: false.\n"

if (ARGV.length == 0 || ARGV.length.odd?) then
	print(help)
	exit(1)
end

me = $0
print("# %s\n"%[me])

opttyp = Array.new(ARGV.length/2)
optval = Array.new(ARGV.length/2)

iop = 0
ARGV.each.with_index do |arg, idx|
	if (idx.even?) then
		if (arg[0, 1] == "-") then
			opttyp[iop] = arg[1, 1]
		else 
			print("! parameter is wrong !\n")
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
	when "t" then
		ctriptfilename = val
	when "a" then
		cflowfilename = val
	when "r" then
		cratefilename = val
	when "o" then
		outfilename = val
	when "x" then
		if (val =~ /\A[0-9]*\z/) then
			timecol = val.to_i
		else
			print(help)
			exit(1)
		end
	when "s" then
		if (val.include?(":")) then
			starttime = settime(val)
		else
			print(help)
			exit(1)
		end
	when "e" then
		if (val.include?(":")) then
			endtime = settime(val)
		else
			print(help)
			exit(1)
		end
	when "l" then
		if (val == "1" || val.downcase == "true") then
			islog =true
		end
	when "1" then
		if (val =~ /\A[0-9.]*\z/) then
			jrnew[0] = val.to_f
		end
	when "2" then
		if (val =~ /\A[0-9.]*\z/) then
			jrnew[1] = val.to_f
		end
	else
		print("! parameter option is wrong !\n")
		print(help)
		exit(1)
	end
end
if (!File.exist?(ctriptfilename)) then
	print("! input file is nothing: %s !\n"%[ctriptfilename])
	print(help)
	exit(1)
end
if (!File.exist?(cflowfilename)) then
	print("! input file is nothing: %s !\n"%[cflowfilename])
	print(help)
	exit(1)
end
if (!File.exist?(cratefilename) && (jrnew[0] < 0.0)) then
	print("! input file is nothing: %s !\n"%[cratefilename])
	print(help)
	exit(1)
end
if (jrnew[0] >= 0.0 && jrnew[1] >= 0.0) then
	if (jrnew[0] <= 1.0 && jrnew[1] <= 1.0) then
		j2r = jrnew[1]
		jrnew[1] = (1.0 - jrnew[0]) * j2r
		jrnew[2] = (1.0 - jrnew[0]) * (1.0 - j2r)
		jrnewf = File.open(cratefilename, "w")
		jrnewf.print("#time,popu_rate_r1,popu_rate_r2,popu_rate_r3\n")
		jrnewf.print("%s,%f,%f,%f\n"%[strtime(starttime), jrnew[0], jrnew[1], jrnew[2]])
		jrnewf.close
	else
		print("! option -1 = %f, -2 = %f settings are wrong, set 0.0~1.0 !\n"%[jrnew[0], jrnew[2]])
		print(help)
		exit(1)
	end
end

print("Assign agents with start Time, from %s, %s to %s !\n"%[ctriptfilename, cflowfilename, cratefilename])
ctript_headers, *ctript = CSV.read(ctriptfilename) #arraival_triptime.csv
cflow_headers,  *cflow  = CSV.read(cflowfilename)  #real_arraival_agent_1min.csv
crate_headers,  *crate  = CSV.read(cratefilename)  #gen_rate.csv
trlen = ctript.length

#calc in
depat = Array.new(trlen) { Array.new(rlen+1) }
depaf = Array.new(trlen) { Array.new(rlen+1) }
depar = Array.new(trlen) { Array.new(rlen+1) }

#out
nlen = ((endtime - starttime).to_f / intervaltime).round(10).to_i + 1
newt = Array.new(nlen)
gen  = Array.new(nlen) { Array.new(rlen+1) }

for i in 0..nlen-1
	if (i == 0) then
		newt[i] = starttime
	elsif (i == nlen-1) then
		newt[i] = endtime
	else
		newt[i] = newt[i-1] + intervaltime
	end
	for j in 0..rlen
		gen[i][j] = 0.0
	end
end

for k in 0..trlen-1
	#set deperture time
	for j in 1..rlen
		depat[k][j] = settime(ctript[k][0]) - sectime(ctript[k][j])
		depat[k][j] = roundoff_sec(depat[k][j])
	end
	depat[k][0] = settime(ctript[k][0])

	findt = ctript[k][0]
	#serch flow
	picupf = cflow.select{ |acflow| acflow[0] == findt }
	if (picupf.length == 0) then
		print("! Not found data at %s in %s !\n"%[strtime(findt), cflowfilename])
		exit(1)
	elsif (picupf.length > 1) then
		print("! WARNNING overlapping data at %s in %s !\n"%[strtime(findt), cflowfilename])
	end
	depaf[0][k] = picupf.last[1].to_f

	#serch rate
	picupr = crate.select{ |acrate| acrate[0] <= findt }
	if (picupf.length == 0) then
		print("! Not found data at %s in %s !\n"%[strtime(findt), cratefilename])
		exit(1)
	end
	for j in 1..rlen
		depar[k][j] = picupr.last[j].to_f
	end

	#calc each flow
	for j in 1..rlen
		depaf[k][j] = depaf[0][k] * depar[k][j]
	end

	if (islog) then
		print("%d, %s, [%s, %s, %s], %d, [%f, %f, %f], [%f, %f, %f]\n"%[k, findt, strtime(depat[k][1]),  strtime(depat[k][2]),  strtime(depat[k][3]), depaf[0][k].to_i, depar[k][1], depar[k][2], depar[k][3], depaf[k][1], depaf[k][2], depaf[k][3]])
	end
end

for i in 0..nlen-1
	totgen = 0.0
	for j in 1..rlen
		#serch
		findt = newt[i]
		picupt = depat.select{ |adepat| adepat[j] == findt }
#		p picupt
		if (picupt.length == 0) then
#			print("! Not found data at %s in arraival_triptime.csv !\n"%[strtime(findt)])
			next
		end
		for h in 0..picupt.length-1
			idx = depat.index(picupt[h])
			gen[i][j] += depaf[idx][j]
			if (islog) then
				print("%d, %d, %d, %f, %f\n"%[i, j, idx, depaf[idx][j], gen[i][j]])
			end
		end
		totgen += gen[i][j]
	end
	gen[i][0] = totgen
end


CSV.open(outfilename, 'w') do |out|
	ailne = Array["#No", "time", "gen_agent", "r1_gen_agent", "r2_gen_agent", "r3_gen_agent"]
	out.puts ailne
	for i in 0..nlen-1
		if (islog) then
			print("%4d, %s, "%[i, strtime(newt[i])])
		end
		ailne = Array.new
		ailne.push(i)
		ailne.push(strtime(newt[i]))
		ailne.push(gen[i][0])
		for j in 1..rlen
			ailne.push(gen[i][j])
		end
		out.puts ailne
		if (islog) then
			print(gen[i],"\n")
		end
	end
end

print("# finished %s !\n"%[me])
