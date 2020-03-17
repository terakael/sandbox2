package main.responses;

import java.util.List;

import main.database.GroundTextureDao;
import main.database.GroundTextureDto;
import main.database.SceneryDao;
import main.database.SceneryDto;
import main.processing.MinimapGenerator;
import main.processing.Player;
import main.requests.Request;

public class LoadRoomResponse extends Response {
	private List<GroundTextureDto> groundTextures = null;
	private List<SceneryDto> scenery = null;
	private String minimap;
	
	public LoadRoomResponse() {
		setAction("load_room");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		scenery = SceneryDao.getAllSceneryByRoom(player.getRoomId());
		groundTextures = GroundTextureDao.getAllGroundTexturesByRoom(player.getRoomId());
		minimap = MinimapGenerator.getImage(player.getRoomId());
		responseMaps.addClientOnlyResponse(player, this);
	}

}
