package builder.requests;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class RequestFactory {
	private static Gson gson = new Gson();
	private static Map<String, Class<? extends Request>> map = new HashMap<>();
	static {
		map.put("load_resources", LoadResourcesRequest.class);
		map.put("load_instances", LoadInstancesRequest.class);
		map.put("move", MoveRequest.class);
		map.put("upsert_ground_texture", UpsertGroundTextureRequest.class);
		map.put("upsert_scenery", UpsertSceneryRequest.class);
		map.put("upsert_npc", UpsertNpcRequest.class);
	}
	public static Request create(String action, String jsonText) {
		if (map.containsKey(action))
			return (Request) gson.fromJson(jsonText, map.get(action));
		return null;
	}
}