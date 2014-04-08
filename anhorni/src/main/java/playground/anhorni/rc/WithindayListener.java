/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.rc;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scoring.functions.OnlyTravelTimeDependentScoringFunctionFactory;
import org.matsim.withinday.controller.WithinDayControlerListener;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.filter.LinkFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;

public class WithindayListener implements StartupListener {
	
	protected Scenario scenario;
	protected WithinDayControlerListener withinDayControlerListener;
	

	public WithindayListener(Controler controler) {
		
		this.scenario = controler.getScenario();
		this.withinDayControlerListener = new WithinDayControlerListener();
		
		// Use a Scoring Function, that only scores the travel times!
		controler.setScoringFunctionFactory(new OnlyTravelTimeDependentScoringFunctionFactory());
		controler.setTravelDisutilityFactory(new OnlyTimeDependentTravelDisutilityFactory());
		
		// workaround
		this.withinDayControlerListener.setLeastCostPathCalculatorFactory(new DijkstraFactory());
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		
		// initialze within-day module
		this.withinDayControlerListener.notifyStartup(event);		
		this.initWithinDayReplanning(this.scenario);
	}
	
	private void initWithinDayReplanning(Scenario scenario) {		
		TravelDisutility travelDisutility = withinDayControlerListener.getTravelDisutilityFactory()
				.createTravelDisutility(withinDayControlerListener.getTravelTimeCollector(), scenario.getConfig().planCalcScore());
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, withinDayControlerListener.getTravelTimeCollector());
		
		LeaveLinkIdentifierFactory duringLegIdentifierFactory = new LeaveLinkIdentifierFactory(withinDayControlerListener.getLinkReplanningMap(),
				withinDayControlerListener.getMobsimDataProvider());
		
		Set<Id> links = new HashSet<Id>();
		
		LinkFilterFactory linkFilter = new LinkFilterFactory(links, withinDayControlerListener.getMobsimDataProvider());
		duringLegIdentifierFactory.addAgentFilterFactory(linkFilter);
		
		DuringLegIdentifier duringLegIdentifier = duringLegIdentifierFactory.createIdentifier();
		
		CurrentLegReplannerFactory duringLegReplannerFactory = new CurrentLegReplannerFactory(scenario, withinDayControlerListener.getWithinDayEngine(),
				withinDayControlerListener.getWithinDayTripRouterFactory(), routingContext);
		duringLegReplannerFactory.addIdentifier(duringLegIdentifier);
		
		withinDayControlerListener.getWithinDayEngine().addDuringLegReplannerFactory(duringLegReplannerFactory);
	}

}