/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.optAV;

import static org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.examples.TaxiDvrpModules;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import playground.ikaddoura.analysis.IKAnalysisRun;
import playground.ikaddoura.analysis.modalSplitUserType.AgentAnalysisFilter;
import playground.ikaddoura.analysis.modalSplitUserType.ModalSplitUserTypeControlerListener;

/**
* @author ikaddoura
*/

public class RunBerlinOptAV {

	private static final Logger log = Logger.getLogger(RunBerlinOptAV.class);

	private static String configFile;
	private static String outputDirectory;
	private static String runId;
	private static String visualizationScriptInputDirectory;
	private static boolean carAvailableModeForNonBerliners = true;
	private static final boolean otfvis = false;
	
	public static void main(String[] args) {
		if (args.length > 0) {
			
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
			outputDirectory = args[1];
			log.info("outputDirectory: "+ outputDirectory);
			
			runId = args[2];
			log.info("runId: "+ runId);
			
			visualizationScriptInputDirectory = args[3];
			log.info("visualizationScriptInputDirectory: "+ visualizationScriptInputDirectory);
			
			carAvailableModeForNonBerliners = Boolean.parseBoolean(args[4]);
			log.info("carAvailableModeForNonBerliners: "+ carAvailableModeForNonBerliners);

		} else {
			
			configFile = "/Users/ihab/Documents/workspace/runs-svn/b5_optAV_congestion/input/berlin-5.1_1pct_config_oAV_G1.xml";
			runId = "oAV_G1_1pct";

//			configFile = "/Users/ihab/Documents/workspace/runs-svn/b5_optAV_congestion/input/berlin-5.1_1pct_config_oAV_A1_test.xml";
//			runId = "oAV_A1_1pct_test";

			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/b5_optAV_congestion/output/run_" + runId + "/";
			visualizationScriptInputDirectory = "./visualization-scripts/";
			carAvailableModeForNonBerliners = false;
		}
		
		RunBerlinOptAV runner = new RunBerlinOptAV();
		runner.run();
	}

	public void run() {
		
		Config config = ConfigUtils.loadConfig(
				configFile,
				new OptAVConfigGroup(),
				new TaxiConfigGroup(),
				new DvrpConfigGroup(),
				new TaxiFareConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup(),
				new DecongestionConfigGroup()
				);
		
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setRunId(runId);
		
		// berlin scenario related settings

		config.controler().setRoutingAlgorithmType( FastAStarLandmarks );
		
		config.subtourModeChoice().setProbaForRandomSingleTripMode(0.5);
		
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		// use the sbb pt raptor router
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new SwissRailRaptorModule());
			}
		});
		
		// use the (congested) car travel time for the teleported ride mode
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());        }
	    });
		
		// some online analysis
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {				
				this.addControlerListenerBinding().to(ModalSplitUserTypeControlerListener.class);
			}
		});
		
		// taxi-related modules
		controler.addOverridingModule(TaxiDvrpModules.create());
		controler.addOverridingModule(new TaxiModule());
		
		// optAV module
		controler.addOverridingModule(new OptAVModule(scenario));
		
		// different modes for different subpopulations
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				
				final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
				
				List<String> availableModesArrayList = new ArrayList<>();
				availableModesArrayList.add("bicycle");
				availableModesArrayList.add("pt");
				availableModesArrayList.add("walk");
				if (carAvailableModeForNonBerliners) {
					availableModesArrayList.add("car");
				}
				
				final String[] availableModes = availableModesArrayList.toArray(new String[availableModesArrayList.size()]);
				
				addPlanStrategyBinding("SubtourModeChoice_noPotentialSAVuser").toProvider(new Provider<PlanStrategy>() {
										
					@Inject
					Scenario sc;

					@Override
					public PlanStrategy get() {
						
						log.info("SubtourModeChoice_noPotentialSAVuser" + " - available modes: " + availableModes.toString());
						final String[] chainBasedModes = {"car", "bicycle"};

						final Builder builder = new Builder(new RandomPlanSelector<>());
						builder.addStrategyModule(new SubtourModeChoice(sc.getConfig()
								.global()
								.getNumberOfThreads(), availableModes, chainBasedModes, false, 
								0.5, tripRouterProvider));
						builder.addStrategyModule(new ReRoute(sc, tripRouterProvider));
						return builder.build();
					}
				});			
			}
		});
		
		// otfvis
		if (otfvis) controler.addOverridingModule(new OTFVisLiveModule());	
				
		controler.run();
		
		// some offline analysis
		
		log.info("Running offline analysis...");
				
		final String scenarioCRS = TransformationFactory.DHDN_GK4;	
		final String shapeFileZones = null;
		final String zonesCRS = null;
		final String homeActivity = "home";
		final int scalingFactor = 10;
		
		List<AgentAnalysisFilter> filters = new ArrayList<>();

		AgentAnalysisFilter filter1 = new AgentAnalysisFilter(scenario);
		filter1.setPersonAttribute("berlin");
		filter1.setPersonAttributeName("home-activity-zone");
		filter1.preProcess(scenario);
		filters.add(filter1);
		
		AgentAnalysisFilter filter2 = new AgentAnalysisFilter(scenario);
		filter2.preProcess(scenario);
		filters.add(filter2);
		
		AgentAnalysisFilter filter3 = new AgentAnalysisFilter(scenario);
		filter3.setPersonAttribute("brandenburg");
		filter3.setPersonAttributeName("home-activity-zone");
		filter3.preProcess(scenario);
		filters.add(filter3);

		IKAnalysisRun analysis = new IKAnalysisRun(
				scenario,
				null,
				visualizationScriptInputDirectory,
				scenarioCRS,
				shapeFileZones,
				zonesCRS,
				homeActivity,
				scalingFactor,
				filters,
				null);
		analysis.run();
		
		// noise post-analysis
		
		OptAVConfigGroup optAVParams = ConfigUtils.addOrGetModule(controler.getConfig(), OptAVConfigGroup.class);
		if (optAVParams.isAccountForNoise()) {
			String immissionsDir = controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immissions/";
			String receiverPointsFile = controler.getConfig().controler().getOutputDirectory() + "/receiverPoints/receiverPoints.csv";
				
			NoiseConfigGroup noiseParams = ConfigUtils.addOrGetModule(controler.getConfig(), NoiseConfigGroup.class);

			ProcessNoiseImmissions processNoiseImmissions = new ProcessNoiseImmissions(immissionsDir, receiverPointsFile, noiseParams.getReceiverPointGap());
			processNoiseImmissions.run();
				
			final String[] labels = { "immission", "consideredAgentUnits" , "damages_receiverPoint" };
			final String[] workingDirectories = { controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immissions/" , controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/consideredAgentUnits/" , controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration()  + "/damages_receiverPoint/" };
		
			MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
			merger.setReceiverPointsFile(receiverPointsFile);
			merger.setOutputDirectory(controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/");
			merger.setTimeBinSize(noiseParams.getTimeBinSizeNoiseComputation());
			merger.setWorkingDirectory(workingDirectories);
			merger.setLabel(labels);
			merger.run();
		}
	
		log.info("Done.");
		
	}

}

