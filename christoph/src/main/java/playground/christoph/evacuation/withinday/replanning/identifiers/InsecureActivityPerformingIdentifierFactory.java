/* *********************************************************************** *
 * project: org.matsim.*
 * InsecureActivityPerformingIdentifierFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import org.matsim.api.core.v01.Coord;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;

public class InsecureActivityPerformingIdentifierFactory extends DuringActivityIdentifierFactory {

	private ActivityReplanningMap activityReplanningMap;
	private Coord centerCoord;
	private double secureDistance;
	
	public InsecureActivityPerformingIdentifierFactory(ActivityReplanningMap activityReplanningMap, Coord centerCoord, double secureDistance) {
		this.activityReplanningMap = activityReplanningMap;
		this.centerCoord = centerCoord;
		this.secureDistance = secureDistance;
	}
	
	@Override
	public DuringActivityIdentifier createIdentifier() {
		DuringActivityIdentifier identifier = new InsecureActivityPerformingIdentifier(activityReplanningMap, centerCoord, secureDistance);
		identifier.setIdentifierFactory(this);
		this.addAgentFiltersToIdentifier(identifier);
		return identifier;
	}

}
