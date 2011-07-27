/* *********************************************************************** *
 /* *********************************************************************** *
 * project: org.matsim.*
 * FhEmissions.java
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
 *                                                                         
 * *********************************************************************** */
/**  @author friederike**/

package playground.fhuelsmann.emission;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;

import playground.benjamin.events.WarmEmissionEventImpl;
import playground.fhuelsmann.emission.objects.HbefaObject;
import playground.fhuelsmann.emission.objects.HotValue;
import playground.fhuelsmann.emission.objects.VisumObject;

public class WarmEmissionAnalysisModule implements AnalysisModule{
	private static final Logger logger = Logger.getLogger(WarmEmissionAnalysisModule.class);

	private final VisumObject[] roadTypes;
	private final EmissionsPerEvent emissionPerEvent;
	private final HbefaHot hbefaHot;
	private final ArrayList<String> listOfPollutant;
	private static int vehInfoWarnCnt = 0;
	private static int maxVehInfoWarnCnt = 10;
	private boolean completeAndValidVehInfo;

	private final String[][] roadTypesTrafficSituations = new String[100][4];

	public WarmEmissionAnalysisModule(ArrayList<String> listOfPollutant, VisumObject[] roadTypes, EmissionsPerEvent emissionFactor, HbefaHot hbefahot) {
		this.roadTypes = roadTypes;
		this.emissionPerEvent = emissionFactor;
		this.hbefaHot = hbefahot;
		this.listOfPollutant = listOfPollutant;
	}

	public  String findHbefaFromVisumRoadType(int roadType){
		return this.roadTypes[roadType].getHBEFA_RT_NR();
	}

