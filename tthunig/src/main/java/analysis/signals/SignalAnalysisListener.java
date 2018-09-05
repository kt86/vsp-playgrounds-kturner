/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package analysis.signals;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import com.google.inject.Inject;

//import playground.vsp.analysis.utils.GnuplotUtils;


/**
 * Class to bind the signal analyze and writing tool to the simulation. 
 * 
 * @author tthunig
 */
public class SignalAnalysisListener implements IterationEndsListener {

	private static final Logger log = Logger.getLogger(SignalAnalysisListener.class);
	
	@Inject
	private Scenario scenario;
	
	@Inject
	private SignalAnalysisWriter writer;
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// write analyzed data
		writer.writeIterationResults(event.getIteration());
		runGnuplotScript("plot_bygoneSignalTimes", event.getIteration());

		// handle last iteration
		if (event.getIteration() == scenario.getConfig().controler().getLastIteration()) {
			// close overall writing stream
			writer.closeAllStreams();
			// plot overall iteration results
			runGnuplotScript("plot_signalOverIt", event.getIteration());
		}
	}
	
	/**
	 * starts the gnuplot script from the specific iteration directory
	 * 
	 * @param gnuplotScriptName
	 * @param iteration
	 */
	private void runGnuplotScript(String gnuplotScriptName, int iteration){
		String pathToSpecificAnalysisDir = scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it." + iteration + "/analysis";		
		String relativePathToGnuplotScript = "../../../../../../../shared-svn/studies/tthunig/gnuplotScripts/" + gnuplotScriptName  + ".p";
		
//		log.info("execute command: cd " + pathToSpecificAnalysisDir);
//		log.info("and afterwards: gnuplot " + relativePathToGnuplotScript);
		
//		GnuplotUtils.runGnuplotScript(pathToSpecificAnalysisDir, relativePathToGnuplotScript);
	}

}
