package playground.gleich.misc;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunMATSimCodeMadeConfig {

	public static void main(String[] args) {
//		String configFilename = args[0];
//		Config config = ConfigUtils.loadConfig(configFilename);
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v0/optimizedNetwork.xml.gz");
		config.transit().setTransitScheduleFile("/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v1/optimizedSchedule_nonSB-bus-split-at-hubs.xml.gz");
		config.transit().setVehiclesFile("/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v1/optimizedVehicles_nonSB-bus-split-at-hubs.xml.gz");
		config.transit().setUseTransit(true);

		config.controler().setLastIteration(0);


		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new SwissRailRaptorModule());

		controler.run();
	}

}
