import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Scanner;

public class CMA_Analyze_map24 {
	Scanner sc = new Scanner(System.in);
	//static String Flowpos = "";
	static String Simpos = "";
	static String Anapos = "";
	static String Rankpos = "";
	static String Pypos = "";
	public static void main(String[] args){
		//Flowpos = args[0];
		Simpos = args[0];
		Anapos = args[1];
		Rankpos = args[2];
		Pypos = args[3];
		new CMA_Analyze_map24();
	}
	public CMA_Analyze_map24(){
		new Test1().doIt();
	}
	class Test1{

		int T2_time[] = new int[20000];
		int pos[] = new int[50000];
		int course[] = new int[50000];
		int route[] = new int[50000];
		int ST[][] = new int[4][400];
		int ST_Time[][] = new int[4][400];
		
		//----------------------
		double Diff_Time[][] = new double[4][400];
		int Rflow[][] = new int[4][1000];
		int Min_Rtime_old[] = {0,250,500,700};
		//----------------------
		
		//--------------------(New)
		double Min_Rtime[] = {0.0,330.0,580.0,758.0};
		int Min_Rtime_Sep[][] = {{0,0,0,0},{0,96,140,94},{0,512,68,0},{0,608,150,0}};
		int pos_time[][] = new int[50000][4];
		int ST_Time_Sep[][][] = new int[4][4][400];
		double Diff_Time_pow[][] = new double[4][400];
		//--------------------

		int Separate_Time[] = {1900,1915,1930,1945,2000,2015,2030,2045,2100,2115,2130,2145,2200,2215,2230};
		double Separate_Route[] = {0.0,323.0,575.0,765.0};
		//double Separate_Route[] = {0.0,320.0,600.0,770.0};
		boolean Set = false;
		
