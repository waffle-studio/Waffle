#! ruby -Ks
# -*- coding: utf-8 -*-

require "csv"

def settime(settimestr)
	strAry = settimestr.split(":")
	timecase =  Time.new(2016, 8, 13, strAry[0].to_i, strAry[1].to_i, strAry[2].to_i, "+09:00")
	return timecase
end

def addtime(timecase, addtimesec)
	timecase += addtimesec.to_f
	return timecase
end

infile = 'evacuatedAgent.csv'
starttime = "19:00:00"

outfile = 'goalagent.csv'
interval = 30

ARGV.each_with_index do |arg, i|
	case i
	when 0 then
		if (arg == "-h") then
			print("ruby check_evacuatedAgent.rb [infile] [outfile] [start time; hh:mm:ss] [interval; sec]\n")
			exit
		end
		infile = arg
	when 1 then
		outfile = arg
	when 2 then
		starttime = arg
	when 3 then
		interval = arg.to_i
	end
end

headers, *src = CSV.read(infile, "r")
rlen = src[0].size

print("run check_evacuatedAgent.rb for ", infile, ", output ", outfile, " : ", src.length)

out = CSV.open(outfile, "wb")

t = settime(starttime)
val = Array.new(rlen)
last_val = Array.new(rlen)
dif = Array.new(rlen)
head = Array.new
if (rlen > 1) then
	head.push("#date")
	for i in 0..rlen-1
		head.push("r%d_diff"%[i+1])
		last_val[i] = 0
	end
	head.push("diff")
else
	head = ["#date", "diff"]
	last_val[0] = 0
end
out.puts head

for i in 0..src.length-1
	if (i % interval) == 0 then
		line = Array.new
		date = t.strftime("%Y/%m/%d %H:%M:%S")
		line.push(date)
		sum = 0
		for j in 0..rlen-1
			val[j] = src[i][j].to_i
			dif[j] = val[j] - last_val[j]
			line.push(dif[j])
			sum += dif[j]
			last_val[j] = val[j]
		end
		if (rlen > 1) then
			line.push(sum)
		end
		out.puts line
		t = addtime(t, interval)
	end
end

out.close

print(" end\n")
STDERR.print(rlen)
