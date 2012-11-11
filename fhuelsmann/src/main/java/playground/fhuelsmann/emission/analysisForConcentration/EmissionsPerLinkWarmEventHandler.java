/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerLinkWarmEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.fhuelsmann.emission.analysisForConcentration;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.vsp.emissions.events.WarmEmissionEvent;
import playground.vsp.emissions.events.WarmEmissionEventHandler;
import playground.vsp.emissions.types.WarmPollutant;
import playground.vsp.emissions.utils.EmissionUtils;

/**
 * @author benjamin, friederike
 *
 */
public class EmissionsPerLinkWarmEventHandler implements WarmEmissionEventHandler{
	private static final Logger logger = Logger.getLogger(EmissionsPerLinkWarmEventHandler.class);

	Map<Double, Map<Id, Map<WarmPollutant, Double>>> time2warmEmissionsTotal = new HashMap<Double, Map<Id, Map<WarmPollutant, Double>>>();
	Map<Double, Map<Id, Map<WarmPollutant, Double>>> time2warmHdvEmissionsTotal = new HashMap<Double, Map<Id, Map<WarmPollutant, Double>>>();
	Map<Double, Map<Id, Double>> time2linkIdLeaveCount = new HashMap<Double, Map<Id,Double>>();
	Map<Double, Map<Id, Double>> time2linkIdLeaveHdvCount = new HashMap<Double, Map<Id,Double>>();
	
	final int noOfTimeBins;
	final double timeBinSize;
	EmissionUtils emissionUtils;

	public EmissionsPerLinkWarmEventHandler(double simulationEndTime, int noOfTimeBins){
		this.noOfTimeBins = noOfTimeBins;
		this.timeBinSize = simulationEndTime / noOfTimeBins;
		this.emissionUtils = new EmissionUtils();
	}

	@Override
	public void reset(int iteration) {
		this.time2warmEmissionsTotal.clear();
		logger.info("Resetting warm emission aggregation to " + this.time2warmEmissionsTotal);
		this.time2warmHdvEmissionsTotal.clear();
		logger.info("Resetting warm emission aggregation to " + this.time2warmHdvEmissionsTotal);
		this.time2linkIdLeaveCount.clear();
		logger.info("Resetting linkLeve counter to " + this.time2linkIdLeaveCount);
		this.time2linkIdLeaveHdvCount.clear();
		logger.info("Resetting linkLeve hdv counter to " + this.time2linkIdLeaveHdvCount);
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Double time = event.getTime();
		Id linkId = event.getLinkId();
		Id vehicleId = event.getVehicleId();

		Map<WarmPollutant, Double> warmEmissionsOfEvent = event.getWarmEmissions();
		double endOfTimeInterval = 0.0;

		for(int i = 0; i < this.noOfTimeBins; i++){
			if(time > i * this.timeBinSize && time <= (i + 1) * this.timeBinSize){
				endOfTimeInterval = (i + 1) * this.timeBinSize;
				Map<Id, Map<WarmPollutant, Double>> warmEmissionsTotal = new HashMap<Id, Map<WarmPollutant, Double>>();;
				Map<Id, Double> countTotal = new HashMap<Id, Double>();
				
				if(this.time2warmEmissionsTotal.get(endOfTimeInterval) != null){
					warmEmissionsTotal = this.time2warmEmissionsTotal.get(endOfTimeInterval);
						countTotal = this.time2linkIdLeaveCount.get(endOfTimeInterval);
					
					if(warmEmissionsTotal.get(linkId) != null){
						Map<WarmPollutant, Double> warmEmissionsSoFar = warmEmissionsTotal.get(linkId);
						Map<WarmPollutant, Double> newWarmEmissionsSoFar = new HashMap<WarmPollutant, Double>();
						for(Entry<WarmPollutant, Double> entry : warmEmissionsOfEvent.entrySet()){
							WarmPollutant pollutant = entry.getKey();
							Double eventValue = entry.getValue();
							Double previousValue = warmEmissionsSoFar.get(pollutant);
							Double newValue = previousValue + eventValue;
							newWarmEmissionsSoFar.put(pollutant, newValue);
						}
						warmEmissionsTotal.put(linkId, newWarmEmissionsSoFar);
						double countsSoFar = countTotal.get(linkId);
						double newValue = countsSoFar + 1.;
						countTotal.put(linkId, newValue);
					} else {
						warmEmissionsTotal.put(linkId, warmEmissionsOfEvent);
						countTotal.put(linkId, 1.);
					}
				} else {
					warmEmissionsTotal.put(linkId, warmEmissionsOfEvent);
					countTotal.put(linkId, 1.);
				}
				this.time2warmEmissionsTotal.put(endOfTimeInterval, warmEmissionsTotal);
				this.time2linkIdLeaveCount.put(endOfTimeInterval, countTotal);
			}
		}
		
		if(vehicleId.toString().contains("gv_")){
			for(int i = 0; i < this.noOfTimeBins; i++){
				if(time > i * this.timeBinSize && time <= (i + 1) * this.timeBinSize){
					endOfTimeInterval = (i + 1) * this.timeBinSize;
					Map<Id, Map<WarmPollutant, Double>> warmEmissionsTotal = new HashMap<Id, Map<WarmPollutant, Double>>();;
					Map<Id, Double> countTotal = new HashMap<Id, Double>();
					
					if(this.time2warmHdvEmissionsTotal.get(endOfTimeInterval) != null){
						warmEmissionsTotal = this.time2warmHdvEmissionsTotal.get(endOfTimeInterval);
							countTotal = this.time2linkIdLeaveHdvCount.get(endOfTimeInterval);
						
						if(warmEmissionsTotal.get(linkId) != null){
							Map<WarmPollutant, Double> warmEmissionsSoFar = warmEmissionsTotal.get(linkId);
							Map<WarmPollutant, Double> newWarmEmissionsSoFar = new HashMap<WarmPollutant, Double>();
							for(Entry<WarmPollutant, Double> entry : warmEmissionsOfEvent.entrySet()){
								WarmPollutant pollutant = entry.getKey();
								Double eventValue = entry.getValue();

								Double previousValue = warmEmissionsSoFar.get(pollutant);
								Double newValue = previousValue + eventValue;
								newWarmEmissionsSoFar.put(pollutant, newValue);
							}
							warmEmissionsTotal.put(linkId, newWarmEmissionsSoFar);
							double countsSoFar = countTotal.get(linkId);
							double newValue = countsSoFar + 1.;
							countTotal.put(linkId, newValue);
						} else {
							warmEmissionsTotal.put(linkId, warmEmissionsOfEvent);
							countTotal.put(linkId, 1.);
						}
					} else {
						warmEmissionsTotal.put(linkId, warmEmissionsOfEvent);
						countTotal.put(linkId, 1.);
					}
					this.time2warmHdvEmissionsTotal.put(endOfTimeInterval, warmEmissionsTotal);
					this.time2linkIdLeaveHdvCount.put(endOfTimeInterval, countTotal);
				}
			}
		}else{//do nothing
		}
	}

	public Map<Double, Map<Id, Double>> getTime2linkIdLeaveCount() {
		return this.time2linkIdLeaveCount;
	}
	
	public Map<Double, Map<Id, Double>> getTime2linkIdLeaveHDVCount() {
		return this.time2linkIdLeaveHdvCount;
	}

	public Map<Double, Map<Id, Map<WarmPollutant, Double>>> getWarmEmissionsPerLinkAndTimeInterval() {
		return time2warmEmissionsTotal;
	}
}