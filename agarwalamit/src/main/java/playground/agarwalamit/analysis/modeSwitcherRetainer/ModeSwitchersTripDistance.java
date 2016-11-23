/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis.modeSwitcherRetainer;

import java.io.BufferedWriter;
import java.util.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.legMode.distributions.LegModeRouteDistanceDistributionHandler;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;


/**
 * This will first find mode switchers and then returns trip distances in groups. 
 *<p>
 * @author amit
 */

public class ModeSwitchersTripDistance {

	private static final Logger LOG = Logger.getLogger(ModeSwitchersTripDistance.class);

	private final Comparator<Tuple<String, String>> comparator = new Comparator<Tuple<String, String>>() {
		@Override
		public int compare(Tuple<String, String> o1, Tuple<String, String> o2) {
			return o1.toString().compareTo(o2.toString());
		}
	};

	private final SortedMap<Tuple<String, String>, List<Id<Person>>> modeSwitchType2PersonIds = new TreeMap<>(comparator);
	private final SortedMap<Tuple<String, String>, Tuple<Double, Double>> modeSwitchType2TripDistances = new TreeMap<>(comparator);
	private final SortedMap<Tuple<String, String>, Integer> modeSwitchType2numberOfLegs = new TreeMap<>(comparator);


	public static void main(String[] args) {
		String dir = FileUtils.RUNS_SVN+"/detEval/emissionCongestionInternalization/otherRuns/output/1pct/run9/";
		String runCases[] ={"baseCaseCtd","ei","ci","eci"};

		for(String runCase : runCases){
			ModeSwitchersTripDistance mstd = new ModeSwitchersTripDistance();
			mstd.run(dir+runCase);
			mstd.writeModeSwitcherTripDistances(dir+runCase);
		}
	}

	private void run (final String runCase){
		// data from event files
		String eventsFileItFirst = runCase+"/ITERS/it.1000/1000.events.xml.gz";
		String eventsFileItLast = runCase+"/ITERS/it.1500/1500.events.xml.gz";
		Scenario sc = LoadMyScenarios.loadScenarioFromNetworkAndConfig(runCase+"/output_network.xml.gz", runCase+"/output_config.xml");

		Map<Id<Person>, List<Tuple<String, Double>>> person2ModeTravelDistsItFirst = getPerson2mode2TripDistances(eventsFileItFirst,sc);
		Map<Id<Person>, List<Tuple<String, Double>>> person2ModeTravelDistsTtLast = getPerson2mode2TripDistances(eventsFileItLast,sc);

		for(Id<Person> pId : person2ModeTravelDistsItFirst.keySet()){

			if(person2ModeTravelDistsTtLast.containsKey(pId) ) {

				int numberOfLegs = 0;
				if(person2ModeTravelDistsTtLast.get(pId).size() != person2ModeTravelDistsItFirst.get(pId).size()){
					//	if person does not have same number of trips as in first iteration

					LOG.warn("Person "+pId+" do not have the same number of trip legs in the two maps. This could be due to stuck and abort event. "
							+ "\n Thus including only minimum legs (removing the common trups) for that person.");
					numberOfLegs = Math.min(person2ModeTravelDistsTtLast.get(pId).size(),person2ModeTravelDistsItFirst.get(pId).size());
				} else numberOfLegs = person2ModeTravelDistsItFirst.get(pId).size();

				for(int ii=0; ii<numberOfLegs;ii++){
					Tuple<String, Double> firstItMode = person2ModeTravelDistsItFirst.get(pId).get(ii);
					Tuple<String, Double> lastItMode = person2ModeTravelDistsTtLast.get(pId).get(ii);

					String firstMode = firstItMode.getFirst();
					String lastMode = lastItMode.getFirst();

					Tuple<String, String> modeSwitchType = new Tuple<>(firstMode, lastMode);
					storeTripDistanceInfo(pId, modeSwitchType, new Tuple<>(firstItMode.getSecond(), lastItMode.getSecond()));
				} 

			} else if(!person2ModeTravelDistsTtLast.containsKey(pId)) {

				LOG.warn("Person "+pId+ "is not present in the last iteration map. This person is thus not included in the results. Probably due to stuck and abort event.");

			} 
		}
	}

