package util;

import jade.core.AID;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import qos.Attribute;
import qos.Dimension;
import qos.Level;
import qos.QoSHandler;
import qos.Value;
import service.Component;
import service.Service;
import service.ServiceHandler;

public class Util {

	public static Service loadServiceFromXML(File xml) {
		try {
			ServiceHandler handler = new ServiceHandler();
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			XMLReader xmlreader = parser.getXMLReader();
			URL url = xml.toURI().toURL();
			xmlreader.setContentHandler(handler);
			InputSource is = new InputSource(url.openStream());
			xmlreader.parse(is);
			return handler.getService();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Level loadLevelFromJSON(String json) {
		try {
			QoSHandler handler = new QoSHandler();
			handler.parse(json);
			return handler.getLevel();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean containsComponent(ArrayList<Component> l1, Component c) {
		for (int i = 0; i < l1.size(); i++) {
			if (l1.get(i).getName().equals(c.getName())) {
				return true;
			}
		}
		return false;
	}

	public static int randomNumber(Random rand, int max, int min) {
		return (rand.nextInt(max - min) + min); // generate
	}

	public static String getComponentNameByAID(HashMap<String, AID> map, AID aid) {
		Set<Entry<String, AID>> set = map.entrySet();
		Iterator<Entry<String, AID>> it = set.iterator();
		while (it.hasNext()) {
			Entry<String, AID> entry = it.next();
			if (entry.getValue().equals(aid)) {
				return entry.getKey();
			}
		}
		return "";
	}

	public static Component getComponentByName(ArrayList<Component> comps,
			String name) {
		for (int i = 0; i < comps.size(); i++) {
			if (comps.get(i).getName().equals(name)) {
				return comps.get(i);
			}
		}
		return null;
	}

	public static String getComponentByAID(HashMap<String, AID> map, AID aid) {
		Set<Entry<String, AID>> set = map.entrySet();
		Iterator<Entry<String, AID>> it = set.iterator();
		while (it.hasNext()) {
			Entry<String, AID> e = (Entry<String, AID>) it.next();
			if (e.getValue().equals(aid)) {
				return e.getKey();
			}
		}
		return "";
	}

	public static boolean containsAgentName(String agentName,
			HashMap<String, AID> agentComponents) {
		ArrayList<AID> aidList = new ArrayList<AID>(agentComponents.values());
		Iterator<AID> aidIt = aidList.iterator();
		while (aidIt.hasNext()) {
			if (aidIt.next().getLocalName().equals(agentName)) {
				return true;
			}
		}
		return false;
	}

	public static List<Component> cloneComponentList(List<Component> list) {
		List<Component> clone = new ArrayList<Component>(list.size());
		try {
			for (Component item : list) {
				clone.add((Component) item.clone());
			}
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return clone;
	}

	public static List<Dimension> cloneDimensionList(List<Dimension> list) {
		List<Dimension> clone = new ArrayList<Dimension>(list.size());
		try {
			for (Dimension item : list) {
				clone.add((Dimension) item.clone());
			}
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return clone;
	}

	public static List<Entry<Attribute, Value>> cloneAttrValList(
			List<Entry<Attribute, Value>> list) {
		List<Entry<Attribute, Value>> clone = new ArrayList<Entry<Attribute, Value>>(
				list.size());
		try {
			for (Entry<Attribute, Value> item : list) {
				clone.add(new util.Entry(item.getKey().clone(), item.getValue()
						.clone()));
			}
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return clone;
	}

}
