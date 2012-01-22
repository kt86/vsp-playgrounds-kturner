/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.ikaddoura.busCorridor.finalDyn2;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * @author Ihab
 *
 */

public class ExternalControler {
	
	private final static Logger log = Logger.getLogger(ExternalControler.class);
	
	static String networkFile = "../../shared-svn/studies/ihab/busCorridor/input_final/network.xml";
	static String configFile = "../../shared-svn/studies/ihab/busCorridor/input_final/config_busline.xml";
	static String populationFile = "../../shared-svn/studies/ihab/busCorridor/input_final/populationWorkOther.xml"; // for first iteration only
	static String outputExternalIterationDirPath = "../../shared-svn/studies/ihab/busCorridor/output_test";
	static int lastExternalIteration = 0;
	static int lastInternalIteration = 0;
	
	// settings for first iteration or if values not changed for all iterations
	TimePeriod p1 = new TimePeriod(1, "SVZ_1", 1, 3*3600, 6*3600); // orderId, id, numberOfBuses, fromTime, toTime
	TimePeriod p2 = new TimePeriod(2, "HVZ_1", 2, 6*3600, 9*3600);
	TimePeriod p3 = new TimePeriod(3, "NVZ", 3, 9*3600, 15*3600);
	TimePeriod p4 = new TimePeriod(4, "HVZ_2", 4, 15*3600, 17*3600);
	TimePeriod p5 = new TimePeriod(5, "SVZ_2", 8, 17*3600, 23*3600);

	private final double MONEY_UTILS = 0.14026; // has to be positive, because costs are negative!
	private double fare = -2.5; // negative!
	private int capacity = 50; // standing room + seats (realistic values between 19 and 101!)

	private int extItNr;
	private String directoryExtIt;
	private int maxNumberOfBuses;
	
	private Map<Integer, TimePeriod> day = new HashMap<Integer, TimePeriod>();

	private Map<Integer, Double> iteration2operatorProfit = new HashMap<Integer, Double>();
	private Map<Integer, Double> iteration2operatorCosts = new HashMap<Integer, Double>();
	private Map<Integer, Double> iteration2operatorRevenue = new HashMap<Integer, Double>();
	private Map<Integer, Double> iteration2numberOfBuses = new HashMap<Integer, Double>();
	private Map<Integer, String> iteration2day = new HashMap<Integer, String>();
	private Map<Integer, Double> iteration2userScore = new HashMap<Integer,Double>();
	private Map<Integer, Double> iteration2userScoreSum = new HashMap<Integer,Double>();
	private Map<Integer, Double> iteration2totalScore = new HashMap<Integer,Double>();
	private Map<Integer, Integer> iteration2numberOfCarLegs = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> iteration2numberOfPtLegs = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> iteration2numberOfWalkLegs = new HashMap<Integer, Integer>();
	private Map<Integer, Double> iteration2fare = new HashMap<Integer, Double>();
	private Map<Integer, Double> iteration2capacity = new HashMap<Integer, Double>();

	public static void main(final String[] args) throws IOException {
		ExternalControler simulation = new ExternalControler();
		simulation.externalIteration();
	}
	
