package main.responses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import main.database.GroundTextureDao;
import main.database.GroundTextureDto;
import main.database.SceneryDao;
import main.processing.FightManager;
import main.processing.FightManager.Fight;
import main.processing.FlowerManager;
import main.processing.MinimapGenerator;
import main.processing.NPC;
import main.processing.Player;
import main.processing.RockManager;
import main.processing.WorldProcessor;
import main.requests.Request;

public class LoadRoomResponse extends Response {
	private HashMap<Integer, HashSet<Integer>> sceneryInstances;
	private HashSet<Integer> depletedScenery; // depleted rocks, flowers etc need to be flicked to their next frame (frame[0] is non-depleted, frame[1] depleted)
	private String minimap;
	
	public LoadRoomResponse() {
		setAction("load_room");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		sceneryInstances = SceneryDao.getAllSceneryInstancesByRoom(player.getRoomId());
		minimap = MinimapGenerator.getImage(player.getRoomId());
		
		depletedScenery = new HashSet<>();
		depletedScenery.addAll(FlowerManager.getDepletedFlowerTileIds());
		depletedScenery.addAll(RockManager.getDepletedRockTileIds());
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
