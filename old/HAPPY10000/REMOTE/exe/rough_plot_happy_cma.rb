#! ruby

require "open3"

# check OS
if ( RUBY_PLATFORM.downcase =~ /mswin(?!ce)|mingw|cygwin|bccwin/ ) then
    os = "win"
else
    os = "linux"
end

exepath = ARGV[0]
anapath = ARGV[1]
#flowpath = ARGV[2]
scenpath = ARGV[2]
if (os == "win") then
    exid = ARGV[3]
    chk = ARGV[4]
else
    cycno = ARGV[3]
    testno = ARGV[4]
    chk = ARGV[5]
end

me = $0
print("# %s\n"%[me])

# make goalagent_raw.csv
isanaagent = true
if ((chk == "noana") || (chk == "eva") || (chk == "false")) then
    isanaagent = false
end
infile = anapath + "analyzeAgent.csv"
if ((! isanaagent) || (! File.exist?(infile))) then
    infile = anapath + "evacuatedAgentO.csv"
    #infile = flowpath + "evacuatedAgent.csv"
end
o, e, s = Open3.capture3("ruby " + exepath + "check_evacuatedAgent.rb " + infile)
p o, e, s
o, e, s = Open3.capture3("cp goalagent.csv " + anapath + "goalagent_raw.csv")

# make goalagent_raw.png
o, e, s = Open3.capture3("cp " + scenpath + "moji_20160813_1900-.csv goalagent_real.csv")
titlename = "Flow (raw)" # data), Number of return station agents"
if (os == "win") then
    o, e, s = Open3.capture3("cp " + exepath + "goalagent.gnp .")
    o, e, s = Open3.capture3("gnuplot -e \"title_name='" + titlename + "'\" goalagent.gnp")
else
    o, e, s = Open3.capture3("gnuplot -e \"title_name='" + titlename + "'\" " + exepath + "goalagent.gnp")
end
p o, e, s
o, e, s = Open3.capture3("mv goalagent.png goalagent_raw.png")
o, e, s = Open3.capture3("cp goalagent_raw.png " + anapath)
 
# make goalagent_filt.csv
o, e, s = Open3.capture3("mv goalagent.csv filt0.csv")
o, e, s = Open3.capture3("ruby " + exepath + "filter.rb -f filt0.csv -c 1 -s 1.5 -w 4 -o filt1.csv")
p o, e, s
o, e, s = Open3.capture3("ruby " + exepath + "filter.rb -f filt1.csv -c 2 -s 1.5 -w 4 -o filt0.csv")
o, e, s = Open3.capture3("ruby " + exepath + "filter.rb -f filt0.csv -c 3 -s 1.5 -w 4 -o filt1.csv")
o, e, s = Open3.capture3("ruby " + exepath + "filter.rb -f filt1.csv -c 4 -s 1.5 -w 4 -o goalagent.csv")
o, e, s = Open3.capture3("cp goalagent.csv " + anapath + "goalagent_filt.csv")

# make goalagent_filt.png
titlename = "Flow (filtered)" # data), Number of return station agents"
if (os == "win") then
    o, e, s = Open3.capture3("gnuplot -e \"title_name='" + titlename + "'\" goalagent.gnp")
else
    o, e, s = Open3.capture3("gnuplot -e \"title_name='" + titlename + "'\" " + exepath + "goalagent.gnp")
end
p o, e, s
o, e, s = Open3.capture3("mv goalagent.png goalagent_filt.png")
o, e, s = Open3.capture3("cp goalagent_filt.png " + anapath)

## make result_bar.png
#o, e, s = Open3.capture3("cp " + flowpath + "Result.csv .")
#o, e, s = Open3.capture3("cp " + flowpath + "Error.csv .")
#if (os == "win") then
#    o, e, s = Open3.capture3("cp " + exepath + "result_bar.gnp .")
#    titlename = "exid: " + exid
#    o, e, s = Open3.capture3("gnuplot -e \"title_name='" + titlename + "'\" result_bar.gnp")
#else
#    titlename = "cycle: " + cycno + ", test: " + testno
#    o, e, s = Open3.capture3("gnuplot -e \"title_name='" + titlename + "'\" " + exepath + "result_bar.gnp")
#end
#p o, e, s
#o, e, s = Open3.capture3("convert result_bar.png -crop 150x480+0+0 result_bar.png")
#o, e, s = Open3.capture3("cp result_bar.png " + anapath)

