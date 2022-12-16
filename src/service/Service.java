package service;

import jade.util.leap.Serializable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.json.simple.parser.Yytoken;

import qos.Level;

public class Service extends SimpleDirectedWeightedGraph<Component, Connection>
		implements Serializable {

	public Service(Class<? extends Connection> arg0) {
		super(arg0);
	}

	public void setAllMinimumLevel(Level minimumLevel) {
		Set compSet = this.vertexSet();
		Iterator compIterator = (Iterator) compSet.iterator();
		while (compIterator.hasNext()) {
			((Component) compIterator.next()).setMinimumLevel(minimumLevel);
		}
	}

	public void setAllPreferredLevel(Level preferredLevel) {
		Set compSet = this.vertexSet();
		Iterator compIterator = (Iterator) compSet.iterator();
		while (compIterator.hasNext()) {
			((Component) compIterator.next()).setPreferredLevel(preferredLevel);
		}
	}

	public Component getComponentByName(String name) {
		Set compSet = this.vertexSet();
		Iterator compIterator = (Iterator) compSet.iterator();
		Service compGraph = new Service(Connection.class);
		while (compIterator.hasNext()) {
			Component tempComp = (Component) compIterator.next();
			if (tempComp.getName().equals(name)) {
				return tempComp;
			}
		}
		return null;
	}

	public Service getSubServiceByComponentName(String name) {
		Set compSet = this.vertexSet();
		Iterator compIterator = (Iterator) compSet.iterator();
		Service compGraph = new Service(Connection.class);
		Component comp = null;
		while (compIterator.hasNext()) {
			Component tempComp = (Component) compIterator.next();
			if (tempComp.getName().equals(name)) {
				comp = tempComp;
				compGraph.addVertex(comp);
				break;
			}
		}
		Set edgeSet = this.outgoingEdgesOf(comp);
		Iterator edgeIterator = (Iterator) edgeSet.iterator();
		while (edgeIterator.hasNext()) {
			Connection tempConn = (Connection) edgeIterator.next();
			Component destComp = compGraph.getEdgeTarget(tempConn);
			if (destComp != null) {
				compGraph.addVertex(destComp);
				compGraph.addEdge(comp, destComp, tempConn);
			}
		}
		return compGraph;
	}

	public boolean containsComponentByName(String name) {
		Set vertexSet = this.vertexSet();
		Iterator it = (Iterator) vertexSet.iterator();
		while (it.hasNext()) {
			if (((Component) it.next()).getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	public ArrayList<Component> getSuccessorComponents(Component comp) {
		ArrayList<Component> succComps = new ArrayList<Component>();
		Component compAux = this.getComponentByName(comp.getName());
		Set<Connection> succConn = this.outgoingEdgesOf(comp);
		Iterator<Connection> it = succConn.iterator();
		while (it.hasNext()) {
			Connection conn = it.next();
			succComps.add(this.getEdgeTarget(conn));
		}
		return succComps;
	}

	public ArrayList<String> getSuccessorComponentsNames(Component comp) {
		ArrayList<String> succComps = new ArrayList<String>();
		Component compAux = this.getComponentByName(comp.getName());
		Set<Connection> succConn = this.outgoingEdgesOf(comp);
		Iterator<Connection> it = succConn.iterator();
		while (it.hasNext()) {
			Connection conn = it.next();
			succComps.add(this.getEdgeTarget(conn).getName());
		}
		return succComps;
	}

	public int countPredecessorComponents(String name) {
		Component tmpComp = this.getComponentByName(name);
		return this.inDegreeOf(tmpComp);
	}

	public ArrayList<Component> getParallelComponents(Component comp,
			ArrayList<Component> myComponents) {
		ArrayList<Component> parallelComps = new ArrayList<Component>();
		for (int i = 0; i < myComponents.size(); i++) {
			if ((!hasDependence(comp, myComponents.get(i)) && !hasDependence(
					myComponents.get(i), comp))
					&& !comp.getName().equals(myComponents.get(i).getName())) {
				parallelComps.add(myComponents.get(i));
			}
		}
		return parallelComps;
	}

	public int significanceDegreeOf(Component comp) {
		int sD = 0;
		ArrayList<Component> x = new ArrayList<Component>();
		ArrayList<Component> allSucc = getAllSuccessorComponents(
				comp.getName(), x, new ArrayList<String>());
		sD = this.outDegreeOf(this.getComponentByName(comp.getName()));
		for (int i = 0; i < allSucc.size(); i++) {
			sD += this.outDegreeOf(allSucc.get(i));
		}
		return sD;
	}

	public ArrayList<Component> getAllSuccessorComponents(String compName,
			ArrayList<Component> x, ArrayList<String> list) {
		Component comp = getComponentByName(compName);
		Set<Connection> conns = this.outgoingEdgesOf(comp);
		Iterator<Connection> it = conns.iterator();
		if (this.outDegreeOf(comp) > 0) {
			while (it.hasNext()) {
				Connection con = (Connection) it.next();
				Component dest = this.getEdgeTarget(con);
				if (!x.contains(dest)) {
					x.add(dest);
				}
				getAllSuccessorComponents(dest.getName(), x, list);
			}
		}
		return x;
	}

	private boolean hasDependence(Component b, Component a) {
		ArrayList<String> x1 = new ArrayList<String>();
		return (getAllSuccessorComponentsNames(a.getName(), x1,
				new ArrayList<String>()).contains(b.getName()));
	}

	public ArrayList<String> getAllSuccessorComponentsNames(String compName,
			ArrayList<String> x, ArrayList<String> list) {
		Component comp = getComponentByName(compName);
		Set<Connection> conns = this.outgoingEdgesOf(comp);
		Iterator<Connection> it = conns.iterator();
		if (this.outDegreeOf(comp) > 0) {
			while (it.hasNext()) {
				Connection con = (Connection) it.next();
				Component dest = this.getEdgeTarget(con);
				if (!x.contains(dest.getName())) {
					x.add(dest.getName());
				}
				getAllSuccessorComponentsNames(dest.getName(), x, list);
			}
		}
		return x;
	}

	public int sumSignificanceDegreeOf(ArrayList<Component> comps) {
		int sumSD = 0;
		for (int i = 0; i < comps.size(); i++) {
			sumSD += (significanceDegreeOf(comps.get(i)) + 1);
		}
		return sumSD;
	}
}
