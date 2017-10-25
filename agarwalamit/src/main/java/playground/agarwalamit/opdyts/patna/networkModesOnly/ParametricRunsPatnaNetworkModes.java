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

package playground.agarwalamit.opdyts.patna.networkModesOnly;

import playground.agarwalamit.opdyts.PreapareParametricRuns;

/**
 * A class to create a job script, write it on remote and then run the job based on the given parameters.
 *
 * Created by amit on 04.10.17.
 */

public class ParametricRunsPatnaNetworkModes {

    public static void main(String[] args) {
        int runCounter= 201;

        String baseOutDir = "/net/ils4/agarwal/patnaOpdyts/networkModes/calibration/output/";
        String matsimDir = "r_87b4237ac664bb1068965f2e8797e6bd3cfa7e1f_patnaOpdyts_25Oct";

        StringBuilder buffer = new StringBuilder();
        PreapareParametricRuns parametricRuns = new PreapareParametricRuns();

        String ascStyles [] = {"axial_randomVariation","diagonal_randomVariation","axial_fixed","diagonal_fixed"};
        double [] stepSizes = {0.5, 0.75, 1.0};
        Integer [] convIterations = {600};
        double [] selfTuningWts = {1.0};
        Integer [] warmUpIts = {1, 5, 10};

        buffer.append("runNr\tascStyle\tstepSize\titerations2Convergence\tselfTunerWt\twarmUpIts"+ PreapareParametricRuns.newLine);

        for (String ascStyle : ascStyles ) {
            for(double stepSize :stepSizes){
                for (int conIts : convIterations) {
                    for (double selfTunWt : selfTuningWts) {
                        for (int warmUpIt : warmUpIts) {

                            String jobName = "run"+String.valueOf(runCounter++);
                            String params= ascStyle + " "+ stepSize + " " + conIts + " " + selfTunWt + " " + warmUpIt;

                            String [] additionalLines = {
                                    "echo \"========================\"",
                                    "echo \" "+matsimDir+" \" ",
                                    "echo \"========================\"",
                                    PreapareParametricRuns.newLine,

                                    "cd /net/ils4/agarwal/matsim/"+matsimDir+"/",
                                    PreapareParametricRuns.newLine,

                                    "java -Djava.awt.headless=true -Xmx58G -cp agarwalamit-0.10.0-SNAPSHOT.jar " +
                                            "playground/agarwalamit/opdyts/patna/networkModesOnly/PatnaNetworkModesOpdytsCalibrator " +
                                            "/net/ils4/agarwal/patnaOpdyts/networkModes/calibration/inputs/config_networkModesOnly.xml " +
                                            "/net/ils4/agarwal/patnaOpdyts/networkModes/calibration/output/"+jobName+"/ " +
                                            "/net/ils4/agarwal/patnaOpdyts/networkModes/relaxedPlans/output/output_plans.xml.gz "+
                                            params+" "
                            };

                            parametricRuns.run(additionalLines, baseOutDir, jobName);
                            buffer.append(jobName+"\t" + params.replace(' ','\t') + PreapareParametricRuns.newLine);
                        }
                    }
                }
            }
        }

        parametricRuns.writeNewOrAppendRemoteFile(buffer, baseOutDir+"/runInfo.txt");
        parametricRuns.close();
    }

}
