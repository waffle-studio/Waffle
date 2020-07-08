#! ruby -Ks
# -*- coding: utf-8 -*-

require "csv"
require "json"
require "complex"

## default
infilename = "input.csv"
outfilename = "output_scen.json"

help = "### Convert csv scenario file to json scenario file.\n" +
 "$ ruby conv_scen_csv2json.rb [option -[fo]] [setting value]\n" +
 " -f [in filename]  : Set input csv scenario File name, def: %s .\n"%[infilename] +
 " -o [out filename] : Set Output json scenario file name, def: %s .\n"%[outfilename]

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
	else
		print(help)
		exit(1)
	end
end
if (!File.exist?(infilename)) then
	print(help)
	exit(1)
end

print("Convert csv scenario file %s to json file %s !\n"%[infilename, outfilename])
src = CSV.read(infilename)
src_len = src.length

#start Jx REMOVE_STOP
is1stj = "J#"
jlist = Array.new
starttime = ""

File.open(outfilename, 'w') do |out|
	out.print("[\n")
	print("[\n")

	src.each.with_index do |csvline, idx|
		attime = csvline[4]
		tag    = csvline[2].upcase
		order  = csvline[3]
		coment = csvline[6]

		case tag[0, 1]
		when "S" then
			if (tag == "START") then
				hash = { "atTime" => attime, "type" => "Initiate" }
				outjline = "  " + JSON.dump(hash)
				out.print(outjline)
				print(outjline)
				starttime = attime
			else
				print("! csv file is wrong, Not START => [%s] !\n"%csvline)
				exit(1)
			end
		when "J" then
			if (is1stj != tag && !jlist.include?(tag)) then
				#start Jx REMOVE_STOP
				src.push([nil,nil,tag,"REMOVE_STOP",starttime,nil,nil])
				src_len = src.length
				is1stj = tag
				jlist.push(tag)
			end
			type_message = "Alert"
			if (order == "ADD_STOP") then
				add_offmess = "_open"
				add_message = "_closed"
				type_gate ="CloseGate"
			elsif (order == "REMOVE_STOP") then
				add_offmess = "_closed"
				add_message = "_open"
				type_gate ="OpenGate"
			else
				print("! csv file is wrong, Not ~_STOP J => [%s] !\n"%csvline)
				exit(1)
			end
			hash = { "atTime" => attime, "type" => type_message, "placeTag" => "FL_"+tag, "message" => tag+add_offmess, "onoff" => "false" }
			outjline = "  " + JSON.dump(hash) + ",\n"
			out.print(outjline)
			print(outjline)
			hash = { "atTime" => attime, "type" => type_message, "placeTag" => "FL_"+tag, "message" => tag+add_message }
			outjline = "  " + JSON.dump(hash) + ",\n"
			out.print(outjline)
			print(outjline)
			hash = { "atTime" => attime, "type" => type_gate, "placeTag" => tag }
			outjline = "  " + JSON.dump(hash)
			out.print(outjline)
			print(outjline)
		when "G" then
			if (order == "ADD_STOP") then
				type_gate ="CloseGate"
			elsif (order == "REMOVE_STOP") then
				type_gate ="OpenGate"
			else
				print("! csv file is wrong, Not ~_STOP G => [%s] !\n"%csvline)
				exit(1)
			end
			hash = { "atTime" => attime, "type" => type_gate, "placeTag" => tag }
			outjline = "  " + JSON.dump(hash)
			out.print(outjline)
			print(outjline)
		else
			print("! csv file is wrong, Not J~ G~ S~ => [%s] !\n"%csvline)
			exit(1)
		end
		if (idx == src_len-1) then
			out.print("\n")
			print("\n")
		else
			out.print(",\n")
			print(",\n")
		end
	end
	out.print("]\n")
	print("]\n")
end

print("# finished %s !\n"%[me])
