/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.sergioo.NetworkBusLaneAdder.gui;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.AStarLandmarks;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;

import playground.sergioo.NetworkBusLaneAdder.gui.BusLaneAdderWindow.Labels;
import playground.sergioo.NetworkBusLaneAdder.gui.BusLaneAdderWindow.Options;
import playground.sergioo.Visualizer2D.Layer;
import playground.sergioo.Visualizer2D.LayersPanel;
import playground.sergioo.Visualizer2D.LinesPainter;
import playground.sergioo.Visualizer2D.NetworkVisualizer.ImagePainter;
import playground.sergioo.Visualizer2D.NetworkVisualizer.NetworkPainters.NetworkPainter;

public class BusLaneAdderPanel extends LayersPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final double BUS_SPEED = 16/3.6;
	
	//Attributes
	private final BusLaneAdderWindow busLaneAdderWindow;
	private int iniX;
	private int iniY;
	private AStarLandmarks aStarLandmarks;
	
	//Methods
	public BusLaneAdderPanel(BusLaneAdderWindow busLaneAdderWindow, NetworkPainter networkPainter, File imageFile, Coord upLeft, Coord downRight) throws IOException {
		super();
		this.busLaneAdderWindow = busLaneAdderWindow;
		ImagePainter imagePainter = new ImagePainter(imageFile, this);
		imagePainter.setImageCoordinates(upLeft, downRight);
		addLayer(new Layer(imagePainter, false));
		addLayer(new Layer(networkPainter), true);
		addLayer(new Layer(new LinesPainter(), false));
		this.setBackground(backgroundColor);
		calculateBoundaries();
		super.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize().width,Toolkit.getDefaultToolkit().getScreenSize().height);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		setFocusable(true);
		TravelMinCost travelMinCost = new TravelMinCost() {
			public double getLinkGeneralizedTravelCost(Link link, double time) {
				return getLinkMinimumTravelCost(link);
			}
			public double getLinkMinimumTravelCost(Link link) {
				if(link.getAllowedModes().contains("bus"))
					return link.getLength()/BUS_SPEED;
				else
					return Double.MAX_VALUE;
			}
		};
		TravelTime timeFunction = new TravelTime() {	
			public double getLinkTravelTime(Link link, double time) {
				if(link.getAllowedModes().contains("bus"))
					return link.getLength()/BUS_SPEED;
				else
					return Double.MAX_VALUE;
			}
		};
		PreProcessLandmarks preProcessData = new PreProcessLandmarks(travelMinCost);
		preProcessData.run(busLaneAdderWindow.getNetwork());
		aStarLandmarks = new AStarLandmarks(busLaneAdderWindow.getNetwork(), preProcessData, timeFunction);
	}
	private void calculateBoundaries() {
		Collection<Coord> coords = new ArrayList<Coord>();
		for(Link link:((NetworkPainter)getPrincipalLayer().getPainter()).getNetwork().getLinks().values()) {
			if(link!=null) {
				coords.add(link.getFromNode().getCoord());
				coords.add(link.getToNode().getCoord());
			}
		}
		super.calculateBoundaries(coords);
	}
	public void clearNodesSelection() {
		((NetworkTwoNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).clearNodesSelection();
	}
	public void selectLinks() {
		((NetworkTwoNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).selectLinks(aStarLandmarks);
	}
	public List<Link> getLinks() {
		return ((NetworkTwoNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).getSelectedLinks();
	}
	public String getLabelText(playground.sergioo.Visualizer2D.LayersWindow.Labels label) {
		try {
			return (String) NetworkTwoNodesPainterManager.class.getMethod("refresh"+label.getText(), new Class[0]).invoke(((NetworkPainter)getPrincipalLayer().getPainter()).getNetworkPainterManager(), new Object[0]);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return "";
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		if(e.getClickCount()==2 && e.getButton()==MouseEvent.BUTTON3)
			camera.centerCamera(getWorldX(e.getX()), getWorldY(e.getY()));
		else {
			if(busLaneAdderWindow.getOption().equals(Options.SELECT_NODES) && e.getButton()==MouseEvent.BUTTON1) {
				((NetworkTwoNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).selectNearestNode(getWorldX(e.getX()),getWorldY(e.getY()));
				busLaneAdderWindow.refreshLabel(Labels.NODES);
			}
			else if(busLaneAdderWindow.getOption().equals(Options.SELECT_NODES) && e.getButton()==MouseEvent.BUTTON3) {
				((NetworkTwoNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).unselectNearestNode(getWorldX(e.getX()),getWorldY(e.getY()));
				busLaneAdderWindow.refreshLabel(Labels.NODES);
			}
			else if(busLaneAdderWindow.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON1) {
				camera.zoomIn(getWorldX(e.getX()), getWorldY(e.getY()));
			}
			else if(busLaneAdderWindow.getOption().equals(Options.ZOOM) && e.getButton()==MouseEvent.BUTTON3) {
				camera.zoomOut(getWorldX(e.getX()), getWorldY(e.getY()));
			}
		}
		repaint();
	}
	@Override
	public void mousePressed(MouseEvent e) {
		this.requestFocus();
		iniX = e.getX();
		iniY = e.getY();
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		
	}
	@Override
	public void mouseEntered(MouseEvent e) {
		
	}
	@Override
	public void mouseExited(MouseEvent e) {
		
	}
	@Override
	public void mouseDragged(MouseEvent e) {
		camera.move(getWorldX(iniX)-getWorldX(e.getX()),getWorldY(iniY)-getWorldY(e.getY()));
		iniX = e.getX();
		iniY = e.getY();
		repaint();
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		busLaneAdderWindow.setCoords(getWorldX(e.getX()),getWorldY(e.getY()));
	}
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		this.requestFocus();
		if(e.getWheelRotation()<0)
			camera.zoomIn();
		else if(e.getWheelRotation()>0)
			camera.zoomOut();
		repaint();
	}
	@Override
	public void keyTyped(KeyEvent e) {
		switch(e.getKeyChar()) {
		case 'v':
			viewAll();
			break;
		}
		repaint();
	}
	@Override
	public void keyPressed(KeyEvent e) {
		switch(e.getKeyCode()) {
		case KeyEvent.VK_DELETE:
			((NetworkTwoNodesPainterManager)((NetworkPainter)getActiveLayer().getPainter()).getNetworkPainterManager()).clearNodesSelection();
			break;
		}
		busLaneAdderWindow.setVisible(true);
		busLaneAdderWindow.repaint();
	}
	@Override
	public void keyReleased(KeyEvent e) {
		
	}
	
}
