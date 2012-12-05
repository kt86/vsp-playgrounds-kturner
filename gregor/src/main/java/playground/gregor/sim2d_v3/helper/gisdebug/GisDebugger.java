/* *********************************************************************** *
 * project: org.matsim.*
 * GisDebugger.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d_v3.helper.gisdebug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GisDebugger {

	private static FeatureType ft;

	private static FeatureType ftLine;

	private static FeatureType ftPoint;

	private static List<Geometry> geos = new ArrayList<Geometry>();

	private static List<String> strs = new ArrayList<String>();

	private static boolean init = false;

	private static String CRS = "EPSG: 3395";

	public static final GeometryFactory geofac = new GeometryFactory();

	public static void setCRSString(String crs) {
		CRS = crs;
	}
	
	public static void addGeometry(Geometry geo) {
		geos.add(geo);
	}

	public static void addGeometry(Geometry geo, String str) {
		addGeometry(geo);
		strs.add(str);

	}

	public static void dump(String file) {
		if (!init) {
			initFeatures();
			init = true;
		}
		Collection<Feature> fts = new  ArrayList<Feature>();
		double d = 0;
		Iterator<String> it = null;
		if (strs.size() == geos.size()) {
			it = strs.iterator();
		}
		for (Geometry geo : geos) {
//			Algorithms.translate(5, 20, geo.getCoordinates());          
			String str = "";
			if (it != null) {
				str = it.next();
			}
			if (geo instanceof MultiPolygon) {
				try {
					fts.add(ft.create(new Object[] {geo,d++,str}));
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}
			} else if (geo instanceof Polygon) {
				MultiPolygon mp = geofac.createMultiPolygon(new Polygon[]{(Polygon) geo});
				try {
					fts.add(ft.create(new Object[] {mp,d++,str}));
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}
			}else if (geo instanceof LineString) {
				try {
					fts.add(ftLine.create(new Object[] {geo,d++,str}));
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}
			} else if (geo instanceof Point) {
				try {
					fts.add(ftPoint.create(new Object[] {geo,d++,str}));
				} catch (IllegalAttributeException e) {
					e.printStackTrace();
				}
			}else {
				throw new RuntimeException("type of Geometry is not supported" + geo);
			}

		}
		ShapeFileWriter.writeGeometries(fts, file);
		geos.clear();
		strs.clear();
	}





	private static void initFeatures() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(CRS);
		AttributeType p = DefaultAttributeTypeFactory.newAttributeType(
				"MultiPolygon", MultiPolygon.class, true, null, null, targetCRS);
		AttributeType l = DefaultAttributeTypeFactory.newAttributeType(
				"LineString", LineString.class, true, null, null, targetCRS);
		AttributeType po = DefaultAttributeTypeFactory.newAttributeType(
				"Point", Point.class, true, null, null, targetCRS);
		AttributeType z = AttributeTypeFactory.newAttributeType(
				"dblAvgZ", Double.class);
		AttributeType t = AttributeTypeFactory.newAttributeType(
				"name", String.class);

		Exception ex;
		try {
			ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { p, z, t }, "MultiPolygon");
			ftLine = FeatureTypeFactory.newFeatureType(new AttributeType[] { l, z, t }, "Line");
			ftPoint = FeatureTypeFactory.newFeatureType(new AttributeType[] { po, z, t }, "Point");
			return;
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);

	}

	public static void addCircle(Coordinate position, double r, String string) {
		
		Coordinate[] circle = new Coordinate[64];
		for (int pos = 0; pos < 63; pos ++) {
			double alpha = pos * ((2*Math.PI) / 64);
			double x = Math.cos(alpha) * (r)+position.x;
			double y = Math.sin(alpha) * (r)+position.y;
			Coordinate c = new Coordinate(x,y);
			circle[pos] = c;
		}
		
		circle[63] = circle[0];
//		Algorithms.translate(position.x, position.y, circle);
		addGeometry(geofac.createLineString(circle), string);
		
	}

}
