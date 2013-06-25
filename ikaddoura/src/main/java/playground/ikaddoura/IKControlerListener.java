/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerListener.java
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

package playground.ikaddoura;

import org.matsim.core.api.experimental.events.EventsManager;

import org.matsim.core.controler.events.StartupEvent;

import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;

/**
 * @author Ihab
 *
 */

public class IKControlerListener implements StartupListener {

	private final ScenarioImpl scenario;

	public IKControlerListener(ScenarioImpl scenario){
		this.scenario = scenario;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		EventsManager eventsManager = event.getControler().getEvents();
//		event.getControler().getEvents().addHandler(new MarginalCongestionHandlerV1(eventsManager, scenario));		
	}

}
