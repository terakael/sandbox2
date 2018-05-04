package sandbox2.requests;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class RequestFactory {
	private static Gson gson = new Gson();
	private static Map<String, Class<?>> map = new HashMap<>();
	static {
		map.put("logon", LogonRequest.class);
		map.put("move", MoveRequest.class);
	}
	public static Request create(String action, String jsonText) {
		if (map.containsKey(action))
			return (Request) gson.fromJson(jsonText, map.get(action));
		return new UnknownRequest();
	}
}