# make move_line.png
titlename = "Line (average)" # data), Movement distance of agents every 15 minutes"
if (os == "win") then
    o, e, s = Open3.capture3("cp " + exepath + "move_line.gnp .")
    o, e, s = Open3.capture3("gnuplot -e \"title_name='" + titlename + "'; ana_dir='" + exid + "/ana/'\" move_line.gnp")
    #o, e, s = Open3.capture3("gnuplot -e \"title_name='" + titlename + "'; ana_dir='ana/'\" move_line.gnp")
    #o, e, s = Open3.capture3("gnuplot -e \"title_name='" + titlename + "'; ana_dir='" + anapath + "'\" move_line.gnp")
else
    o, e, s = Open3.capture3("gnuplot -e \"title_name='" + titlename + "'; ana_dir='" + anapath + "'\" " + exepath + "move_line.gnp")
end
p o, e, s
o, e, s = Open3.capture3("cp move_line.png " + anapath)

# make goalagente.png
o, e, s = Open3.capture3("convert -append goalagent_filt.png move_line.png goalagent_raw.png goalagent_w.png")
p o, e, s
if (os == "win") then
    goalpngname = "goalagent_" + exid + ".png"
else
    goalpngname = "goalagent_c" + cycno + "-t" + testno + ".png"
end
p o, e, s
o, e, s = Open3.capture3("cp goalagent_w.png " + goalpngname)
#o, e, s = Open3.capture3("convert +append result_bar.png goalagent_w.png " + goalpngname)
#o, e, s = Open3.capture3("cp " + goalpngname + " " + flowpath)
o, e, s = Open3.capture3("cp " + goalpngname + " " + anapath)

# make happy_process.png
o, e, s = Open3.capture3("cp " + anapath + "Separate_Ave_Var.csv .")
titlename = "Walking Time (Ave, Max-Min)"
if (os == "win") then
    o, e, s = Open3.capture3("cp " + exepath + "happy_process.gnp .")
    o, e, s = Open3.capture3("gnuplot -e \"title_name='" + titlename + "'\" happy_process.gnp")
else
    o, e, s = Open3.capture3("gnuplot -e \"title_name='" + titlename + "'\" " + exepath + "happy_process.gnp")
end
p o, e, s
o, e, s = Open3.capture3("cp happy_process.gnp " + anapath)

# make happy_bar.png
o, e, s = Open3.capture3("cp " + anapath + "Happy.csv .")
o, e, s = Open3.capture3("cp " + anapath + "???_Ave_Var.csv .")
if (os == "win") then
    o, e, s = Open3.capture3("cp " + exepath + "happy_bar.gnp .")
    titlename = "exid: " + exid
    o, e, s = Open3.capture3("gnuplot -e \"title_name='" + titlename + "'\" happy_bar.gnp")
else
    titlename = "cycle: " + cycno + ", test: " + testno
    o, e, s = Open3.capture3("gnuplot -e \"title_name='" + titlename + "'\" " + exepath + "happy_bar.gnp")
end
p o, e, s
o, e, s = Open3.capture3("convert happy_bar.png -crop 150x480+0+0 happy_bar.png")
o, e, s = Open3.capture3("cp happy_bar.png " + anapath)

# make happy.png
if (os == "win") then
    happypngname = "happy_" + exid + ".png"
else
    happypngname = "happy_c" + cycno + "-t" + testno + ".png"
end
o, e, s = Open3.capture3("convert +append happy_bar.png happy_process.png " + happypngname)
p o, e, s
#o, e, s = Open3.capture3("cp " + happypngname +" " + flowpath)
o, e, s = Open3.capture3("cp " + happypngname +" " + anapath)

# finishing
o, e, s = Open3.capture3("rm -f filt?.csv goalagent_real.csv goalagent_w.png")
if (os == "win") then
    o, e, s = Open3.capture3("rm -f *.gnp")
end

print("# finished %s !\n"%[me])
exit 0
