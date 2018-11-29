/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package gunnar.ihop4.tollzonepassagedata;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.opdyts.buildingblocks.calibration.counting.AbsoluteLinkEntryCountDeviationObjectiveFunction;
import org.matsim.contrib.opdyts.buildingblocks.calibration.counting.CountMeasurementSpecification;
import org.matsim.contrib.opdyts.buildingblocks.calibration.counting.CountMeasurements;
import org.matsim.contrib.opdyts.buildingblocks.calibration.counting.Filter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.TimeDiscretization;
import gunnar.ihop4.IhopConfigGroup;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TollZoneMeasurementReader {

	// -------------------- CONSTANTS --------------------

	private final String pathStr;

	private final Config config;

	private final int maxVehicleLength_m;

	private final TimeDiscretization allDayTimeDiscr;

	private final TimeDiscretization tollTimeOnlyTimeDiscr;

	private final List<String> days;

	// -------------------- MEMBERS --------------------

	private CountMeasurements allDayMeasurements = null;

	private CountMeasurements onlyTollTimeMeasurements = null;

	// -------------------- CONSTRUCTION --------------------

	public TollZoneMeasurementReader(final String path, final Config config, final int maxVehicleLength_m,
			final TimeDiscretization allDayTimeDiscr, final TimeDiscretization tollTimeOnlyTimeDiscr,
			final List<String> days) {
		this.pathStr = path;
		this.config = config;
		this.maxVehicleLength_m = maxVehicleLength_m;
		this.allDayTimeDiscr = allDayTimeDiscr;
		this.tollTimeOnlyTimeDiscr = tollTimeOnlyTimeDiscr;
		this.days = days;
	}

	public TollZoneMeasurementReader(final Config config) {
		// this("./2016-10-xx_passagedata", config, 20, new TimeDiscretization(0, 1800,
		// 48),
		// new TimeDiscretization(6 * 3600 + 30 * 60, 1800, 24),
		// Arrays.asList("2016-10-11", "2016-10-12", "2016-10-13", "2016-10-18",
		// "2016-10-19", "2016-10-20",
		// "2016-10-25", "2016-10-26", "2016-10-27"),
		// simulatedPopulationShare);
		this("/Users/GunnarF/NoBackup/data-workspace/ihop4/2016-10-xx_passagedata", config, 20,
				new TimeDiscretization(0, 1800, 48), new TimeDiscretization(6 * 3600 + 30 * 60, 1800, 24),
				Arrays.asList("2016-10-11", "2016-10-12", "2016-10-13", "2016-10-18", "2016-10-19", "2016-10-20",
						"2016-10-25", "2016-10-26", "2016-10-27"));
	}

	// -------------------- IMPLEMENTATION --------------------

	private void replaceByInterpolation(final DynamicData<String> data, final int interpolateTime_s) {
		int bin = data.bin(interpolateTime_s);
		for (String key : data.keySet()) {
			final double prevValue = data.getBinValue(key, bin - 1);
			final double nextValue = data.getBinValue(key, bin + 1);
			data.put(key, bin, 0.5 * (prevValue + nextValue));
		}
	}

	private Set<String> keysWithDataOutsideOfTollTime(final DynamicData<String> data, final int tollStart_s,
			final int tollEnd_s) {
		final Set<String> result = new LinkedHashSet<>();
		for (int bin = 0; bin < data.bin(tollStart_s); bin++) {
			for (String key : data.keySet()) {
				if (data.getBinValue(key, bin) > 0) {
					result.add(key);
				}
			}
		}
		for (int bin = data.bin(tollEnd_s) + 1; bin < data.getBinCnt(); bin++) {
			for (String key : data.keySet()) {
				if (data.getBinValue(key, bin) > 0) {
					result.add(key);
				}
			}
		}
		return result;
	}

	public void run() {

		final List<String> files = new ArrayList<>(this.days.size() * 2);
		for (String day : this.days) {
			for (String postfix : new String[] { "-01", "-02" }) {
				final Path path = Paths.get(this.pathStr, "wsp-passages-vtr-" + day + postfix + ".csv");
				files.add(path.toString());
			}
		}

		final SizeAnalyzer sizeAnalyzer = new SizeAnalyzer(this.maxVehicleLength_m);
		for (String file : files) {
			System.out.println("size analysis: " + file);
			sizeAnalyzer.parse(file);
		}
		System.out.println(sizeAnalyzer.toString());

		final PassageDataAnalyzer dataAnalyzer = new PassageDataAnalyzer(this.allDayTimeDiscr,
				sizeAnalyzer.getRelevanceProbabilityPerMeterLengthClass());
		for (String file : files) {
			System.out.println("passage analysis: " + file);
			dataAnalyzer.parse(file);
		}
		System.out.println(dataAnalyzer.toString());

		replaceByInterpolation(dataAnalyzer.getData(), 6 * 3600 + 15 * 60);
		replaceByInterpolation(dataAnalyzer.getData(), 18 * 3600 + 45 * 60);
		System.out.println(dataAnalyzer.toString());

		// CREATION OF ACTUAL SENSOR DATA

		final double simulatedPopulationShare = ConfigUtils.addOrGetModule(this.config, IhopConfigGroup.class).getSimulatedPopulationShare();
		this.allDayMeasurements = new CountMeasurements(simulatedPopulationShare);
		this.onlyTollTimeMeasurements = new CountMeasurements(simulatedPopulationShare);

		final DynamicData<String> allData = dataAnalyzer.getData();
		final Set<String> linksWithDataOutsideOfTollTime = keysWithDataOutsideOfTollTime(allData, 6 * 3600, 19 * 3600);

		for (String linkStr : allData.keySet()) {

			final TimeDiscretization singleSensorTimeDiscr;
			if (linksWithDataOutsideOfTollTime.contains(linkStr)) {
				singleSensorTimeDiscr = this.allDayTimeDiscr;
			} else {
				singleSensorTimeDiscr = this.tollTimeOnlyTimeDiscr;
			}

			final CountMeasurementSpecification spec = new CountMeasurementSpecification(singleSensorTimeDiscr,
					new Filter<Id<Vehicle>>() {
						@Override
						public boolean test(Id<Vehicle> arg0) {
							return true;
						}
					}, new LinkedHashSet<Id<Link>>(Arrays.asList(Id.createLinkId(linkStr))));

			final double[] singleSensorData = new double[singleSensorTimeDiscr.getBinCnt()];
			for (int singleSensorTimeBin = 0; singleSensorTimeBin < singleSensorTimeDiscr
					.getBinCnt(); singleSensorTimeBin++) {
				final int allDayTimeBin = this.allDayTimeDiscr
						.getBin(singleSensorTimeDiscr.getBinCenterTime_s(singleSensorTimeBin));
				singleSensorData[singleSensorTimeBin] = allData.getBinValue(linkStr, allDayTimeBin);
			}

			if (linksWithDataOutsideOfTollTime.contains(linkStr)) {
				this.allDayMeasurements.addMeasurement(spec, singleSensorData);
			} else {
				this.onlyTollTimeMeasurements.addMeasurement(spec, singleSensorData);
			}
		}

		this.allDayMeasurements.build();
		this.onlyTollTimeMeasurements.build();

		System.out.println("\nALL-DAY SENSORS");
		for (AbsoluteLinkEntryCountDeviationObjectiveFunction objFct : this.allDayMeasurements
				.getObjectiveFunctions()) {
			System.out.print(objFct.getSpecification().getLinks() + ": "
					+ objFct.getSpecification().getTimeDiscretization() + ": ");
			for (double val : objFct.getRealData()) {
				System.out.print(val + " ");
			}
			System.out.println();
		}

		System.out.println("\nONLY-TOLL SENSORS");
		for (AbsoluteLinkEntryCountDeviationObjectiveFunction objFct : this.onlyTollTimeMeasurements
				.getObjectiveFunctions()) {
			System.out.print(objFct.getSpecification().getLinks() + ": "
					+ objFct.getSpecification().getTimeDiscretization() + ": ");
			for (double val : objFct.getRealData()) {
				System.out.print(val + " ");
			}
			System.out.println();
		}
	}

	public CountMeasurements getAllDayMeasurements() {
		return this.allDayMeasurements;
	}

	public CountMeasurements getOnlyTollTimeMeasurements() {
		return this.onlyTollTimeMeasurements;
	}
}
