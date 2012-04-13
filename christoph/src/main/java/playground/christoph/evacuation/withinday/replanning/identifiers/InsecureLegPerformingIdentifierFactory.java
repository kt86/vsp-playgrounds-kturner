/* *********************************************************************** *
 * project: org.matsim.*
 * InsecureLegPerformingIdentifierFactory.java
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

import org.matsim.api.core.v01.network.Network;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;

import playground.christoph.evacuation.analysis.CoordAnalyzer;

public class InsecureLegPerformingIdentifierFactory extends DuringLegIdentifierFactory {

	private final LinkReplanningMap linkReplanningMap;
	private final Network network;
	private final CoordAnalyzer coordAnalyzer;
	
	public InsecureLegPerformingIdentifierFactory(LinkReplanningMap linkReplanningMap, Network network, CoordAnalyzer coordAnalyzer) {
		this.linkReplanningMap = linkReplanningMap;
		this.network = network;
		this.coordAnalyzer = coordAnalyzer;
	}
	
	@Override
	public DuringLegIdentifier createIdentifier() {
		DuringLegIdentifier identifier = new InsecureLegPerformingIdentifier(linkReplanningMap, network, coordAnalyzer);
		identifier.setIdentifierFactory(this);
		this.addAgentFiltersToIdentifier(identifier);
		return identifier;
	}

}
