/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxi.berlin.demand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import playground.jbischoff.taxi.berlin.data.BeelineDistanceExractor;
import playground.michalm.util.matrices.MatrixUtils;


public class Demand2ShpWriter
{

    /**
     * @param args
     */
    Matrices matrices = MatrixUtils
            .readMatrices("C:/local_jb/data/taxi_berlin/2013/OD/demandMatrices.xml");
    Matrices accmat = new Matrices();
    Scenario scenario;
    private SimpleFeatureBuilder builder;
    private BeelineDistanceExractor bde;
    private GeometryFactory geofac;


    public static void main(String[] args)
    {
        Demand2ShpWriter dsw = new Demand2ShpWriter();
        dsw.accumulate();
        dsw.writeShape("C:/local_jb/data/taxi_berlin/2013/OD/demandLines.shp");

    }


    private void accumulate()
    {
        Matrix sum = accmat.createMatrix("accmat", "accumulated demand");
        for (Matrix matrix : matrices.getMatrices().values()) {
            for (ArrayList<Entry> l : matrix.getFromLocations().values()) {
                for (Entry e : l) {
                    MatrixUtils.setOrIncrementValue(sum, e.getFromLocation(), e.getToLocation(), e.getValue());
                }
            }
        }

    }


    private void writeShape(String outfile)
    {
        Matrix matrix = accmat.getMatrix("accmat");
        Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();

        for (ArrayList<Entry> l : matrix.getFromLocations().values()) {
            for (Entry e : l) {

                LineString ls = this.geofac.createLineString(new Coordinate[] {
                        MGC.coord2Coordinate(bde.getZoneCentroid(e.getFromLocation())),
                        MGC.coord2Coordinate(bde.getZoneCentroid(e.getToLocation())) });
                Object[] attribs = new Object[5];
                attribs[0] = ls;
                attribs[1] = e.getFromLocation().toString();
                attribs[2] = e.getToLocation().toString();
                attribs[3] = e.getValue();
                attribs[4] = 0.1 * e.getValue();
                String ftid = e.getFromLocation().toString()+"_"+e.getToLocation().toString();
                SimpleFeature sf = this.builder.buildFeature(ftid, attribs);
                features.add(sf);
            }

        }
        ShapeFileWriter.writeGeometries(features, outfile);

    }


    public Demand2ShpWriter()
    {
        bde = new BeelineDistanceExractor();
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        this.geofac = new GeometryFactory();
        initFeatureType();
    }


    private void initFeatureType()
    {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("trips");
        typeBuilder.setCRS(MGC.getCRS(TransformationFactory.WGS84_UTM33N));
        typeBuilder.add("location", LineString.class);
        typeBuilder.add("fromId", String.class);
        typeBuilder.add("toId", String.class);
        
        typeBuilder.add("amount", Double.class);
        typeBuilder.add("visWidth", Double.class);

        this.builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
    }

}
