package searchacceleration;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.opdyts.utils.TimeDiscretization;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import searchacceleration.datastructures.SpaceTimeIndicatorVectorListbased;
import searchacceleration.examples.matsimdummy.DummyPSim;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LinkUsageAnalyzer implements IterationStartsListener {

	// -------------------- MEMBERS --------------------

	private final LinkUsageListener physicalMobsimUsageListener;

	private final Set<Id<Person>> replannerIds = new LinkedHashSet<>();

	private final Map<Id<Person>, Plan> personId2newPlan = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public LinkUsageAnalyzer(final LinkUsageListener physicalMobsimLinkUsageListener) {
		this.physicalMobsimUsageListener = physicalMobsimLinkUsageListener;
	}

	// -------------------- FUNCTIONALITY --------------------

	public boolean isAllowedToReplan(final Id<Person> personId) {
		return this.replannerIds.contains(personId);
	}

	public Plan getNewPlan(final Id<Person> personId) {
		return this.personId2newPlan.get(personId);
	}

	// --------------- IMPLEMENTATION OF IterationStartsListener ---------------

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {

		// This data structure represents what happened in the network during
		// the most recent real network loading.
		final Map<Id<Vehicle>, SpaceTimeIndicatorVectorListbased<Id<Link>>> vehId2physicalLinkUsage = this.physicalMobsimUsageListener
				.getAndResetIndicators();

		/*
		 * PSEUDOSIM
		 * 
		 * Let every agent re-plan once. Memorize the new plan.
		 */

		final DummyPSim pSim = new DummyPSim();

		this.personId2newPlan.clear();
		this.personId2newPlan.putAll(pSim.getNewPlanForAllAgents());

		/*
		 * PSEUDOSIM
		 * 
		 * Execute all new plans in the pSim.
		 */

		final LinkUsageListener pSimLinkUsageListener = new LinkUsageListener(
				this.physicalMobsimUsageListener.getTimeDiscretization());
		pSim.executePlans(this.personId2newPlan, pSimLinkUsageListener);

		// This data structure represents what happened in the network during
		// the most recent real network loading.
		final Map<Id<Vehicle>, SpaceTimeIndicatorVectorListbased<Id<Link>>> vehId2pSimLinkUsage = pSimLinkUsageListener
				.getAndResetIndicators();

		/*
		 * At this point, one has (i) the link usage statistics from the
		 * previous MATSim network loading (vehId2physLinkUsage), and (ii) the
		 * hypothetical link usage statistics that would result from a 100%
		 * re-planning rate if network congestion did not change
		 * (vehId2pSimLinkUsage).
		 *
		 * Now, the actual search acceleration is run. It decides which newly
		 * generated plans are selected in the next MATSim iteration.
		 */

		// TODO Insert implementation.

		/*
		 * Now, it needs to be memorized which agents get to switch to their
		 * newly generated plans, and which must stick to their most recently
		 * selected plan. For this, the vehicle-specific link usage information
		 * from the mobility simulation(s) needs to be related to the
		 * person-specific re-planning information, meaning that one somehow
		 * needs to figure out who the drivers of all vehicles were.
		 */

		this.replannerIds.clear();
		// TODO ... and add selected re-planners.

	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		System.out.println("Started ...");

		final Config config = ConfigUtils.loadConfig("./testdata/berlin_2014-08-01_car_1pct/config.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler(scenario);

		final TimeDiscretization timeDiscr = new TimeDiscretization(0, 3600, 24);
		final LinkUsageListener linkUsageListener = new LinkUsageListener(timeDiscr);
		controler.getEvents().addHandler(linkUsageListener);

		final LinkUsageAnalyzer linkUsageAnalyzer = new LinkUsageAnalyzer(linkUsageListener);
		controler.addControlerListener(linkUsageAnalyzer);

		controler.run();

		System.out.println("... done.");
	}
}
