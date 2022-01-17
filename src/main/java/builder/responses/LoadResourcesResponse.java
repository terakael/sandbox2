package builder.responses;

import java.util.List;
import java.util.Set;

import builder.requests.Request;
import database.dao.GroundTextureDao;
import database.dao.ItemDao;
import database.dao.NPCDao;
import database.dao.SceneryDao;
import database.dao.SpriteFrameDao;
import database.dao.SpriteMapDao;
import database.dto.GroundTextureDto;
import database.dto.ItemDto;
import database.dto.NPCDto;
import database.dto.SceneryDto;
import database.dto.SpriteFrameDto;
import database.dto.SpriteMapDto;

@SuppressWarnings("unused")
public class LoadResourcesResponse extends Response {
	private List<SpriteMapDto> spriteMaps = null;
	private List<SpriteFrameDto> spriteFrames = null;
	private Set<SceneryDto> scenery = null;
	private Set<NPCDto> npcs = null;
	private List<GroundTextureDto> groundTextures = null;
	private List<ItemDto> items = null; // for respawnables
	
	private final int startingFloor = 0;
	private final int startingTileId = 929160881;
	
	@Override
	public void process(Request req, List<Response> responses) {
		setAction("load_resources");
		
		spriteMaps = SpriteMapDao.getSpriteMaps();
		spriteFrames = SpriteFrameDao.getAllSpriteFrames();
		scenery = SceneryDao.getAllScenery();
		npcs = NPCDao.getNpcList();
		groundTextures = GroundTextureDao.getGroundTextures();
		items = ItemDao.getAllItems();
		
		responses.add(this);
	}
}
