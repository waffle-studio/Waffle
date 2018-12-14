#! /usr/bin/env ruby
## -*- mode: ruby -*-
## = Sample Agent for CrowdWalk
## Author:: Itsuki Noda
## Version:: 0.0 2015/06/28 I.Noda
## Version:: 1.0 2018/10/31 R.Nishida [change calcCost]
##
## === History
## * [2014/06/28]: Create This File.
## * [YYYY/MM/DD]: add more
## == Usage
## * ...

require "date" ;
require 'RubyAgentBase.rb' ;
require './normal.rb' ;
require './GateOperation_nishida' ;



#--======================================================================
#++
## SampleAgent class
class SampleAgent < RubyAgentBase
	
  #--============================================================
  #--::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  #++
  ## Java から Ruby を呼び出すTriggerでのFilter。
  ## この配列に Java のメソッド名（キーワード）が入っていると、
  ## Ruby 側が呼び出される。入っていないと、無視される。
  ## RubyAgentBase を継承するクラスは、このFilterを持つことが望ましい。
  ## このFilterは、クラスをさかのぼってチェックされる。
  TriggerFilter = [
#                   "preUpdate",
#                   "update",
                   "calcCostFromNodeViaLink",
#                   "calcSpeed",
#                   "calcAccel",
									 "setGoal",
                   "thinkCycle",
                  ] ;

  #--@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
  #++
  ## call counter
  attr_accessor :counter ;

  #--------------------------------------------------------------
  #++
  ## シミュレーション各サイクルの前半に呼ばれる。
  def initialize(agent, config, fallback) 
		@counter = 0 ;
		@@finish = 0;
    super ;
  end

  #--------------------------------------------------------------
  #++
  ## シミュレーション各サイクルの前半に呼ばれる。
  def preUpdate()
#    p ['SampleAgent', :preUpdate, getAgentId(), currentTime()] ;
    @counter += 1;
    return super()
  end

  #--------------------------------------------------------------
  #++
  ## シミュレーション各サイクルの後半に呼ばれる。
  def update()
#    p ['SampleAgent', :update, getAgentId(), currentTime()] ;
    @counter += 1;
    return super() ;
  end

	def getSimulator()
		@counter += 1;
		return super();
	end
 	
  
  #--------------------------------------------------------------
  #++
  ## あるwayを選択した場合の目的地(_target)までのコスト。
  ## _way_:: 現在進もうとしている道
  ## _node_:: 現在の分岐点
  ## _target_:: 最終目的地
  def calcCostFromNodeViaLink(link, node, target)
#    p ['SampleAgent', :calcCostFromNodeViaLink, getAgentId(), currentTime()] ;
    @counter += 1;
		
		# 通常
		cost = super(link, node, target);

		# 経路選択モデルによるコスト計算
		if getAgentId() then
			# 分岐点
			if (node.getTags().contains('X_R1-R2')) || (node.getTags().contains('X_R2-R3')) then
				params = $params.assoc(getAgentId())
				b1 = params[1].to_f
				b2 = params[2].to_f
				b3 = params[3].to_f
				b4 = params[4].to_f
				b5 = params[5].to_f
				e1 = params[6].to_f
				e2 = params[7].to_f
				## 確定項
				# 距離
				if(link.getTags().contains('X1_R1')) then
		  		distance = 0.325
					error = e1
	    	end
	    	if(link.getTags().contains('X1_R2')) then
					distance = 0.575
					error = e2
	    	end
				if(link.getTags().contains('X2_R2')) then
					distance = 0.135
					error = e1
	    	end
	    	if(link.getTags().contains('X2_R3')) then
					distance = 0.325	
					error = e2
	    	end
				
				# 密度
				density = link.realCrowdness();

				# 待機
				wait = 0;
	    	# atX1
	    	if(link.getTags().contains('X1_R1')) then
	        if(listenAlert(Term_J1closed)) then
						wait = 1;
	    		end
	    	end
	    	# atX2
	    	if(link.getTags().contains('X2_R2')) then
	        if(listenAlert(Term_J2closed)) then
						wait = 1;
	    		end
	    	end
				
				# 魅力、他人
				attract = 0;
				other = 0
				if getCurrentTime() then
			    time = getCurrentTime().getAbsoluteTimeString()
					if time[0,2].to_i < 24 then
						# atX1,toR1 花火
						if(link.getTags().contains('X1_R1')) && (listenAlert(Term_J1closed)) then
		    			if(DateTime.parse(time) > DateTime.parse("19:50:00")) && (DateTime.parse(time) < DateTime.parse("20:40:00")) then
		      			attract = 1;
		    			end
						end
						# atX1,toR2 屋台
						if(link.getTags().contains('X1_R2')) then
		    			if(DateTime.parse(time) < DateTime.parse("22:00:00")) then
								attract = 1;
		    			end
						end

						if(link.getTags().contains('X1_R1')) then
						other = $atX1toR1_count5.assoc(time)[1]
			    	end
			    	if(link.getTags().contains('X1_R2')) then
							other = $atX1toR2_count5.assoc(time)[1]
			    	end
						if(link.getTags().contains('X2_R2')) then
							other = $atX2toR2_count5.assoc(time)[1]
			    	end
			    	if(link.getTags().contains('X2_R3')) then
							other = $atX2toR3_count5.assoc(time)[1]		
			    	end
					end
				end
				
				
				## コスト
				cost = b1*distance + b2*density + b3*wait + b4*attract + b5*other + error; 
				return cost
			else
				if(link.getTags().contains('X1_R1')) ||  (link.getTags().contains('X1_R2')) || (link.getTags().contains('X2_R2')) ||  (link.getTags().contains('X2_R3'))then
					cost = 1000.0
					return cost
				end
			end
		end
		
		#
		if (node.getTags().contains('setR1')) then
			setGoal('EXIT_R1')
		elsif (node.getTags().contains('setR2')) then
			setGoal('EXIT_R2')
		elsif (node.getTags().contains('setR3')) then
			setGoal('EXIT_R3')
		end

		
    return  cost;
  end

  #--------------------------------------------------------------
  ## message
  Term_J1closed = ItkTerm.ensureTerm("J1_closed");
  Term_J2closed = ItkTerm.ensureTerm("J2_closed");  


	

  #--------------------------------------------------------------
  #++
  ## 速度を計算する。
  ## たまに減速させてみる。
  ## _previousSpeed_:: 前のサイクルの速度。
  ## *return* 速度。
  def calcSpeed(previousSpeed)
#    p ['SampleAgent', :calcSpeed, getAgentId(), currentTime()] ;
    @counter += 1;
    return super(previousSpeed) ;
  end

  #--------------------------------------------------------------
  #++
  ## 加速度を計算する。
  ## _baseSpeed_:: 自由速度。
  ## _previousSpeed_:: 前のサイクルの速度。
  ## *return* 加速度。
  def calcAccel(baseSpeed, previousSpeed)
#    p ['SampleAgent', :calcAccel, getAgentId(), currentTime()] ;
    @counter += 1;
    return super(baseSpeed, previousSpeed) ;
  end

  #--------------------------------------------------------------
  #++
  ## 思考ルーチン。
  ## ThinkAgent のサンプルと同じ動作をさせている。
  def thinkCycle()
#    p ['SampleAgent', :thinkCycle, getAgentId(), currentTime()] ;
    @counter += 1;
#    return super ;
    return ItkTerm::NullTerm ;
  end

end # class SampleAgent