	private void externalIteration() throws IOException {
		
		day.put(p1.getOrderId(), p1);
		day.put(p2.getOrderId(), p2);
		day.put(p3.getOrderId(), p3);
		day.put(p4.getOrderId(), p4);
		day.put(p5.getOrderId(), p5);
		
		for (int extIt = 0; extIt <= lastExternalIteration ; extIt++){
			
			log.info("************* EXTERNAL ITERATION "+extIt+" BEGINS *************");
			this.setExtItNr(extIt);
			this.setDirectoryExtIt(outputExternalIterationDirPath +"/extITERS/extIt."+extIt);
			File directory = new File(this.getDirectoryExtIt());
			directory.mkdirs();
			
			VehicleScheduleWriter transitWriter = new VehicleScheduleWriter(this.day, this.getCapacity(), networkFile, this.getDirectoryExtIt());
			transitWriter.writeTransit();
			
			this.setDay(transitWriter.getNewDay());
			this.setMaxNumberOfBuses(this.day);

			InternalControler internalControler = new InternalControler(configFile, this.extItNr, this.getDirectoryExtIt(), lastInternalIteration, populationFile, outputExternalIterationDirPath, this.getMaxNumberOfBuses(), networkFile, fare, MONEY_UTILS);
			internalControler.run();

			Operator operator = new Operator(this.getMaxNumberOfBuses(), this.getCapacity());
			Users users = new Users(this.getDirectoryExtIt(), networkFile, MONEY_UTILS);
			
			OperatorUserAnalysis analysis = new OperatorUserAnalysis(this.directoryExtIt, lastInternalIteration, networkFile);
			analysis.readEvents(operator, users, this.day);
			
			users.calculateScore();
			operator.calculateScore();

			this.iteration2operatorProfit.put(this.getExtItNr(), operator.getProfit());
			this.iteration2operatorCosts.put(this.getExtItNr(), operator.getCosts());
			this.iteration2operatorRevenue.put(this.getExtItNr(), operator.getRevenue());
			this.iteration2numberOfBuses.put(this.getExtItNr(), (double) this.getMaxNumberOfBuses());
			this.iteration2day.put(this.getExtItNr(), this.day.toString());
			this.iteration2userScoreSum.put(this.getExtItNr(), users.getLogSum());
			this.iteration2userScore.put(this.getExtItNr(), users.getAvgExecScore());
			this.iteration2totalScore.put(this.getExtItNr(), (users.getLogSum()+operator.getProfit()));
			this.iteration2numberOfCarLegs.put(this.getExtItNr(), users.getNumberOfCarLegs());
			this.iteration2numberOfPtLegs.put(this.getExtItNr(), users.getNumberOfPtLegs());
			this.iteration2numberOfWalkLegs.put(this.getExtItNr(), users.getNumberOfWalkLegs());
			this.iteration2fare.put(this.getExtItNr(), this.getFare());
			this.iteration2capacity.put(this.getExtItNr(),(double) this.getCapacity());
			
			TextFileWriter stats = new TextFileWriter();
			stats.writeFile(outputExternalIterationDirPath, this.iteration2numberOfBuses, this.iteration2day, this.iteration2fare, this.iteration2capacity, this.iteration2operatorCosts, this.iteration2operatorRevenue, this.iteration2operatorProfit, this.iteration2userScore, this.iteration2userScoreSum, this.iteration2totalScore, this.iteration2numberOfCarLegs, this.iteration2numberOfPtLegs, this.iteration2numberOfWalkLegs);
			
			// settings for next external iteration	
			if (this.getExtItNr() < lastExternalIteration){
				
				this.setDay(increaseNumberOfBusesAllTimePeriods(1));
				
				this.setDay(increaseBuses("HVZ_1", 1)); // id, number of buses
				this.setDay(increaseBuses("HVZ_2", 1)); // id, number of buses
				
//				this.setDay(changeBusesRandomly(1, 15)); // minimalBusNumber, maximalBusNumber
//
//				this.setDay(extend("HVZ_1", 60 * 60));
//				this.setDay(extend("HVZ_2", 60 * 60));
				
//				this.setFare(operator.increaseFare(this.getFare(), -0.5)); // absolute value
//				this.setCapacity(operator.increaseCapacity(2)); // absolute value
			}
			
			log.info("************* EXTERNAL ITERATION "+extIt+" ENDS *************");
		}

		ChartFileWriter chartWriter = new ChartFileWriter();
		
		chartWriter.writeChart_Parameters(outputExternalIterationDirPath, this.iteration2numberOfBuses, "Number of buses per iteration", "NumberOfBuses");
		chartWriter.writeChart_Parameters(outputExternalIterationDirPath, this.iteration2capacity, "Vehicle capacity per iteration", "Capacity");
		chartWriter.writeChart_Parameters(outputExternalIterationDirPath, this.iteration2fare, "Bus fare per iteration", "Fare");

		chartWriter.writeChart_LegModes(outputExternalIterationDirPath, this.iteration2numberOfCarLegs, this.iteration2numberOfPtLegs);
		chartWriter.writeChart_UserScores(outputExternalIterationDirPath, this.iteration2userScore);
		chartWriter.writeChart_UserScoresSum(outputExternalIterationDirPath, this.iteration2userScoreSum);
		chartWriter.writeChart_TotalScore(outputExternalIterationDirPath, this.iteration2totalScore);
		chartWriter.writeChart_OperatorScores(outputExternalIterationDirPath, this.iteration2operatorProfit, this.iteration2operatorCosts, this.iteration2operatorRevenue);
	}

