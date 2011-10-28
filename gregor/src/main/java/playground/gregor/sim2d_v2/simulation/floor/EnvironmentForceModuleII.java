package playground.gregor.sim2d_v2.simulation.floor;

import java.util.ArrayList;
import java.util.List;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;

import playground.gregor.sim2d_v2.helper.DenseMultiPointFromGeometries;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;

public class EnvironmentForceModuleII implements ForceModule {

	private final Scenario sc;

	private QuadTree<Coordinate> quad;

	//Helbing constant
	private static final double Bi=0.08;
	private static final double Ai=2000;
	private static final double k = 1.2 * 100000;
	private static final double kappa = 2.4 * 100000;



	/**
	 * @param floor
	 * @param scenario
	 */
	public EnvironmentForceModuleII(Floor floor, Scenario scenario) {
		this.sc = scenario;
		//sensing range to maximum
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.gregor.sim2_v2.simulation.floor.ForceModule#run(playground
	 * .gregor.sim2_v2.simulation.Agent2D)
	 */
	@Override
	public void run(Agent2D agent) {
		double fx = 0;
		double fy = 0;

		Coordinate obj = this.quad.get(agent.getPosition().x, agent.getPosition().y);
		double dist = obj.distance(agent.getPosition());
		if (dist > 5) {
			return;
		}

		double dx =(agent.getPosition().x - obj.x) / dist;
		double dy =(agent.getPosition().y - obj.y) / dist;

		double bounderyDist = Agent2D.AGENT_DIAMETER/2 - dist;
		double g = bounderyDist > 0 ? bounderyDist : 0;
		double tanDvx = (- agent.getVx()) * dx;
		double tanDvy = (- agent.getVy()) * dy;

		double tanX = tanDvx * -dx;
		double tanY = tanDvy * dy;

		double xc = (Ai * Math.exp((bounderyDist) / Bi) + k*g)* dx+ kappa * g * tanX;
		double yc = (Ai * Math.exp((bounderyDist) / Bi) + k*g)* dy + kappa * g * tanY;

		fx += xc;
		fy += yc;



		agent.getForce().incrementX(fx);
		agent.getForce().incrementY(fy);

	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.gregor.sim2d_v2.simulation.floor.ForceModule#init()
	 */
	@Override
	public void init() {
		ShapeFileReader reader = this.sc.getScenarioElement(ShapeFileReader.class);
		Envelope e = reader.getBounds();
		this.quad = new QuadTree<Coordinate>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());

		List<Geometry> geos = new ArrayList<Geometry>();
		for (Feature ft : reader.getFeatureSet()) {
			Geometry geo = ft.getDefaultGeometry();
			geos.add(geo);
		}
		DenseMultiPointFromGeometries dmp = new DenseMultiPointFromGeometries();
		MultiPoint mp = dmp.getDenseMultiPointFromGeometryCollection(geos);
		for (int i = 0; i < mp.getNumPoints(); i++) {
			Point p = (Point) mp.getGeometryN(i);
			this.quad.put(p.getX(), p.getY(), p.getCoordinate());
		}
	}
}
