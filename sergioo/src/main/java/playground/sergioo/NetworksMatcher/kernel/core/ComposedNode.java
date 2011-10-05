package playground.sergioo.NetworksMatcher.kernel.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;



public class ComposedNode implements Node, Cloneable {


	//Constants

	public static final String SEPARATOR = "-";


	//Enums

	public enum Types {
		EMPTY,
		SOURCE,
		SINK,
		DEAD_END,
		ONE_WAY_PASS,
		TWO_WAY_PASS,
		ONE_WAY_START,
		ONE_WAY_END,
		CROSSING;
	}
	
	
	//Static attributes
	
	public static double radius;

	//Attributes

	private Coord coord;

	private final Id id;

	private Map<Id, Link> inLinks;

	private Map<Id, Link> outLinks;
	
	private final Set<Node> nodes;
	
	private ComposedNode containerNode;
	
	private Types type;

	//Methods
	
	public ComposedNode(Node node) {
		id = new IdImpl(node.getId().toString());
		coord = new CoordImpl(node.getCoord().getX(), node.getCoord().getY());
		inLinks = new HashMap<Id, Link>();
		outLinks = new HashMap<Id, Link>();
		nodes = new HashSet<Node>();
	}
	
	public ComposedNode(Set<Node> nodes) {
		String idText = "";
		for(Node node:nodes)
			idText+=node.getId()+SEPARATOR;
		idText=idText.substring(0, idText.length()-1);
		id = new IdImpl(idText);
		coord = new CoordImpl(0, 0);
		for(Node node:nodes)
			coord.setXY(coord.getX()+node.getCoord().getX(), coord.getY()+node.getCoord().getY());
		coord.setXY(coord.getX()/nodes.size(), coord.getY()/nodes.size());
		this.nodes = nodes;
		inLinks = new HashMap<Id, Link>();
		outLinks = new HashMap<Id, Link>();
		setIncidentLinks();
		setType();
		for(Node node:nodes) {
			if(node.getClass().equals(ComposedNode.class))
				((ComposedNode)node).setContainerNode(this);
		}
	}
	
	public List<Link> getInLinksList() {
		List<Link> inLinksList = new ArrayList<Link>(inLinks.values());
		for(int i=0; i<inLinksList.size()-1; i++)
			for(int j=i+1; j<inLinksList.size(); j++)
				if(((ComposedLink)inLinksList.get(i)).getAngle()>((ComposedLink)inLinksList.get(j)).getAngle()) {
					ComposedLink temporalLink = (ComposedLink)inLinksList.get(i);
					inLinksList.set(i, inLinksList.get(j));
					inLinksList.set(j, temporalLink);
				}
		return inLinksList;
	}

	public List<Link> getOutLinksList() {
		List<Link> outLinksList = new ArrayList<Link>(outLinks.values());
		for(int i=0; i<outLinksList.size()-1; i++)
			for(int j=i+1; j<outLinksList.size(); j++)
				if(((ComposedLink)outLinksList.get(i)).getAngle()>((ComposedLink)outLinksList.get(j)).getAngle()) {
					ComposedLink temporalLink = (ComposedLink)outLinksList.get(i);
					outLinksList.set(i, outLinksList.get(j));
					outLinksList.set(j, temporalLink);
				}
		return outLinksList;
	}
	
	public void setType(Types type) {
		this.type = type;
	}
	