	@Override
	public void calculateWarmEmissions(Id linkId, Id personId,
			Integer roadType, Double freeVelocity, Double linkLength,
			Double enterTime, Double travelTime, String ageFuelCcm,
			HbefaObject[][] hbefaTable, HbefaObject[][] hbefaHdvTable,
			EventsManager eventsManager) {
		
		completeAndValidVehInfo = false;

		// TODO: use freeVelocity, not hbefa value!
		// TODO: Delete EmissionPerEvent, transfer important methods to this class
		// TODO: Kill this array, set return type to Map
		double [] emissionsOfEvent;
		Map<String, Double> warmEmissions = new HashMap<String, Double>();
		Map<String, double[][]> hashOfPollutant = new TreeMap<String, double[][]>();

		if(ageFuelCcm != null){// check for non-existing vehicle information
			completeAndValidVehInfo = true;
			String[] ageFuelCcmArray = ageFuelCcm.split(";");
//			logger.info(personId + " " + ageFuelCcmArray[0] + " " + ageFuelCcmArray[1] + " " + ageFuelCcmArray[2]);
//			for(String string : ageFuelCcmArray){// check for non-valid vehicle information
//				if(string == null || string == ""){
			if(!ageFuelCcm.contains("Baujahr") || ageFuelCcm.contains("hubraum")
							||!ageFuelCcm.contains("Hubraum") ||!ageFuelCcm.contains("Antriebsart")
							|| ageFuelCcm.contains("99999") || ageFuelCcm.contains("99998")|| ageFuelCcm.contains("99994")
							|| ageFuelCcm.contains("99997") || ageFuelCcm.equals("") || ageFuelCcm.contains("default")
							|| ageFuelCcm.contains("Antriebsart:3") || ageFuelCcm.contains("Antriebsart:7") 
							|| ageFuelCcm.contains("Antriebsart:8")|| ageFuelCcm.contains("Antriebsart:9")){
					completeAndValidVehInfo = false;
//				}
			}
			String[] fuelCcmEuro = null;
			if(completeAndValidVehInfo){
				fuelCcmEuro = mapVehicleAttributesFromMiD2Hbefa(ageFuelCcmArray);

				String[] keys = new String[4];
				for(String pollutant : listOfPollutant){
					double[][] emissionsInFourSituations = new double[4][2];
					for(int i = 0; i < 4; i++){// [0] for freeFlow, [1] for heavy, [2] for saturated, [3] for Stop&Go
						keys[i] = makeKey(pollutant, roadType, fuelCcmEuro[0], fuelCcmEuro[1], fuelCcmEuro[2], i);
						HotValue hotValue = this.hbefaHot.getHbefaHot().get(keys[i]);
						if (hotValue != null) {
							emissionsInFourSituations[i][0] = hotValue.getV();
							emissionsInFourSituations[i][1] = hotValue.getEFA();
						} else {
							completeAndValidVehInfo = false;
						}
					}
					// in hashOfPollutant we save V and EFA for 4 traffic situations
					hashOfPollutant.put(pollutant, emissionsInFourSituations);
				}
			}
		}
		else{
			// do nothing
		}

		if(completeAndValidVehInfo){
			emissionsOfEvent = emissionPerEvent.calculateDetailedEmissions(hashOfPollutant, travelTime, linkLength);
			for(int i = 0; i < listOfPollutant.size(); i++){
				String pollutant = listOfPollutant.get(i);
				Double value = emissionsOfEvent[i];
				warmEmissions.put(pollutant, value);
			}
		}
		else{
			if(vehInfoWarnCnt < maxVehInfoWarnCnt){
				vehInfoWarnCnt++;
				logger.warn("Vehicle information for person " + personId + " is either non-existing or not valid. Using fleet average values instead.");
				if (vehInfoWarnCnt == maxVehInfoWarnCnt)
					logger.warn(Gbl.FUTURE_SUPPRESSED);
			}
			// linkage between Hbefa road types and Visum road types
			int hbefaRoadType = Integer.valueOf(findHbefaFromVisumRoadType(roadType));

			if(!personId.toString().contains("gv_")){// Non-HDV emissions; TODO: better filter?!?
				emissionsOfEvent = emissionPerEvent.calculateAverageEmissions (listOfPollutant, hbefaRoadType, travelTime, linkLength, hbefaTable);
			}
			else{// HDV emissions; TODO: "only for CO2 and FC are values available, otherwise 0.0", "so far only fc and co2 emissionFactors are listed in the hbefaHdvTable" --- WHAT?!?
				emissionsOfEvent = emissionPerEvent.calculateAverageEmissions(listOfPollutant, hbefaRoadType, travelTime, linkLength, hbefaHdvTable);
			}
			for(int i = 0; i < listOfPollutant.size(); i++){
				String pollutant = listOfPollutant.get(i);
				Double value = emissionsOfEvent[i];
				warmEmissions.put(pollutant, value);
			}
		}
		Event warmEmissionEvent = new WarmEmissionEventImpl(enterTime, linkId, personId, warmEmissions);
		eventsManager.processEvent(warmEmissionEvent);
	}

	// TODO: Put in main
	public void createRoadTypesTafficSituation(String filename) {
		int[] counter = new int[100];
		for(int i=0; i<100;i++)
			counter[i]=0;
		try{
			FileInputStream fstream = new FileInputStream(filename);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine="";
			//Read File Line By Line
			br.readLine();

			while ((strLine = br.readLine()) != null){

				//for all lines (whole text) we split the line to an array 
				String[] array = strLine.split(";");
				int roadtype=Integer.valueOf(array[0]);
				int traficSitIndex = counter[roadtype]++;
				//			System.out.println(roadtype+";"+traficSitIndex);
				this.roadTypesTrafficSituations[roadtype][traficSitIndex] = array[3]; 	
				//		System.out.println("5.05" + this.vehicleCharacteristic);
			}
			in.close();
		}
		catch (Exception e){
			logger.error("Error: " + e);
		}
	}

