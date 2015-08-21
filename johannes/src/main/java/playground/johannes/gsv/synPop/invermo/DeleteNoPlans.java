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

package playground.johannes.gsv.synPop.invermo;

import playground.johannes.synpop.processing.PersonTask;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Person;

/**
 * @author johannes
 *
 */
public class DeleteNoPlans implements PersonTask {

	@Override
	public void apply(Person person) {
		if(person.getEpisodes().size() == 0) {
			person.setAttribute(CommonKeys.DELETE, "true");
		}

	}

}
