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

package playground.agarwalamit.nemo.calibration.locationChoice;

import playground.agarwalamit.parametricRuns.PrepareParametricRuns;

/**
 * Created by amit.
 */

public class ParametricRunsNEMOLocationChoice {

    public static void main(String[] args) {
        int runCounter= 1;

        String baseOutDir = "/net/ils4/agarwal/nemo/locationChoice/output/";
        String matsimDir = "r_5a207c4ba06fa4017620044f18a82170122eacf4_patnaOpdyts_26Oct";

        StringBuilder buffer = new StringBuilder();
        PrepareParametricRuns parametricRuns = new PrepareParametricRuns();

        double [] flowCaps = {0.01};
        double [] storageCaps = {0.015, 0.02};
        double [] cadytsWts = {0.15};
        Integer [] lastIts = {200, 300};

        buffer.append("runNr\tplansFile\tnetworkFile\tflowCapacityFactor\tstorageCapacityFactor\tcadytsWt\tlastIteration"+ PrepareParametricRuns.newLine);

        for (double flowCap : flowCaps ) {
            for(double storageCap :storageCaps){
                for (double cadytsWt : cadytsWts) {
                    for (double lastIt : lastIts) {

                        String configFile = "/net/ils4/agarwal/nemo/data/locationChoice/input/config.xml";
                        String plansFile = "/net/ils4/agarwal/nemo/data/input/matsim_initial_plans/nrw_plans_1pct.xml.gz";
                        String networkFile = "/net/ils4/agarwal/nemo/data/input/network/allWaysNRW/tertiaryNemo_Network_31102017filteredcleaned_network.xml.gz";
                        String jobName = "run"+String.valueOf(runCounter++);
                        String outputDir = baseOutDir+"/"+jobName+"/";

                        String params = configFile + " "+ plansFile + " " + networkFile + " " + outputDir + " " + jobName + " "+flowCap + " " + storageCap + " "+ lastIt +" " +cadytsWt;

                        String [] additionalLines = {
                                "echo \"========================\"",
                                "echo \" "+matsimDir+" \" ",
                                "echo \"========================\"",
                                PrepareParametricRuns.newLine,

                                "cd /net/ils4/agarwal/matsim/"+matsimDir+"/",
                                PrepareParametricRuns.newLine,

                                "java -Djava.awt.headless=true -Xmx29G -cp agarwalamit-0.10.0-SNAPSHOT.jar " +
                                        "org/matsim/scenarioCreation/locationChoice/NemoLocationChoiceCalibration " +
                                        params+" "
                        };

                        parametricRuns.run(additionalLines, baseOutDir, jobName);

                        buffer.append(jobName+"\t" + params.replace(' ','\t') + PrepareParametricRuns.newLine);
                    }
                }
            }
        }

        parametricRuns.writeNewOrAppendRemoteFile(buffer, baseOutDir+"/runInfo.txt");
        parametricRuns.close();
    }

}
