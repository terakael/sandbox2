package builder.responses;

public class ResponseFactory {	
	public static Response create(String action) {
		if (action == null)
			return null;
		
		switch (action) {			
		case "load_resources":
			return new LoadResourcesResponse();
		case "load_instances":
			return new LoadInstancesResponse();
		case "move":
			return new MoveResponse();
		case "upsert_ground_texture":
			return new UpsertGroundTextureResponse();
		case "upsert_scenery":
			return new UpsertSceneryResponse();
		case "upsert_npc":
			return new UpsertNpcResponse();
		}		
		return null;
	}
}
