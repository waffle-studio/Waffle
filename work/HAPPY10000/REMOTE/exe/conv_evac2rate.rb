#! ruby -Ks
# -*- coding: utf-8 -*-

require "csv"

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

help = "### Sum every 60 row(sec) and set time\n" +
    "ruby conv_evac2rate.rb [original input file] [summing output file] [start time; hh:mm:ss] [end time; hh:mm:ss]\n" +
	"ex) ruby conv_evac2rate.rb evac_cyc0.csv gen_rate.csv 19:00:00 23:59:00\n"

eve = 60

me = $0
print("# %s\n"%[me])

if (ARGV.length < 4) then
	print(help)
	exit(1)
end
infile = ARGV[0]
outfile = ARGV[1]
startt = settime(ARGV[2])
endt = settime(ARGV[3])

header, *src = CSV.read(infile)
rlen = src[0].size

count = Array.new{ Array.new(rlen) }

attime = startt
lastsrc = Array.new(rlen, 0.0)
src.each.with_index do |asrc, i|
	if (((i+1) % eve) == 0) then
		cntl = Array.new
		cntl.push(attime)
		attime += eve.to_f
		diff = Array.new(rlen, 0.0)
		for j in 0..rlen-1
			diff[j] = asrc[j].to_f - lastsrc[j]
			cntl.push(diff[j])
			lastsrc[j] = asrc[j].to_f
		end
		count.push(cntl)
		print(strtime(cntl[0]), ", ", cntl[1..-1], "\n")
	end
end

outf = CSV.open(outfile, 'w')
outl = Array.new
outl.push("#time")
for j in 0..rlen-1
	outl.push("popu_rate_r%d"%[j+1])
end
outf.puts(outl)
count.each.with_index do |acount, i|
	if (acount[0] > endt) then
		break
	end
	outl = Array.new
	outl.push(strtime(acount[0]))
	rsum = 0.0
	for j in 0..rlen-1
		rsum += acount[j+1]
	end
	for j in 0..rlen-1
		if (rsum != 0.0) then
			outl.push(acount[j+1]/rsum)
		else
			outl.push(1.0/rlen.to_f)
		end
	end
	outf.puts(outl)
end
outf.close

print("# finished %s !\n"%[me])

