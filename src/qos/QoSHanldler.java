package qos;

import java.io.InputStream;

import net.sf.json.JSONSerializer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class QoSHanldler {

	private static Level level;

	private static final String DIMENSIONS = "dimensions";
	private static final String NAME = "name";

	public QoSHanldler() {
		level = new Level("");
	}

	public void parse(String json) {
		try {
			InputStream is = QoSHanldler.class.getResourceAsStream(json);
			String jsonTxt = is.toString();

			JSONObject jsonObj = (JSONObject) JSONSerializer.toJSON(jsonTxt);

			JSONArray jsonDimensions = jsonObj.getJSONArray(DIMENSIONS);

			for (int i = 0; i < jsonDimensions.length(); i++) {
				JSONObject jsonDim = jsonDimensions.getJSONObject(i);
				String dimName = jsonDim.getString(NAME);
				level.addDimension(new Dimension(dimName));
			}
			// TODO: acabar handler de ficheiro .json
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public Level getLevel() {
		return level;
	}
}
