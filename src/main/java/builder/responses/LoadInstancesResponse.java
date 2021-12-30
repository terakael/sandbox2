package builder.responses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import builder.managers.BuildManager;
import builder.requests.Request;
import database.dao.GroundTextureDao;
import database.dao.NPCDao;
import database.dao.SceneryDao;
import utils.Utils;

public class LoadInstancesResponse extends Response {
	private Map<Integer, Set<Integer>> groundTextures; // groundTextureId, <tileIds>
	private Map<Integer, Set<Integer>> scenery; // <sceneryId, <tileIds>>
	private Map<Integer, Set<Integer>> npcs; // <npcId, <tileIds>>

	@Override
	public void process(Request req, List<Response> responses) {
		setAction("load_instances");
		
		final int centreTileId = BuildManager.get().getTileId();
		final int floor = BuildManager.get().getFloor();
		
		groundTextures = new HashMap<>();
		scenery = new HashMap<>();
		npcs = new HashMap<>();
		Utils.getLocalTiles(centreTileId, 12)
			.forEach(tile -> {
				final int groundTextureId = GroundTextureDao.getGroundTextureIdByTileId(floor, tile);
				groundTextures.putIfAbsent(groundTextureId, new HashSet<>());
				groundTextures.get(groundTextureId).add(tile);
				
				final int sceneryId = SceneryDao.getSceneryIdByTileId(floor, tile);
				if (sceneryId != -1) {
					scenery.putIfAbsent(sceneryId, new HashSet<>());
					scenery.get(sceneryId).add(tile);
				}
				
				final int npcId = NPCDao.getNpcIdFromInstanceId(floor, tile);
				if (npcId != -1) {
					npcs.putIfAbsent(npcId, new HashSet<>());
					npcs.get(npcId).add(tile);
				}
			});
		
		responses.add(this);
	}

}
