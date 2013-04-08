/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.anhorni.barbellscenario;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;


public class TravelTimeEventHandler implements AgentDepartureEventHandler, AgentArrivalEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {
	private final Map<Id, AgentDepartureEvent> pendantDepartureEvent = new HashMap<Id, AgentDepartureEvent>();
	private final Map<Id, LinkEnterEvent> pendantLinkEnterEvent = new HashMap<Id, LinkEnterEvent>();
	
	private List<Double> netTTs = new Vector<Double>();
	private List<Double> linkTTs = new Vector<Double>();
	
	@Override
	public void reset(final int iteration) {
		this.pendantDepartureEvent.clear();
		this.pendantLinkEnterEvent.clear();
	}
	
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		double netTT = event.getTime() - this.pendantDepartureEvent.get(event.getPersonId()).getTime();
		this.netTTs.add(netTT);
	}

	@Override
	public void handleEvent(final AgentDepartureEvent event) {
		this.pendantDepartureEvent.put(event.getPersonId(), event);
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getLinkId().compareTo(new IdImpl(3)) == 0) {
			this.pendantLinkEnterEvent.put(event.getPersonId(), event);
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (event.getLinkId().compareTo(new IdImpl(3)) == 0) {
			double linkTT = event.getTime() - this.pendantLinkEnterEvent.get(event.getPersonId()).getTime();
			this.linkTTs.add(linkTT);
		}
	}

	public List<Double> getNetTTs() {
		return netTTs;
	}

	public List<Double> getLinkTTs() {
		return linkTTs;
	}	
}

