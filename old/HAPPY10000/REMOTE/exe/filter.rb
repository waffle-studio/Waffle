#! ruby -Ks
# -*- coding: utf-8 -*-

require "csv"
require "complex"

def settime(settimestr)
	strAry = settimestr.split(":")
	timecase =  Time.new(2016, 8, 13, strAry[0].to_i, strAry[1].to_i, strAry[2].to_i, "+09:00")
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

def cumulative_dist(x, s)
	sum = 0.5*(1.0 + Math.erf(x / Math.sqrt(2.0*s*s)))
	return sum
end

## default
infilename = "input.csv"
outfilename = "output.csv"
objcol = 1
sigma = 2.5
nd_width = (sigma * 2 + 1).round(10).to_i
#nd_width = 6
istime = false
islog = false

help = "### Filter to smooth with summing normal distribution.\n" +
 "$ ruby filter.rb [option -[focsw]] [setting value]\n" +
 " -f [in filename]  : Set input data file name to convert.\n" +
 " -o [out filename] : Set output data file name that was converted.\n" +
 " -c [int column]   : Set column number(0 start) of data to covert.\n" +
 " -s [real sigma]   : Set sigma of normal distribution with number of rows, default: %f.\n"%[sigma] +
 " -w [int row]      : Set influence range of summing with number of rows(one side),\n" +
 "                     default: 2*sigma + 1 = %d.\n"%[nd_width] +
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
	when "c" then
		if (val =~ /\A[0-9]*\z/) then
			objcol = val.to_i
		else
			print(help)
			exit(1)
		end
	when "s" then
		if (val =~ /\A[0-9.]*\z/) then
			sigma = val.to_f
		else
			print(help)
			exit(1)
		end
	when "w" then
		if (val =~ /\A[0-9]*\z/) then
			nd_width = val.to_i
		else
			print(help)
			exit(1)
		end
	when "l" then
		if (val == "1" || val.downcase == "true") then
			islog =true
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
nd_width = (sigma * 2 + 1).round(10).to_i

normal_dist = Array.new(nd_width+1)
for i in 0..nd_width
	if (i < nd_width) then
		normal_dist[i] = cumulative_dist(i.to_f+0.5, sigma) - cumulative_dist(i.to_f-0.5, sigma)
	else
		normal_dist[i] = 1 - cumulative_dist(i.to_f-0.5, sigma)
	end
	#print(i, ", ", normal_dist[i], "\n")
end

print("Convert filtering %s to %s !\n"%[infilename, outfilename])
#p infilename
src_headers, *src = CSV.read(infilename)
slen = src.length

if (src[0][objcol].include?(":")) then
	istime = true
end
vals = Array.new(slen)
cnvv = Array.new(slen)
for i in 0..slen-1
	if (istime) then
		vals[i] = settime(src[i][objcol])
	else
		vals[i] = src[i][objcol].to_f
	end
	cnvv[i] = 0.0
end


for i in 0..slen-1
	for m in -nd_width..nd_width
		d = i + m
		if (d >= 0 && d < slen) then
			p = d
		elsif (d < 0) then
			p = d.abs - 1
		elsif (d >= slen) then
			p = (slen-1) - (d - (slen-1)) + 1
		end
		#print("%3d, %3d, %3d, %d\n"%[i, d, m, p])
		cnvv[p] += vals[i].to_f * normal_dist[m.abs]
	end
end

ssiz = src[0].size
csvl = Array.new(ssiz)
CSV.open(outfilename, 'w') do |out|

	out.puts src_headers
	for i in 0..slen-1
		for j in 0..ssiz-1
			if (j == objcol) then
				if (istime) then
					csvl[j] = strtime(Time.at(cnvv[i]))
				else
					csvl[j] = cnvv[i]
				end
			else
				csvl[j] = src[i][j]
			end
		end
		out.puts csvl
		if (islog) then
			if (istime) then
				print("%d, %s, %s\n"%[i, strtime(vals[i]), csvl[objcol]])
			else
				print("%d, %f, %f\n"%[i, vals[i], cnvv[i]])
			end
		end
	end

end

print("# finished %s !\n"%[me])
