/* *********************************************************************** *
 * project: org.matsim.*
 * PenaltyTravelCostFactory.java
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

package playground.christoph.evacuation.router.util;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.PersonalizableTravelDisutility;
import org.matsim.core.router.util.PersonalizableTravelTime;

public class PenaltyTravelCostFactory implements TravelDisutilityFactory {

	private final TravelDisutilityFactory costFactory;
	private final AffectedAreaPenaltyCalculator penaltyCalculator;
	
	public PenaltyTravelCostFactory(TravelDisutilityFactory costFactory, AffectedAreaPenaltyCalculator penaltyCalculator) {
		this.costFactory = costFactory;
		this.penaltyCalculator = penaltyCalculator;
	}

	@Override
	public PersonalizableTravelDisutility createTravelDisutility(PersonalizableTravelTime travelTime, PlanCalcScoreConfigGroup cnScoringGroup) {
		PersonalizableTravelDisutility travelCost = this.costFactory.createTravelDisutility(travelTime, cnScoringGroup);
		return new PenaltyTravelCost(travelCost, penaltyCalculator.getPenaltyCalculatorInstance());
	}
	
}