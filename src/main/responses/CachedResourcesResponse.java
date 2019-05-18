package main.responses;

import java.util.List;
import java.util.Map;

import javax.websocket.Session;

import main.database.ContextOptionsDao;
import main.database.ItemDao;
import main.database.ItemDto;
import main.database.SceneryDao;
import main.database.SceneryDto;
import main.database.SpriteFrameDao;
import main.database.SpriteFrameDto;
import main.database.SpriteMapDao;
import main.database.SpriteMapDto;
import main.requests.Request;

public class CachedResourcesResponse extends Response {
	private List<SpriteMapDto> spriteMaps = null;
	private List<ItemDto> items = null;
	private List<SpriteFrameDto> spriteFrames = null;
	private List<SceneryDto> scenery = null;
	private Map<Integer, String> contextOptions = null;
	private static CachedResourcesResponse instance = null;

	private CachedResourcesResponse(String action) {
		super(action);
	}
	
	public static CachedResourcesResponse get() {
		if (instance == null) {
			instance = new CachedResourcesResponse("cached_resources");
			instance.loadCachedResources();
		}
		return instance;
	}

	@Override
	public void process(Request req, Session client, ResponseMaps responseMaps) {
		
	}
	
	private void loadCachedResources() {
		spriteMaps = SpriteMapDao.getAllSpriteMaps();
		spriteFrames = SpriteFrameDao.getAllSpriteFrames();
		items = ItemDao.getAllItems();
		scenery = SceneryDao.getAllSceneryByRoom(1);// TODO dynamic based on player room when there's more rooms
		contextOptions = ContextOptionsDao.getAllContextOptions();
	}

}