	public void storeTripDistanceInfo(final Id<Person> personId, final Tuple<String, String> modeSwitchTyp, final Tuple<Double, Double> travelDistances){

		if(! this.modeSwitchType2numberOfLegs.containsKey(modeSwitchTyp)) { // initialize all
			this.modeSwitchType2numberOfLegs.put(modeSwitchTyp, 0);
			this.modeSwitchType2PersonIds.put(modeSwitchTyp, new ArrayList<>());
			this.modeSwitchType2TripDistances.put(modeSwitchTyp, new Tuple<>(0., 0.));
		}

		this.modeSwitchType2numberOfLegs.put(modeSwitchTyp, this.modeSwitchType2numberOfLegs.get(modeSwitchTyp)+1);

		List<Id<Person>> swicherIds = this.modeSwitchType2PersonIds.get(modeSwitchTyp);
		swicherIds.add(personId);
		this.modeSwitchType2PersonIds.put(modeSwitchTyp, swicherIds);

		Tuple<Double, Double> nowFirstLastItsTripTimes = new Tuple<>(this.modeSwitchType2TripDistances.get(modeSwitchTyp).getFirst() + travelDistances.getFirst(),
				this.modeSwitchType2TripDistances.get(modeSwitchTyp).getSecond() + travelDistances.getSecond());
		this.modeSwitchType2TripDistances.put(modeSwitchTyp, nowFirstLastItsTripTimes);
	}

	public void writeModeSwitcherTripDistances(final String runCase){
		String outFile = runCase+"/analysis/modeSwitchersTripDistances.txt";
		BufferedWriter writer =  IOUtils.getBufferedWriter(outFile);
		try {
			writer.write("firstMode \t lastMode \t numberOfLegs \t totalTripDistancesForFirstIterationInKm \t totalTripDistancesForLastIterationInKm \n");

			for(Tuple<String, String> str: this.modeSwitchType2numberOfLegs.keySet()){
				writer.write(str.getFirst()+"\t"+str.getSecond()+"\t"+this.modeSwitchType2numberOfLegs.get(str)+"\t"
						+this.modeSwitchType2TripDistances.get(str).getFirst()/1000.+"\t"
						+this.modeSwitchType2TripDistances.get(str).getSecond()/1000.+"\n");
			}

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(
					"Data is not written in file. Reason: " + e);
		}
		LOG.info("Data is written to "+outFile);
	}


	private Map<Id<Person>, List<Tuple<String, Double>>> getPerson2mode2TripDistances(final String eventsFile, final Scenario sc){

		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);

		LegModeRouteDistanceDistributionHandler distHandler = new LegModeRouteDistanceDistributionHandler(sc);
		events.addHandler(distHandler);

		reader.readFile(eventsFile);

		SortedMap<String,Map<Id<Person>,List<Double>>> mode2Person2TripDists = distHandler.getMode2PersonId2TravelDistances();

		Map<Id<Person>, List<Tuple<String, Double>>> person2ModeTravelDists = new HashMap<>();


		for(String mode : mode2Person2TripDists.keySet()){
			for (Id<Person> p : mode2Person2TripDists.get(mode).keySet()){
				for(Double d :mode2Person2TripDists.get(mode).get(p)){
					Tuple<String, Double> mode2TripDist = new Tuple<>(mode, d);

					if (person2ModeTravelDists.containsKey(p)){
						List<Tuple<String, Double>> mode2TripDistList  = person2ModeTravelDists.get(p);
						mode2TripDistList.add(mode2TripDist);
					} else {
						List<Tuple<String, Double>> mode2TripDistList = new ArrayList<>();
						mode2TripDistList.add(mode2TripDist);
						person2ModeTravelDists.put(p, mode2TripDistList);
					}

				}
			}
		}
		return person2ModeTravelDists;
	}
}