		int sep[] = {190000,191500,193000,194500,200000,201500,203000,204500,210000,
				211500,213000,214500,220000,221500,223000};
		int MaxRoute[] = {0,323,575,765};
		//int MaxRoute[] = {0,320,600,770};
		void doIt(){
			//Directory
			int num = Integer.parseInt(sc.next());
			int island = 0;
			//int island = Integer.parseInt(sc.next());
			int roop = Integer.parseInt(sc.next());
			System.out.println(Simpos+"\n "+Anapos+"\n "+Rankpos+"\n "+Pypos+"\n "+num+" "+roop);

			//Flow
			System.out.println("Flow... ");
			int cnt = 0;
			int T1_time[] = new int[3000];
			int T1_flow[] = new int[3000];
			
			String T1_ss = "Flow_2.csv";
			String T1_ss2 = Simpos+"analyzeAgent.csv";
			//String T1_ss2 = Simpos+"evacuatedAgent.csv";
			String T1_ss3 = Anapos+"evacuatedAgentS.csv";
			String T1_ss4 = Anapos+"evacuatedAgentS2.csv";
			cnt = T1_R_Flow(T1_ss,cnt,T1_flow);
			cnt = T1_S_Flow(T1_ss2,0,0,T1_time);
			
			//----------------------
			T1_outtime_30sec(19,0,0,cnt,T1_time,T1_flow,num,T1_ss3);
			T1_outtime_1min(19,0,0,cnt,num,T1_ss4);
			//----------------------
			
			//Line
			System.out.print("Line... ");
			ArrayList<ArrayList<T2_Par>> Route1 = new ArrayList<ArrayList<T2_Par>>();
			ArrayList<ArrayList<T2_Par>> Route2 = new ArrayList<ArrayList<T2_Par>>();
			ArrayList<ArrayList<T2_Par>> Route3 = new ArrayList<ArrayList<T2_Par>>();
			T2_set(19,0,0);
			try {
				for(int i = 0;i < 400;i++){
					Route1.add(new ArrayList<T2_Par>());
					Route2.add(new ArrayList<T2_Par>());
					Route3.add(new ArrayList<T2_Par>());
				}
				String T2_ss[] = new String[2];
				T2_ss[0] = Simpos+"agent_movement_history.csv";
				//T2_ss[0] = Simpos+"route.csv";
				T2_ss[1] = Simpos+"log_individual_pedestrians.csv";
				for(int i = 0;i < 2;i++){
					String line;
					FileReader fr = new FileReader(T2_ss[i]);
					BufferedReader br = new BufferedReader(fr);
					line = br.readLine();
					if(i == 0){System.out.print(" First Check...");
						while ((line = br.readLine()) != null){
							T2_csvCheck(line);
						}
					}else{System.out.print(" Second Check...");
						while ((line = br.readLine()) != null){
							T2_csvRead(line,Route1,Route2,Route3);
						}
					}
					br.close();
				}
				System.out.println(" Output Route...");
				T2_output(Route1,323,1,num,roop,island);
				T2_output(Route2,575,2,num,roop,island);
				T2_output(Route3,765,3,num,roop,island);
			} catch (IOException ex){
				ex.printStackTrace();
			}
			
			//Average
			System.out.println("Average... ");
			for(int i = 0;i < Separate_Time.length;i++){
				for(int j = 1;j < 4;j++){
					Set = false;
					double h = (int)(Separate_Time[i] / 100);
					double m = (int)(Separate_Time[i] % 100);
					double s = 0;
					
					double Dist[] = new double[20000];
					double Cnt[] = new double[20000];
					for(int k = (i*15) - 7;k <= (i*15) + 7;k++)if(k >= 0)T3_Dist_sum(j,k,Dist,Cnt,num,roop,island);
					if(Set)T3_csv_write(j,i,h,m,s,Dist,Cnt,num,roop,island);
				}
			}

			//Happiness
			System.out.println("Happiness... ");
			Happiness_Par();
			Happiness_Separate(num,roop,island);
/*
			try {
				copyFile(num,roop,island,1);
				copyFile(num,roop,island,2);
			} catch (IOException e) {
				e.printStackTrace();
			}
*/
		}
		void Happiness_Max_All(int pos,int roop,int island,double Average,double Min_Max_Dist,String type){
			try {
				FileWriter fw = new FileWriter(Anapos+type+"_Ave_Var.csv",false);
				PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
				pw.println(type+","+Average+","+Min_Max_Dist);
				pw.close();
			}catch (IOException e){	
			}
		}
		void Happiness_Separate(int pos,int roop,int island){
			int hstart = 1900;   // happy check start time [HHMM]
			int hrange =  300;   // happy check time range [min]
			double Stop_pow = 0;
			double Stop = 0;
			double Max_Average = 0;
			double Max_Min_Max_Dist = 0;
			double Sum_Average = 0;
			double Sum_Min_Max_Dist = 0;
			double pedestrian_flow = 38000;
			try {
				FileWriter fw = new FileWriter(Anapos+"Separate_Ave_Var.csv",false);
				PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
				int time = hstart;
				for(int i = 0;i < hrange;i++){
					double Average = (ST_Time[1][i]+ST_Time[2][i]+ST_Time[3][i])*1.0 / 3;
					double Min_Max_Dist = Math.max(Math.max(ST_Time[1][i],ST_Time[2][i]),ST_Time[3][i]) - Math.min(Math.min(ST_Time[1][i],ST_Time[2][i]),ST_Time[3][i]);
					for(int j = 1;j <= 3;j++){
						if(i == 0 || Rflow[j][i] == 0)continue;
						Diff_Time[j][i] = (ST_Time[j][i] - Min_Rtime[j]) * (Rflow[j][i] - Rflow[j][i - 1]);
						pedestrian_flow = pedestrian_flow - (Rflow[j][i] - Rflow[j][i - 1]);
						//--------------------------
						Diff_Time_pow[j][i] = 0;
						for(int k = 1;k <= 3;k++){
                            if (Min_Rtime_Sep[j][k] < 1.0) continue;
							Diff_Time_pow[j][i] = (ST_Time_Sep[j][k][i] - Min_Rtime_Sep[j][k]) * (ST_Time_Sep[j][k][i] - Min_Rtime_Sep[j][k]) * (Min_Rtime[j] / Min_Rtime_Sep[j][k]*1.0) + Diff_Time_pow[j][i];
						}
						Diff_Time_pow[j][i] = Diff_Time_pow[j][i] / 1000000.0;
						Diff_Time_pow[j][i] = Diff_Time_pow[j][i] * (Rflow[j][i] - Rflow[j][i - 1]);
						Stop_pow = Stop_pow + Diff_Time_pow[j][i];
						//-------------------------
						Stop = Stop + Diff_Time[j][i];
					}
					Sum_Average = Average + Sum_Average;
					Sum_Min_Max_Dist = Min_Max_Dist + Sum_Min_Max_Dist;
					Max_Average = Math.max(Max_Average, Average);
					Max_Min_Max_Dist = Math.max(Max_Min_Max_Dist, Min_Max_Dist);
					
					int timeh = time / 100;
					int timem = time % 100;
					pw.println(timeh+":"+timem+","+ST_Time[1][i]+","+ST_Time[2][i]+","+ST_Time[3][i]+","+Average+","+Min_Max_Dist+","+Diff_Time_pow[1][i]+","+Diff_Time_pow[2][i]+","+Diff_Time_pow[3][i]+","+Rflow[1][i]+","+Rflow[2][i]+","+Rflow[3][i]+","+ST_Time_Sep[1][1][i]+","+ST_Time_Sep[1][2][i]+","+ST_Time_Sep[1][3][i]+","+ST_Time_Sep[2][1][i]+","+ST_Time_Sep[2][2][i]+","+ST_Time_Sep[3][1][i]+","+ST_Time_Sep[3][2][i]);
					//pw.println(timeh+":"+timem+","+ST_Time[1][i]+","+ST_Time[2][i]+","+ST_Time[3][i]+","+Average+","+Min_Max_Dist+","+Diff_Time_pow[1][i]+","+Diff_Time_pow[2][i]+","+Diff_Time_pow[3][i]);
					//pw.println(timeh+":"+timem+","+ST_Time[1][i]+","+ST_Time[2][i]+","+ST_Time[3][i]+","+Average+","+Min_Max_Dist+","+Diff_Time[1][i]+","+Diff_Time[2][i]+","+Diff_Time[3][i]);
					time = time + 1;
					if(time % 100 == 60)time = time + 40;
				}
				if(pedestrian_flow > 0){
					pedestrian_flow = pedestrian_flow / 1000000;
					Stop_pow = Stop_pow + (pedestrian_flow * 18000 * 18000);
				}			
				pw.close();
			}catch (IOException e){	
			}
			Happiness_Max_All(pos,roop,island,Max_Average,Max_Min_Max_Dist,"Max");
			Happiness_Max_All(pos,roop,island,Sum_Average,Sum_Min_Max_Dist,"All");
			Result_Happy(pos,roop,island,Max_Average,Max_Min_Max_Dist,Sum_Average,Sum_Min_Max_Dist,Stop_pow,Stop);
//		  CMA_Opt(pos,roop,island,Max_Average,Max_Min_Max_Dist,Sum_Average,Sum_Min_Max_Dist);
			//CMA_Opt2(pos,roop,island,Max_Average,Max_Min_Max_Dist,Sum_Average,Sum_Min_Max_Dist,Stop_pow,Stop);
		}
		void Happiness_Par(){
			if(ST_Time[0][0] == 0)ST_Time[0][0] = 300;
			if(ST_Time[1][0] == 0)ST_Time[1][0] = 600;
			if(ST_Time[2][0] == 0)ST_Time[2][0] = 900;
			if(ST_Time[3][0] == 0)ST_Time[3][0] = 1200;
//			{{0},{0,96,236,330},{0,512,580},{0,608,758}};
			if(ST_Time_Sep[1][1][0] == 0){
				ST_Time_Sep[1][0][0] = 0;
				ST_Time_Sep[1][1][0] = 96;
				ST_Time_Sep[1][2][0] = 140;
				ST_Time_Sep[1][3][0] = 94;
			}
			if(ST_Time_Sep[2][1][0] == 0){
				ST_Time_Sep[2][0][0] = 0;
				ST_Time_Sep[2][1][0] = 512;
				ST_Time_Sep[2][2][0] = 68;
				ST_Time_Sep[2][3][0] = 0;
			}
			if(ST_Time_Sep[3][1][0] == 0){
				ST_Time_Sep[3][0][0] = 0;
				ST_Time_Sep[3][1][0] = 608;
				ST_Time_Sep[3][2][0] = 150;
				ST_Time_Sep[3][3][0] = 0;
			}
			
			for(int i = 0;i < 4;i++){
				for(int k = 0;k < 4;k++){
					for(int j = 1;j < 400;j++){
						if(ST_Time_Sep[i][k][j] == 0)ST_Time_Sep[i][k][j] = ST_Time_Sep[i][k][j-1];
					}
				}
			}
			for(int i = 0;i < 4;i++){
				for(int j = 1;j < 400;j++){
					if(ST_Time[i][j] == 0)ST_Time[i][j] = ST_Time[i][j-1];
				}
			}
		}
		void copyFile(int num,int roop,int island, int mode) throws IOException {
			try {
				FileInputStream fis;
				FileOutputStream fos;
				switch (mode) {
					case 1:
						fis = new FileInputStream(Rankpos+"CMA_Opt.csv");
						fos = new FileOutputStream(Pypos+"CMA_Opt.csv");
						break;
					case 2:
						fis = new FileInputStream(Simpos+"evacuatedAgent.csv");
						fos = new FileOutputStream(Anapos+"evacuatedAgentO.csv");
						break;
					default:
						System.err.println("Error! copyFile mode="+mode);
						fis = new FileInputStream(Simpos+"evacuatedAgent.csv");
						fos = new FileOutputStream(Anapos+"evacuatedAgentO.csv");
						break;
				}
				byte buf[] = new byte[256];
				int len;
				while ((len = fis.read(buf)) != -1) {
					fos.write(buf, 0, len);
				}
				fos.flush();
				fos.close();
				fis.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		int T1_R_Flow(String ss,int cnt,int flow[]){
			try {
				FileReader fr = new FileReader(ss);
				BufferedReader br = new BufferedReader(fr);
				String line;
				line = br.readLine();
				while ((line = br.readLine()) != null) {
					cnt++;
					flow[cnt] = Integer.parseInt(line);
				}
				br.close();
				return cnt;
			} catch (IOException e) {
				return cnt;
			}
		}	
		int T1_S_Flow(String ss,int cnt,int cc,int T1_time[]){
			try {
				FileReader fr = new FileReader(ss);
				BufferedReader br = new BufferedReader(fr);
				String line;
				line = br.readLine();
				while ((line = br.readLine()) != null) {
					cc++;
					String sss[] = line.split(",");
					if(cc % 30 == 0){
						cnt++;
						if(cnt % 2 == 0){
							Rflow[1][cnt/2] = Integer.parseInt(sss[0]);
							Rflow[2][cnt/2] = Integer.parseInt(sss[1]);
							Rflow[3][cnt/2] = Integer.parseInt(sss[2]);
						}
					}
					T1_time[cnt] = Integer.parseInt(sss[0])+Integer.parseInt(sss[1])+Integer.parseInt(sss[2]);
				}
				br.close();
				return cnt;
			} catch (IOException e) {
				return cnt;
			}
		}
		
		void T1_outtime_30sec(int h,int m,int s,int cnt,int T1_time[],int flow[],int index,String ss){
			try {
				FileWriter fw = new FileWriter(ss, false);
				PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
				for(int i = 1;i < cnt;i++){
					double num = h*100 + m*60*100.0/3600 + ((s % 100) * 100.0)/3600;
					int sum = h*10000 + m*100 + s;
					int error = (flow[i] - (T1_time[i] - T1_time[i-1]));
					int error2 = error * error;
					pw.printf("%d,%f,%d,%d,%d\n",sum,num,(T1_time[i]-T1_time[i - 1]),error,error2);
					s = s + 30;
					m = m + (s / 60);s = s % 60;
					h = h + (m / 60);m = m % 60;
				}
				pw.close();
			} catch (IOException e) {
			}
		}		
		void T1_outtime_1min(int h,int m,int s,int cnt,int index,String ss){
			try {
				FileWriter fw = new FileWriter(ss, false);
				PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
				for(int i = 1;i < cnt/2;i++){
					double num = h*100 + m*60*100.0/3600 + ((s % 100) * 100.0)/3600;
					int sum = h*10000 + m*100 + s;
					pw.printf("%d,%f,%d,%d,%d\n",sum,num,Rflow[1][i] - Rflow[1][i-1],Rflow[2][i] - Rflow[2][i-1],Rflow[3][i] - Rflow[3][i-1]);
					m = m + 1;
					h = h + (m / 60);m = m % 60;
				}
				pw.close();
			} catch (IOException e) {
			}
		}		
		void T2_set(int h,int m,int s){
			for(int i = 0;i < 20000;i++){
				s = s + 1;
				m = m + (s/60);s = s % 60;
				h = h + (m/60);m = m % 60;
				T2_time[i] = h*10000 + m*100 + s;
			}
			for(int i = 0;i < 50000;i++)route[i] = -1;
			for(int i = 0;i < 4;i++){
				for(int j = 0;j < 400;j++){
					ST[i][j] = -1;
				}
			}
		}
		void T2_csvCheck(String line){
			String s1[] = line.split(",");
			//int s = Integer.parseInt(s1[0].replaceAll("ag", ""));
			//if(s1[4].equals("SL_INDEXA")){
			//	if(s1[1].equals("R1"))pos[s] = 1;
			//	else if(s1[1].equals("R2"))pos[s] = 2;
			//	else if(s1[1].equals("R3"))pos[s] = 3;
			int s = Integer.parseInt(s1[1].replaceAll("ag", ""));
			if(!s1[8].equals("")){
				if(!s1[12].equals("")){
				pos[s] = 1;
					pos_time[s][0] = 0;
					pos_time[s][1] = Integer.parseInt(s1[10]) - Integer.parseInt(s1[8]);
					pos_time[s][2] = Integer.parseInt(s1[11]) - Integer.parseInt(s1[10]);
					pos_time[s][3] = Integer.parseInt(s1[22]) - Integer.parseInt(s1[11]);
				}
				else if(!s1[18].equals("")){
					pos[s] = 2;
					pos_time[s][0] = 0;
					pos_time[s][1] = Integer.parseInt(s1[17]) - Integer.parseInt(s1[8]);
					pos_time[s][2] = Integer.parseInt(s1[22]) - Integer.parseInt(s1[17]);
				}
				else if(!s1[20].equals("")){
					pos[s] = 3;
					pos_time[s][0] = 0;
					pos_time[s][1] = Integer.parseInt(s1[19]) - Integer.parseInt(s1[8]);
					pos_time[s][2] = Integer.parseInt(s1[22]) - Integer.parseInt(s1[19]);
				}
			}
		}
		void T2_csvRead(String line,ArrayList<ArrayList<T2_Par>> Route1,ArrayList<ArrayList<T2_Par>> Route2,ArrayList<ArrayList<T2_Par>> Route3){
			String ss[] = line.split(",");
			int s = Integer.parseInt(ss[0].replaceAll("ag", ""));
			if(pos[s] == 0)return;
			int time2 = Integer.parseInt(ss[4]) % 60;
			
			if(route[s] == -1){
				if(Math.abs(time2) > 30)return;
				route[s] = Integer.parseInt(ss[4]) / 60;
			}
			
			if(ST[pos[s]][route[s]] == -1){
				if(pos[s] == 1)Route1.get(route[s]).add(new T2_Par(ss[2],ss[3],ss[4],ss[5]));
				if(pos[s] == 2)Route2.get(route[s]).add(new T2_Par(ss[2],ss[3],ss[4],ss[5]));
				if(pos[s] == 3)Route3.get(route[s]).add(new T2_Par(ss[2],ss[3],ss[4],ss[5]));
				ST[pos[s]][route[s]] = s;
				ST_Time[pos[s]][route[s]] = 1;
				for(int i = 0;i < 4;i++){
					ST_Time_Sep[pos[s]][i][route[s]] = pos_time[s][i];
				}
			}else if(ST[pos[s]][route[s]] == s){
				if(ss[5].equals("255081261") || ss[5].equals("95008905")){
					ST[pos[s]][route[s]] = -s;
					return;
				}
				ST_Time[pos[s]][route[s]] = ST_Time[pos[s]][route[s]] + 1;
				if(pos[s] == 1)Route1.get(route[s]).add(new T2_Par(ss[2],ss[3],ss[4],ss[5]));
				if(pos[s] == 2)Route2.get(route[s]).add(new T2_Par(ss[2],ss[3],ss[4],ss[5]));
				if(pos[s] == 3)Route3.get(route[s]).add(new T2_Par(ss[2],ss[3],ss[4],ss[5]));
			}
		}
		void T2_output(ArrayList<ArrayList<T2_Par>> Route,double numsum,int pos,int pp,int roop,int island){
			try {
				for(int i = 0;i < 300;i++){
					FileWriter fw = new FileWriter(Anapos+"Route"+pos+"_"+i+".csv", false);
					PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
					int length = Route.get(i).size();
					double dist[] = new double[length];
					for(int j = 1;j < length-1;j++){
						double x1 = Double.parseDouble(Route.get(i).get(j-1).x);
						double y1 = Double.parseDouble(Route.get(i).get(j-1).y);
						double x2 = Double.parseDouble(Route.get(i).get(j).x);
						double y2 = Double.parseDouble(Route.get(i).get(j).y);
						dist[j] = dist[j - 1] + Math.sqrt((x2 - x1)*(x2 - x1) + (y2 - y1)*(y2 - y1));
					}
					double par = 0;
					if(length > 1)par = numsum / dist[length - 2];
					
					for(int j = 1;j < length-1;j++){
						double num1 = T2_time[Integer.parseInt(Route.get(i).get(j).Ntime)];
						double h = (int)(num1 / 10000);
						double m = (int)((num1 % 10000) / 100);
						double s = ((num1 % 100) * 100)/3600;
						num1 = h*100 + m*60*100/3600 + s;
						dist[j] = dist[j] * par;
						pw.println(T2_time[Integer.parseInt(Route.get(i).get(j).Ntime)]+","+num1+","+Route.get(i).get(j).x+","+Route.get(i).get(j).y+","+dist[j]);
					}
					pw.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		class T2_Par{
			String x,y,Ntime,route;
			public T2_Par(String x,String y,String Ntime,String route){
				this.x = x;
				this.y = y;
				this.Ntime = Ntime;
				this.route = route;
			}
		}
		void T3_csv_write(int j,int i,double h,double m,double s,double Dist[],double Cnt[],int pos,int roop,int island){
			double Distance = 0.0;
			int cc = 0;
			try {
				String str = Anapos+"Route_A_"+j+"_"+i+".csv";
				FileWriter fw = new FileWriter(str, false);
				PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
				
				while(Distance < Separate_Route[j]){
					int num1 = (int)(h*10000 + m*100 + s);
					double num2 = h*100 + m*5/3 + (s/36);
					Distance = Distance + (Dist[cc] / Cnt[cc]);
					if(Distance > Separate_Route[j])Distance = Separate_Route[j];
					pw.printf("%d,%f,%f\n",num1,num2,Distance);
					cc++;
					s++;
					if(s == 60){
						s = 0;m++;
						if(m == 60){
							m = 0;h++;
						}
					}
				}
	
				pw.close();
			}catch (IOException e){
				
			}
		}
		void T3_Dist_sum(int f1,int f2,double Dist[],double Cnt[],int pos,int roop,int island){
			String str = Anapos+"Route"+f1+"_"+f2+".csv";
			try {
				FileReader fr = new FileReader(str);
				BufferedReader br = new BufferedReader(fr);
				String line;
				int cc = 0;
				double df = 0;
				double de = 0;
				while ((line = br.readLine()) != null) {
					String ss[] = line.split(",");
					df = Double.parseDouble(ss[4]);
					Dist[cc] = Dist[cc] + (df - de);
					Cnt[cc] = Cnt[cc] + 1.0;
					de = df;
					cc++;
					if(cc > 0)Set = true;
				}
				br.close();
			}catch (IOException e){
				
			}
		}
		void Result_Happy(int num,int roop,int island,double Max_Average,double Max_Min_Max_Dist,double Sum_Average,double Sum_Min_Max_Dist,double Stop_pow,double Stop){
			try {
				FileWriter fw = new FileWriter(Anapos+"Happy.csv", false);
				PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
				pw.println("#testNo,Stop_Time_pow,Stop_Time,Sum_Average,Sum_Min_Max_Dist,Max_Average,Max_Min_Max_Dist");
				pw.println(num+","+Stop_pow+","+Stop+","+Sum_Average+","+Sum_Min_Max_Dist+","+Max_Average+","+Max_Min_Max_Dist);
				pw.close();
			} catch (IOException ex){
			}
		}
		void CMA_Opt(int num,int roop,int island,double Max_Average,double Max_Min_Max_Dist,double Sum_Average,double Sum_Min_Max_Dist){
			try {
				FileWriter fw = new FileWriter(Rankpos+"CMA_Opt.csv", true);
				PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
				pw.println(num+","+Sum_Average+","+Sum_Min_Max_Dist+","+Max_Average+","+Max_Min_Max_Dist);
				pw.close();
			} catch (IOException ex){
			}
		}
		void CMA_Opt2(int num,int roop,int island,double Max_Average,double Max_Min_Max_Dist,double Sum_Average,double Sum_Min_Max_Dist,double Stop_pow,double Stop){
			try {
				FileWriter fw = new FileWriter(Rankpos+"CMA_Opt.csv", true);
				PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
				pw.println(num+","+Stop_pow+","+Stop+","+Sum_Average+","+Sum_Min_Max_Dist+","+Max_Average+","+Max_Min_Max_Dist);
				pw.close();
			} catch (IOException ex){
			}
		}
		class Par{
			double time,flow;
			public Par(double time,double flow){
				this.time = time;
				this.flow = flow;
			}
		}
	}
}
