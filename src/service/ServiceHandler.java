package service;

import java.util.HashMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * @author Oscar
 */
public class ServiceHandler extends DefaultHandler {

	private final static String EDGE = "connection";
	private final static String DEADLINE = "deadline";
	private final static String ORIGIN = "origin";
	private final static String DESTINATION = "destination";
	private Service service;
	private static String level;
	private static String auxOrigin;
	private static String auxDestination;
	private static Connection auxConn;

	public static HashMap<String, Component> vertexMap;

	public ServiceHandler() {
		super();
		this.service = new Service(Connection.class);
	}

	public Service getService() {
		return service;
	}

	@Override
	public void startDocument() throws SAXException {
		ServiceHandler.level = new String();
		ServiceHandler.auxOrigin = new String();
		ServiceHandler.auxDestination = new String();
		ServiceHandler.auxConn = new Connection();
		vertexMap = new HashMap<String, Component>();
	}

	@Override
	public void endDocument() throws SAXException {
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		if (qName.equals(ORIGIN)) {
			ServiceHandler.level = ORIGIN;
			return;
		}
		if (qName.equals(DESTINATION)) {
			ServiceHandler.level = DESTINATION;
			return;
		}
		if (qName.equals(DEADLINE)) {
			ServiceHandler.level = DEADLINE;
			return;
		}
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		ServiceHandler.level = "";
		if (qName.equals(EDGE)) {
			ServiceHandler.auxConn = new Connection();
			return;
		}
	}

	@Override
	public void characters(char ch[], int start, int length) {
		String data = new String(ch, start, length);
		if (level.equals(ORIGIN)) {
			auxOrigin = data;
			if (!vertexMap.containsKey(auxOrigin)) {
				Component c = new Component(auxOrigin);
				vertexMap.put(auxOrigin, c);
				this.service.addVertex(c);
				return;
			}
		} else if (level.equals(DESTINATION)) {
			auxDestination = data;
			if (!vertexMap.containsKey(auxDestination)) {
				Component c = new Component(auxDestination);
				vertexMap.put(auxDestination, c);
				this.service.addVertex(c);
			}
		} else if (level.equals(DEADLINE)) {
			auxConn.setDeadline(Integer.parseInt(data));
			this.service.addEdge((Component) vertexMap.get(auxOrigin),
					(Component) vertexMap.get(auxDestination), auxConn);
		}
	}
}