	public void setType() {
		if(inLinks.values().size() == 0 && outLinks.values().size()==0)
			type = Types.EMPTY;
		else if(inLinks.values().size() == 0)
			type = Types.SOURCE;
		else if(outLinks.values().size() == 0)
			type = Types.SINK;
		else if(inLinks.values().size() == 1 && outLinks.values().size() == 1 && inLinks.values().iterator().next().getFromNode().equals(outLinks.values().iterator().next().getToNode()))
			type = Types.DEAD_END;
		else if(inLinks.values().size() == 1 && outLinks.values().size() == 1)
			type = Types.ONE_WAY_PASS;
		else if(inLinks.values().size() == 2 && outLinks.values().size() == 1) {
			Iterator<Link> inLinksIterator = inLinks.values().iterator();
			Link firstInLink = inLinksIterator.next();
			Link secondInLink = inLinksIterator.next();
			if(outLinks.values().iterator().next().getToNode().equals(firstInLink.getFromNode()) || outLinks.values().iterator().next().getToNode().equals(secondInLink.getFromNode()))
				type = Types.ONE_WAY_END;
			else
				type = Types.CROSSING;
		}
		else if(inLinks.values().size() == 1 && outLinks.values().size() == 2) {
			Iterator<Link> outLinksIterator = outLinks.values().iterator();
			Link firstOutLink = outLinksIterator.next();
			Link secondOutLink = outLinksIterator.next();
			if(inLinks.values().iterator().next().getFromNode().equals(firstOutLink.getToNode()) || inLinks.values().iterator().next().getFromNode().equals(secondOutLink.getToNode()))
				type = Types.ONE_WAY_START;
			else
				type = Types.CROSSING;
		}
		else if(inLinks.values().size() == 2 && outLinks.values().size() == 2) {
			Iterator<Link> inLinksIterator = inLinks.values().iterator();
			Link firstInLink = inLinksIterator.next();
			Link secondInLink = inLinksIterator.next();
			Iterator<Link> outLinksIterator = outLinks.values().iterator();
			Link firstOutLink = outLinksIterator.next();
			Link secondOutLink = outLinksIterator.next();
			if((firstInLink.getFromNode().equals(firstOutLink.getToNode()) && secondInLink.getFromNode().equals(secondOutLink.getToNode())) || (firstInLink.getFromNode().equals(secondOutLink.getToNode()) && secondInLink.getFromNode().equals(firstOutLink.getToNode())))
				type = Types.TWO_WAY_PASS;
			else
				type = Types.CROSSING;
		}
		else
			type = Types.CROSSING;
	}
	
	private void setIncidentLinks() {
		for(Node node:nodes) {
			for(Link link:node.getInLinks().values()) {
				boolean insideLink = false;
				for(Node node2:nodes)
					if(link.getFromNode().getId().equals(node2.getId()))
						insideLink = true;
				if(!insideLink)
					addInLink(link);
			}
			for(Link link:node.getOutLinks().values()) {
				boolean insideLink = false;
				for(Node node2:nodes)
					if(link.getToNode().getId().equals(node2.getId()))
						insideLink = true;
				if(!insideLink)
					addOutLink(link);
			}			
		}
	}
	
	public boolean isConnected() {
		Set<Node> connectedNodes = new HashSet<Node>();
		fillNodes(nodes.iterator().next(), connectedNodes);
		return nodes.size()==connectedNodes.size();
	}
	
	private void fillNodes(Node node, Set<Node> connectedNodes) {
		connectedNodes.add(node);
		for(Link link:node.getInLinks().values())
			if(nodes.contains(link.getToNode()) && !connectedNodes.contains(link.getToNode()))
				fillNodes(link.getFromNode(), connectedNodes);
		for(Link link:node.getOutLinks().values())
			if(nodes.contains(link.getToNode()) && !connectedNodes.contains(link.getToNode()))
				fillNodes(link.getToNode(), connectedNodes);
	}
	
	public ComposedNode getContainerNode() {
		return containerNode;
	}


	public void setContainerNode(ComposedNode containerNode) {
		this.containerNode = containerNode;
	}


	@Override
	public Coord getCoord() {
		return coord;
	}

	@Override
	public Id getId() {
		return id;
	}

	@Override
	public boolean addInLink(Link link) {
		return inLinks.put(link.getId(), link)!=null;
	}

	@Override
	public boolean addOutLink(Link link) {
		return outLinks.put(link.getId(), link)!=null;
	}

	@Override
	public Map<Id, ? extends Link> getInLinks() {
		return inLinks;
	}

	@Override
	public Map<Id, ? extends Link> getOutLinks() {
		return outLinks;
	}
	
	public Set<Node> getNodes() {
		return nodes;
	}

	public Types getType() {
		return type;
	}

}
