package playground.mzilske.cdr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class BerlinPhoneCongested {

	public static final int[] CALLRATES = new int[]{0, 2, 5, 10, 20, 30, 40, 50, 100, 150};

	public static void main(String[] args) throws FileNotFoundException {
		new BerlinPhoneCongested().run();
	}

	private void run() throws FileNotFoundException {
		final Config baseConfig = ConfigUtils.createConfig();
		new MatsimConfigReader(baseConfig).parse(this.getClass().getResourceAsStream("2kW.15.xml"));
		Scenario scenario = ScenarioUtils.createScenario(baseConfig);
		new MatsimNetworkReader(scenario).readFile(BerlinRun.BERLIN_PATH + "network/bb_4.xml.gz");
		new MatsimPopulationReader(scenario).readFile("/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/car-congested/output-berlin/ITERS/it.200/2kW.15.200.plans.xml.gz");
	//	PrintWriter pw = new PrintWriter(new File("/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/car-congested/quality-over-callrate.txt"));
//		for (int dailyRate : CALLRATES) {
//			run(scenario, dailyRate, pw);
//		}
		runContinuedBasePlans(scenario);
//		runPhoneOnActivityStartEnd(scenario);
//		pw.close();
	}

	private static void run(Scenario scenario, final int dailyRate, PrintWriter pw) {
		EventsManager events = EventsUtils.createEventsManager();
		CompareMain compareMain = new CompareMain(scenario, events, new CallBehavior() {

			@Override
			public boolean makeACall(ActivityEndEvent event) {
				return false;
			}

			@Override
			public boolean makeACall(ActivityStartEvent event) {
				return false;
			}

			@Override
			public boolean makeACall(Id id, double time) {
				double secondlyProbability = dailyRate / (double) (24*60*60);
				return Math.random() < secondlyProbability;
			}

			@Override
			public boolean makeACallAtMorningAndNight() {
				return false;
			}

		});
		new MatsimEventsReader(events).readFile("/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/car-congested/output-berlin/ITERS/it.200/2kW.15.200.events.xml.gz");
		Config config = phoneConfig();

		config.controler().setOutputDirectory("/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/car-congested/output-" + Integer.toString((int) dailyRate));

		compareMain.runOnceWithSimplePlans(config);
	//	pw.printf("%d\t%f\t%f\t%f\n", dailyRate, compareMain.compareAllDay(), compareMain.compareTimebins(), compareMain.compareEMDMassPerLink());
	//	pw.flush();
	}

	private static Config phoneConfig() {
		Config config = ConfigUtils.createConfig();
		ActivityParams sightingParam = new ActivityParams("sighting");
		sightingParam.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(sightingParam);
		config.planCalcScore().setTraveling_utils_hr(-6);
		config.planCalcScore().setWriteExperiencedPlans(true);
		config.controler().setLastIteration(20);
		QSimConfigGroup tmp = config.qsim();
		tmp.setFlowCapFactor(0.02);
		tmp.setStorageCapFactor(0.06);
		tmp.setRemoveStuckVehicles(false);
		tmp.setStuckTime(10.0);
		{
			StrategySettings stratSets = new StrategySettings(new IdImpl(1));
			stratSets.setModuleName("ChangeExpBeta");
			stratSets.setProbability(0.7);
			config.strategy().addStrategySettings(stratSets);
		}
		{
			StrategySettings stratSets = new StrategySettings(new IdImpl(2));
			stratSets.setModuleName("ReRoute");
			stratSets.setProbability(0.3);
			config.strategy().addStrategySettings(stratSets);
		}
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		return config;
	}

	private void runContinuedBasePlans(Scenario baseScenario) {
		Config config = phoneConfig();
		for (ActivityParams params : baseScenario.getConfig().planCalcScore().getActivityParams()) {
			ActivityParams zero = new ActivityParams(params.getType());
			zero.setScoringThisActivityAtAll(false);
			config.planCalcScore().addActivityParams(zero);
		}
		config.controler().setOutputDirectory("/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/car-congested/output-contbaseplans");
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		((ScenarioImpl) scenario).setNetwork(baseScenario.getNetwork());
		
		for (Person basePerson : baseScenario.getPopulation().getPersons().values()) {
			Person person = scenario.getPopulation().getFactory().createPerson(basePerson.getId());
			PlanImpl planImpl = (PlanImpl) scenario.getPopulation().getFactory().createPlan();
			planImpl.copyFrom(basePerson.getSelectedPlan());
			person.addPlan(planImpl);
			scenario.getPopulation().addPerson(person);
		}
		
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);
		controler.run();
	}
	
	private void runPhoneOnActivityStartEnd(Scenario scenario) {
		EventsManager events = EventsUtils.createEventsManager();
		CompareMain compareMain = new CompareMain(scenario, events, new CallBehavior() {

			@Override
			public boolean makeACall(ActivityEndEvent event) {
				return true;
			}

			@Override
			public boolean makeACall(ActivityStartEvent event) {
				return true;
			}

			@Override
			public boolean makeACall(Id id, double time) {
				return false;
			}

			@Override
			public boolean makeACallAtMorningAndNight() {
				return true;
			}

		});
		new MatsimEventsReader(events).readFile("/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/car-congested/output-berlin/ITERS/it.200/2kW.15.200.events.xml.gz");
		
		Config config = phoneConfig();
		config.controler().setOutputDirectory("/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/car-congested/output-actevents");
		compareMain.runOnceWithSimplePlans(config);
	}
	

}
