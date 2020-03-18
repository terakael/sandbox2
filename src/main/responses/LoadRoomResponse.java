package main.responses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import main.database.GroundTextureDao;
import main.database.GroundTextureDto;
import main.database.SceneryDao;
import main.processing.MinimapGenerator;
import main.processing.Player;
import main.requests.Request;

public class LoadRoomResponse extends Response {
	private List<GroundTextureDto> groundTextures;
	private HashMap<Integer, HashSet<Integer>> sceneryInstances;
	private String minimap;
	
	public LoadRoomResponse() {
		setAction("load_room");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		sceneryInstances = SceneryDao.getAllSceneryInstancesByRoom(player.getRoomId());
		groundTextures = GroundTextureDao.getAllGroundTexturesByRoom(player.getRoomId());
		minimap = MinimapGenerator.getImage(player.getRoomId());
		responseMaps.addClientOnlyResponse(player, this);
	}

}