	private Map<Integer, TimePeriod> changeBusesRandomly(int min, int max) {
		// TODO Auto-generated method stub
		return null;
	}

	private Map<Integer, TimePeriod> increaseBuses(String periodId, int increase) {
		Map<Integer, TimePeriod> dayMod = this.getDay();	
		int period = 0;
		for (TimePeriod tt : dayMod.values()){
			if (tt.getId().equals(periodId)){
				period = tt.getOrderId();
			}
		}
		if (dayMod.containsKey(period)){
			dayMod.get(period).increaseNumberOfBuses(increase);
		}
		return dayMod;
	}

	private Map<Integer, TimePeriod> extend(String periodId, double time) {
		Map<Integer, TimePeriod> dayNextExtIt = this.getDay();	
		int period = 0;
		for (TimePeriod timePeriod : dayNextExtIt.values()){
			if (timePeriod.getId().equals(periodId)){
				period = timePeriod.getOrderId();
			}
		}
		
		if (dayNextExtIt.containsKey(period)){
			
			dayNextExtIt.get(period).changeFromTime(-time/2);
			dayNextExtIt.get(period).changeToTime(time/2);
			if (dayNextExtIt.containsKey(period-1)){
				dayNextExtIt.get(period-1).changeToTime(-time/2);
			}
			if (dayNextExtIt.containsKey(period+1)){
				dayNextExtIt.get(period+1).changeFromTime(time/2);
			}
		}		
		return dayNextExtIt;
	}
	
	private Map<Integer, TimePeriod> increaseNumberOfBusesAllTimePeriods(int i) {
		Map<Integer, TimePeriod> dayNextExtIt = new HashMap<Integer, TimePeriod>();
		for (TimePeriod t : this.getDay().values()){
			TimePeriod t2 = t;
			t2.increaseNumberOfBuses(1);
			dayNextExtIt.put(t.getOrderId(), t2);
		}
		return dayNextExtIt;
	}

	public int getMaxNumberOfBuses() {
		return maxNumberOfBuses;
	}

	public void setMaxNumberOfBuses(Map<Integer, TimePeriod> day) {
		int maxBusNumber = 0;
		for (TimePeriod t : day.values()){
			if (t.getNumberOfBuses() > maxBusNumber){
				maxBusNumber = t.getNumberOfBuses();
			}
		}
		log.info("Total number of Vehicles: "+maxBusNumber);
		this.maxNumberOfBuses = maxBusNumber;
	}

	/**
	 * @return the extItNr
	 */
	public int getExtItNr() {
		return extItNr;
	}

	/**
	 * @param extItNr the extItNr to set
	 */
	public void setExtItNr(int extItNr) {
		this.extItNr = extItNr;
	}

	/**
	 * @return the directoryExtIt
	 */
	public String getDirectoryExtIt() {
		return directoryExtIt;
	}

	/**
	 * @param directoryExtIt the directoryExtIt to set
	 */
	public void setDirectoryExtIt(String directoryExtIt) {
		this.directoryExtIt = directoryExtIt;
	}

	/**
	 * @return the fare
	 */
	public double getFare() {
		return fare;
	}

	/**
	 * @param fare the fare to set
	 */
	public void setFare(double fare) {
		this.fare = fare;
	}

	/**
	 * @return the capacity
	 */
	public int getCapacity() {
		return capacity;
	}

	/**
	 * @param capacity the capacity to set
	 */
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	/**
	 * @return the day
	 */
	public Map<Integer, TimePeriod> getDay() {
		return day;
	}

	/**
	 * @param day the day to set
	 */
	public void setDay(Map<Integer, TimePeriod> day) {
		this.day = day;
	}
	
}