	// TODO: Put in main
	public void createRoadTypes(String filename){
		try{
			FileInputStream fstream = new FileInputStream(filename);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			//Read first line:
			br.readLine();
			//Read File Line By Line:
			while ((strLine = br.readLine()) != null){

				//for all lines (whole text) we split the line to an array 
				String[] array = strLine.split(",");
				VisumObject obj = new VisumObject(Integer.parseInt(array[0]), array[2]);
				this.roadTypes[obj.getVISUM_RT_NR()] = obj;
			}
			in.close();
		}
		catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
	}

	public String makeKey(String pollutant, int roadType, String technology, String Sizeclass, String EmConcept, int traficSitNumber){
		return "PC[3.1]" + ";" + "pass. car" + ";" + "2010" + ";" + ";" + pollutant + ";" + ";" + this.roadTypesTrafficSituations[roadType][traficSitNumber]
		                                                                                                                                    + ";" + "0%" + ";" + technology + ";" + Sizeclass + ";" + EmConcept + ";";
	}

	// is used in order to split a phrase like baujahr:1900 , we are only interested in 1900 as Integer
	private int splitAndConvert(String string, String splittZeichen){

		String[] array = string.split(splittZeichen);
		return Integer.valueOf(array[1]);
	}

	private String[] mapVehicleAttributesFromMiD2Hbefa(String[] ageFuelCcmArray){
		String[] fuelCcmEuro = new String[3];

		int fuelType = splitAndConvert(ageFuelCcmArray[1], ":");
		if(fuelType == 1) fuelCcmEuro[0] = "petrol (4S)";
		else if(fuelType == 2) fuelCcmEuro[0] = "diesel";
		else completeAndValidVehInfo = false;

		int cubicCap = splitAndConvert(ageFuelCcmArray[2], ":");
		if (cubicCap <= 1400) fuelCcmEuro[1] = "<1,4L";	
		else if(cubicCap <= 2000 && cubicCap > 1400) fuelCcmEuro[1] = "1,4-<2L";
		else if(cubicCap > 2000 && cubicCap < 90000) fuelCcmEuro[1] = ">=2L";
		else completeAndValidVehInfo = false;

		int year = splitAndConvert(ageFuelCcmArray[0], ":");
		if (year < 1993 && fuelCcmEuro[0].equals("petrol (4S)")) fuelCcmEuro[2] = "PC-P-Euro-0";
		else if (year < 1993 && fuelCcmEuro[0].equals("diesel")) fuelCcmEuro[2] = "PC-D-Euro-0";
		else if(year < 1997 && fuelCcmEuro[0].equals("petrol (4S)")) fuelCcmEuro[2] = "PC-P-Euro-1";
		else if(year < 1997 && fuelCcmEuro[0].equals("diesel")) fuelCcmEuro[2] = "PC-D-Euro-1";
		else if(year < 2001 && fuelCcmEuro[0].equals("petrol (4S)") ) fuelCcmEuro[2] = "PC-P-Euro-2";
		else if(year < 2001 && fuelCcmEuro[0].equals("diesel") ) fuelCcmEuro[2] = "PC-D-Euro-2";
		else if(year < 2006 && fuelCcmEuro[0].equals("petrol (4S)")) fuelCcmEuro[2] = "PC-P-Euro-3";
		else if(year < 2006 && fuelCcmEuro[0].equals("diesel")) fuelCcmEuro[2] = "PC-D-Euro-3";
		else if(year < 2011 && fuelCcmEuro[0].equals("petrol (4S)") ) fuelCcmEuro[2] = "PC-P-Euro-4";
		else if(year < 2011 && fuelCcmEuro[0].equals("diesel") ) fuelCcmEuro[2] = "PC-D-Euro-4";
		else if(year < 2015 && fuelCcmEuro[0].equals("petrol (4S)") ) fuelCcmEuro[2] = "PC-P-Euro-5";
		else if(year < 2015 && fuelCcmEuro[0].equals("diesel") ) fuelCcmEuro[2] = "PC-D-Euro-5";
		else completeAndValidVehInfo = false;
		
		return fuelCcmEuro;
	}
}