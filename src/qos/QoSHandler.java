package qos;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import util.Entry;

public class QoSHandler {

	private static Level level;

	private static final String NAME = "name";
	private static final String QOS = "qos";
	private static final String DIMENSIONS = "dimensions";
	private static final String DIMENSION = "dimension";
	private static final String ATTRIBUTES = "attributes";
	private static final String ATTRIBUTE = "attribute";
	private static final String VALUES = "values";
	private static final String DOMAIN = "domain";

	public QoSHandler() {
	}

	public <T> void parse(String jsonFile) {

		try {
			level = new Level("");

			JSONParser parser = new JSONParser();
			Object obj = parser.parse(new FileReader(jsonFile));
			JSONObject jsonObject = (JSONObject) obj;
			JSONObject jsonQoS = (JSONObject) jsonObject.get(QOS);

			level.setName((String) jsonQoS.get(NAME));
			JSONArray jsonDimensions = (JSONArray) jsonQoS.get(DIMENSIONS);

			for (int i = 0; i < jsonDimensions.size(); i++) {
				JSONObject jsonDim = (JSONObject) ((JSONObject) jsonDimensions
						.get(i)).get(DIMENSION);
				String dimName = (String) jsonDim.get(NAME);
				JSONArray jsonAttributes = (JSONArray) jsonDim.get(ATTRIBUTES);
				Dimension dim = new Dimension(dimName);
				ArrayList<Entry<Attribute, Value>> dimAttr = new ArrayList<Entry<Attribute, Value>>();
				for (int j = 0; j < jsonAttributes.size(); j++) {
					JSONObject jsonAttr = (JSONObject) ((JSONObject) jsonAttributes
							.get(j)).get(ATTRIBUTE);
					String attrName = (String) jsonAttr.get(NAME);
					String attrType = (String) jsonAttr.get(DOMAIN);
					JSONArray jsonValues = (JSONArray) jsonAttr.get(VALUES);
					Attribute<T> attr = new Attribute<T>(attrName, attrType);
					for (int k = 0; k < jsonValues.size(); k++) {
						T jsonValue = (T) jsonValues.get(k);
						Value<String, T> value = new Value<String, T>(attrName,
								jsonValue);
						attr.addPossibleValue(value.getValue());
					}
					dimAttr.add(new Entry<Attribute, Value>(attr,
							new Value<String, T>(attr.getName())));
				}
				dim.setAttributeValue(dimAttr);
				level.addDimension(dim);
			}

		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Level getLevel() {
		return level;
	}
}
