/* *********************************************************************** *
 * project: org.matsim.*
 * DgMfd
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;


/**
 * 
 * @author dgrether
 */
public class DgMfd implements LinkEnterEventHandler, LinkLeaveEventHandler, AgentArrivalEventHandler, AgentStuckEventHandler, AgentWait2LinkEventHandler {
	
	private static final Logger log = Logger.getLogger(DgMfd.class);
	
	private static final double binSizeSeconds = 1.0 * 60.0;
	private static final double vehicleSize = 7.5 * (1.0/0.7);
	
	private Map<Id, Double> firstTimeSeenMap = new HashMap<Id, Double>();
	private Map<Id, LinkLeaveEvent> lastTimeSeenMap = new HashMap<Id, LinkLeaveEvent>();
	private Map<Id, LinkEnterEvent> enterEventByPersonIdMap = new HashMap<Id, LinkEnterEvent>();
	private Network network;
	private double networkLengthKm;
	private Data data;

	
	public DgMfd(Network network){
		this.network = network;
		this.networkLengthKm =  this.calcNetworkLengthKm();
		this.data = new Data(networkLengthKm);
	}

	@Override
	public void reset(int iteration) {
		this.firstTimeSeenMap.clear();
		this.lastTimeSeenMap.clear();
		this.data.reset();
	}

	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		boolean personIdAlreadySeen = this.firstTimeSeenMap.containsKey(event.getPersonId());
		if (this.network.getLinks().containsKey(event.getLinkId())) {
			if (! personIdAlreadySeen) {
				this.firstTimeSeenMap.put(event.getPersonId(), event.getTime());
			}
			this.enterEventByPersonIdMap.put(event.getPersonId(), event);
		}
		else {
			if (personIdAlreadySeen){
				this.handleLeaveNetworkOrArrival(event.getPersonId());
			}
		}
	}
	
	@Override
	public void handleEvent(AgentWait2LinkEvent event) {
		if (this.network.getLinks().containsKey(event.getLinkId())){
			this.firstTimeSeenMap.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Link link = this.network.getLinks().get(event.getLinkId());
		if (link != null) {
			this.lastTimeSeenMap.put(event.getPersonId(), event);
			int slot = this.getBinIndex(event.getTime());
			this.data.incrementFlow(slot, link);
			LinkEnterEvent enterEvent = this.enterEventByPersonIdMap.get(event.getPersonId());
			if (enterEvent != null) {
				double velocity = link.getLength() / (event.getTime() - enterEvent.getTime());
				this.data.addVelocity(slot, link, velocity);
			}
		}
	}
	
	@Override
	public void handleEvent(AgentStuckEvent event) {
		log.warn("got AgentStuckEvent, the code might not be correct if removeStuckVehicles config switch is set to true");
	}
	
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.handleLeaveNetworkOrArrival(event.getPersonId());
	}

	
	private void handleLeaveNetworkOrArrival(Id personId) {
		Double firstEvent = this.firstTimeSeenMap.remove(personId);
		LinkLeaveEvent lastEvent = this.lastTimeSeenMap.remove(personId);
		
		if (firstEvent != null && lastEvent != null){
			int index = getBinIndex(firstEvent);
			this.data.incrementDepartures(index);
			index = getBinIndex(lastEvent.getTime());
			this.data.incrementArrivals(index);
		}
//		else {
//			log.warn("No first or last event found for person id: " + personId);
//		}
	}

	
	
	private int getBinIndex(double time){
		return (int)(time / binSizeSeconds);
	}
	

	
	private double calcNetworkLengthKm() {
		double length = 0.0;
		for (Link l : this.network.getLinks().values()){
			length += (l.getLength() * l.getNumberOfLanes());
		}
		return length / 1000.0;
	}


	public void writeFile(String filename) {
		PrintStream stream;
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		write(stream);
		stream.close();
	}
	
	public void completedEventsHandling() {
		this.data.completedEventsHandling();
	}
	
	public void write(final PrintStream stream) {
		String header  = "slot\ttime[s]\thour\tdepartures\tarrivals\ten-route\tdensity[veh/km]\tspaceMeanFlow\tspaceMeanSpeed\tnetworkLength[km]";
		stream.println(header);
		double density = 0.0;
		Integer arnew = 0;
		Integer depnew = 0;
		double noVehicles = 0;
		for (int slot = this.data.getFirstSlot() - 2; slot <= this.data.getLastSlot() + 2; slot++) {
			SlotData slotData = this.data.getSlotData().get(slot);
			if (slotData == null) {
				slotData = new SlotData();
			}
			arnew = slotData.arrivals;
//			if (arnew == null) {
//				arnew = 0;
//			}
			depnew = slotData.departures;
//			if (depnew == null) {
//				depnew = 0;
//			}
			noVehicles = noVehicles + depnew - arnew ;
			density = (noVehicles) / this.networkLengthKm;
			double timeSec = slot * binSizeSeconds;
			int hour = (int) (timeSec / 3600);	
			StringBuffer line = new StringBuffer();
			line.append(slot);
			line.append("\t");
			line.append(timeSec);
			line.append("\t");
			line.append(hour);
			line.append("\t");
			line.append(depnew);
			line.append("\t");
			line.append(arnew);
			line.append("\t");
			line.append(noVehicles);
			line.append("\t");
			line.append(density);
			line.append("\t");
			line.append(slotData.spaceMeanFlow);
			line.append("\t");
			line.append(slotData.spaceMeanSpeed);
			line.append("\t");
			line.append(networkLengthKm);
			
			stream.println(line.toString());
		}
	}

	
	private static class SlotData {
		Integer arrivals = 0;
		Integer departures = 0;
		Double spaceMeanSpeed = 0.0;
		Double spaceMeanFlow = 0.0;
	}
	
	private static class Data {
		
//		private SortedMap<Integer, Integer> arrivalsBySlot = new TreeMap<Integer, Integer>();
//		private SortedMap<Integer, Integer> departuresBySlot = new TreeMap<Integer, Integer>();
		private SortedMap<Integer, LinksData> linksBySlot = new TreeMap<Integer, LinksData>();
		private SortedMap<Integer, SlotData> dataBySlot = new TreeMap<Integer, SlotData>();
		private Integer firstSlot = null;
		private Integer lastSlot = null;
		private double networkLengthKm;
		
		
		public Data(double networkLengthKm) {
			this.networkLengthKm = networkLengthKm;
		}

		public void completedEventsHandling() {
			for (int slot = getFirstSlot(); slot <= getLastSlot(); slot++) {
				SlotData slotData = this.getSlotData().get(slot);
				if (slotData == null) {
					slotData = new SlotData();
					this.getSlotData().put(slot, slotData);
				}
				double spaceMeanSpeed = this.calcSpaceMeanSpeed(slot);
				double spaceMeanFlow = this.calcSpaceMeanFlow(slot);
				slotData.spaceMeanFlow = spaceMeanFlow;
				slotData.spaceMeanSpeed = spaceMeanSpeed;
			}
			linksBySlot.clear();
		}

		private double calcSpaceMeanSpeed(int slot) {
			LinksData linksData = getLinksDataBySlot().get(slot);
			double sum = 0.0;
			if (linksData != null) {
				for (LinkData ld : linksData.linkData.values()){
					if (ld.velocitySum > 0.0) {
//						sum += ld.velocitySum / ld.velocityCount;
						sum += (ld.velocityCount / ld.velocitySum);
					}
				}
				sum = sum / linksData.linkData.values().size();
			}
			return sum;
		}

		private double calcSpaceMeanFlow(int slot) {
			LinksData linksData = getLinksDataBySlot().get(slot);
			double sum = 0.0;
			if (linksData != null) {
				for (LinkData ld : linksData.linkData.values()){
					double lengthMeter = ld.link.getLength() * ld.link.getNumberOfLanes();
					sum += ld.flow * lengthMeter;
				}
				return sum / (networkLengthKm  * 1000.0);
			}
			return 0.0;
		}

		
		public Integer getFirstSlot(){
			return firstSlot;
		}
		
		public Integer getLastSlot(){
			return lastSlot;
		}
		
		private void checkSlot(Integer slot){
			if (firstSlot == null || slot < firstSlot) {
				firstSlot = slot;
			}
			if (lastSlot == null ||  slot > lastSlot) {
				lastSlot = slot;
			}
			if (! this.dataBySlot.containsKey(slot)) {
				this.dataBySlot.put(slot, new SlotData());
			}

		}
		public void incrementArrivals(Integer slot){
			this.checkSlot(slot);
			this.dataBySlot.get(slot).arrivals++;
		}
		
		public void incrementDepartures(Integer slot) {
			this.checkSlot(slot);
			this.dataBySlot.get(slot).departures++;
		}
		
		public void incrementFlow(Integer slot, Link l){
			this.checkSlot(slot);
			this.checkLinkData(slot, l);
			this.linksBySlot.get(slot).incrementFlow(l);
		}

		public void addVelocity(Integer slot, Link l, double v) {
			this.checkSlot(slot);
			this.checkLinkData(slot, l);
			this.linksBySlot.get(slot).addVelocity(l, v);
		}

		private void checkLinkData(Integer slot, Link l) {
			if (! this.linksBySlot.containsKey(slot)){
				this.linksBySlot.put(slot, new LinksData());
			}
		}
		
		
		public void reset(){
			this.linksBySlot.clear();
			this.lastSlot = null;
			this.firstSlot = null;
		}

		
		
		public SortedMap<Integer, SlotData> getSlotData() {
			return this.dataBySlot;
		}

		public Map<Integer, LinksData> getLinksDataBySlot(){
			return this.linksBySlot;
		}
		
	}
	
	private static class LinkData {
		Link link;
		Double flow = null;
		Double velocitySum = null;
		Double velocityCount = null;
		
		public LinkData(Link l) {
			link = l;
			flow = 0.0;
			velocitySum = 0.0;
			velocityCount = 0.0;
		}
		
	}
	
	private static class LinksData {

		private Map<Id, LinkData> linkData = new HashMap<Id, LinkData>();
		
		public void incrementFlow(Link l) {
			this.checkLinkData(l);
			linkData.get(l.getId()).flow++;
		}

		public void addVelocity(Link l, double v) {
			this.checkLinkData(l);
			linkData.get(l.getId()).velocitySum += (1/v);
			linkData.get(l.getId()).velocityCount++;
		}
		
		private void checkLinkData(Link l){
			if (! linkData.containsKey(l.getId())){
				linkData.put(l.getId(), new LinkData(l));
			}
		}
		
	}

}
