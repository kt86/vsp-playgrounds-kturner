package playground.sergioo.passivePlanning2012.core.population;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;

import playground.sergioo.weeklySimulation.util.misc.Time;

public abstract class PlaceSharer {
	
	public class KnownPlace {
		
		//Attributes
		private Id facilityId;
		private SortedMap<Time.Period, Set<String>> timeTypes = new TreeMap<Time.Period, Set<String>>();
		/*{
			for(Period period:Time.Period.values())
				timeTypes.put(period, new HashSet<String>());
		}*/
		protected Map<String, SortedMap<Time.Period, Map<Id, Double>>> travelTimes = new HashMap<String, SortedMap<Time.Period, Map<Id, Double>>>();
		
		//Constructors
		KnownPlace(Id facilityId) {
			this.facilityId = facilityId;
		}
		
		public Id getFacilityId() {
			return facilityId;
		}
		
		public Set<double[]> getTimes(String activityType) {
			Set<double[]> times = new HashSet<double[]>();
			for(Entry<Time.Period, Set<String>> timeType: timeTypes.entrySet())
				if(timeType.getValue().contains(activityType))
					times.add(new double[]{timeType.getKey().getStartTime(), timeType.getKey().getEndTime()});
			return times;
		}
		public Set<String> getActivityTypes() {
			Set<String> activities = new HashSet<String>();
			for(Set<String> timeType: timeTypes.values())
				activities.addAll(timeType);
			return activities;
		}
		public Set<String> getActivityTypes(double time) {
			Set<String> types = timeTypes.get(Time.Period.getPeriod(time));
			if(types==null) {
				types = new HashSet<String>();
				timeTypes.put(Time.Period.getPeriod(time), types);
			}
			return types;
		}
		public double getTravelTime(String mode, double startTime, Id destinationId) {
			SortedMap<Time.Period, Map<Id, Double>> timess = travelTimes.get(mode);
			if(timess!=null) {
				Map<Id, Double> times = timess.get(Time.Period.getPeriod(startTime));
				if(times!=null) {
					Double time = times.get(destinationId);
					if(time!=null)
						return time;
				}
			}
			return -1;
		}
		
	}
	
	protected final Set<PlaceSharer> knownPeople = new HashSet<PlaceSharer>();
	protected final Map<Id, KnownPlace> knownPlaces = new HashMap<Id, KnownPlace>();
	private double shareProbability = 1;
	private boolean areKnownPlacesUsed = false;
	
	public PlaceSharer() {
	}
	
	public void setShareProbability(double shareProbability) {
		if(shareProbability>0 && shareProbability<=1)
			this.shareProbability = shareProbability;
	}
	public void addKnownPerson(PlaceSharer placeSharer) {
		knownPeople.add(placeSharer);
	}
	public boolean areKnownPlacesUsed() {
		return areKnownPlacesUsed;
	}
	public void setAreKnownPlacesUsed(boolean areKnownPlacesUsed) {
		this.areKnownPlacesUsed = areKnownPlacesUsed;
	}
	public Collection<KnownPlace> getKnownPlaces() {
		return knownPlaces.values();
	}
	public KnownPlace getKnownPlace(Id facilityId) {
		return knownPlaces.get(facilityId);
	}
	public void addKnownPlace(Id facilityId, double startTime, String typeOfActivity) {
		KnownPlace knownPlace = knownPlaces.get(facilityId);
		if(knownPlace==null) {
			knownPlace = new KnownPlace(facilityId);
			knownPlaces.put(facilityId, knownPlace);
		}
		Set<String> types = knownPlace.timeTypes.get(Time.Period.getPeriod(startTime));
		if(types==null) {
			types = new HashSet<String>();
			knownPlace.timeTypes.put(Time.Period.getPeriod(startTime), types);
		}
		types.add(typeOfActivity);
	}
	public void addKnownPlace(Id facilityId, double startTime, double endTime, String typeOfActivity) {
		KnownPlace knownPlace = knownPlaces.get(facilityId);
		if(knownPlace==null) {
			knownPlace = new KnownPlace(facilityId);
			knownPlaces.put(facilityId, knownPlace);
		}
		boolean add = false;
		for(Time.Period period:Time.Period.values()) {
			if(period.getStartTime()<=startTime && period.getEndTime()>=startTime)
				add = true;
			if(add) {
				Set<String> types = knownPlace.timeTypes.get(period);
				if(types==null) {
					types = new HashSet<String>();
					knownPlace.timeTypes.put(period, types);
				}
				types.add(typeOfActivity);
			}
			if(period.getStartTime()<=endTime && period.getEndTime()>=endTime)
				add = false;
		}
	}
	public void addKnownPlace(KnownPlace knownPlace) {
		KnownPlace knownPlace2 = knownPlaces.get(knownPlace.getFacilityId());
		if(knownPlace2==null)
			knownPlaces.put(knownPlace.getFacilityId(), knownPlace);
		else
			for(Entry<Time.Period, Set<String>> entry:knownPlace.timeTypes.entrySet()) {
				Set<String> acts = knownPlace2.timeTypes.get(entry.getKey());
				if(acts == null)
					knownPlace2.timeTypes.put(entry.getKey(), entry.getValue());
				else
					for(String act:entry.getValue())
						acts.add(act);
			}
	}
	public void addKnownTravelTime(Id oFacilityId, Id dFacilityId, String mode, double startTime, double travelTime) {
		KnownPlace knownPlaceO = knownPlaces.get(oFacilityId);
		if(knownPlaceO==null) {
			knownPlaceO = new KnownPlace(oFacilityId);
			knownPlaces.put(oFacilityId, knownPlaceO);
		}
		KnownPlace knownPlaceD = knownPlaces.get(dFacilityId);
		if(knownPlaceD==null) {
			knownPlaceD = new KnownPlace(dFacilityId);
			knownPlaces.put(dFacilityId, knownPlaceD);
		}
		SortedMap<Time.Period, Map<Id, Double>> timess = knownPlaceO.travelTimes.get(mode);
		if(timess==null) {
			timess = new TreeMap<Time.Period, Map<Id,Double>>();
			knownPlaceO.travelTimes.put(mode, timess);
		}
		Map<Id, Double> times = timess.get(Time.Period.getPeriod(startTime));
		if(times==null) {
			times = new ConcurrentHashMap<Id, Double>();
			timess.put(Time.Period.getPeriod(startTime), times);
		}
		times.put(dFacilityId, travelTime);
	}
	public void shareKnownPlace(Id facilityId, double startTime, String type) {
		for(PlaceSharer placeSharer:knownPeople)
			if(MatsimRandom.getRandom().nextDouble()<shareProbability && !placeSharer.areKnownPlacesUsed)
				placeSharer.addKnownPlace(facilityId, startTime, type);
	}
	public void shareKnownTravelTime(Id oFacilityId, Id dFacilityId, String mode, double startTime, double travelTime) {
		for(PlaceSharer placeSharer:knownPeople)
			placeSharer.addKnownTravelTime(oFacilityId, dFacilityId, mode, startTime, travelTime);
	}

}
