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

## default
infilename = "input.csv"
outfilename = "output.csv"
timecol = 0
starttime  = settime("19:00:00")
#endtime  = settime("23:59:00")
endtime  = settime("24:00:00")
intervaltime = 60.0

sw_start = "START"
sw_close = "ADD_STOP"
sw_open  = "REMOVE_STOP"
sw_comme = "#"

help = "### Seanrio csv file maker from 0:close, 1:open csv file.\n" +
 "$ ruby make_scen.rb [option -[foset]] [setting value]\n" +
 " -f [in filename]  : Set input csv data File name.\n" +
 " -o [out filename] : Set Output scenario file name.\n" +
 " -s [time]         : Set Start time; format 00:00:00 default %s.\n"%[strtime(starttime)] +
 " -e [time]         : Set End time; format 00:00:00 default %s.\n"%[strtime(endtime)] +
 " -t [flow sec]     : Set Time intervals (sec or 00:00:00) default %f sec.\n"%[intervaltime]

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
		infilename = val
	when "o" then
		outfilename = val
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
	when "t" then
		if (val =~ /\A[0-9.]*\z/) then
			intervaltime = val.to_f
		elsif (val.include?(":")) then
			intervaltime = sectime(val)
		else
			print(help)
			exit(1)
		end
	else
		print(help)
		exit(1)
	end
end
if (!File.exist?(infilename)) then
	print(help)
	exit(1)
end

print("Make scenario file %s from %s !\n"%[outfilename, infilename])
src_headers, *src = CSV.read(infilename)

numgates  = src[0].size - 1
weregates = Array.new(numgates)
outcline  = Array.new(7)

isend = false
cnt = 1
for gno in 0..numgates-1
	weregates[gno] = 1
end
CSV.open(outfilename, 'w') do |out|
	outcline = [cnt, 0, sw_start, nil, strtime(starttime), nil, sw_comme + "start"]
	out.puts outcline
	print("%d, %d, %s, , %s,  %s\n"%[cnt, 0, sw_start, strtime(starttime), sw_comme + "start"])
	cnt += 1
	for gno in 0..numgates-1
		print("%d, %d, %s, %s, %s, ,  %s\n"%[cnt, 1, src_headers[gno+1], sw_open, strtime(starttime), sw_comme + "ini_" + src_headers[gno+1] + "x"])
		outcline = [cnt, 1, src_headers[gno+1], sw_open, strtime(starttime), sw_comme + "ini_" + src_headers[gno+1] + "o"]
		out.puts outcline
		cnt += 1
		weregates[gno] = 1
	end

	endopen = [strtime(endtime)]
	for i in 1..numgates
		endopen.push("1")
	end
	src.push(endopen)

	src.each do |aregates|
		attime = aregates[0]
		if (settime(attime) < starttime) then
			next
		end
		if (settime(attime) >= endtime) then
			isend = true
		end
		for gno in 0..numgates-1
			isgate = aregates[gno+1].to_i
			if (isend) then
				isgate = 1
			end
			print("%d, %d, %s, %s, "%[cnt, 1, src_headers[gno+1], attime])
			if (isgate == 1 && weregates[gno] != isgate) then
				print("%s, , %s"%[sw_open, sw_comme + src_headers[gno+1] + "o"])
				outcline = [cnt, 1, src_headers[gno+1], sw_open, attime, nil, sw_comme + src_headers[gno+1] + "o"]
				out.puts outcline
				cnt += 1
			elsif (isgate == 0 && weregates[gno] != isgate) then
				print("%s, , %s"%[sw_close, sw_comme + src_headers[gno+1] + "x"])
				outcline = [cnt, 1, src_headers[gno+1], sw_close, attime, nil, sw_comme + src_headers[gno+1] + "x"]
				out.puts outcline
				cnt += 1
			end
			print(" : %d\n"%[isgate])
			weregates[gno] = isgate
		end
		if (isend) then
			break
		end
	end
end

print("# finished %s !\n"%[me])
