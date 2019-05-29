package main.responses;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import main.database.ContextOptionsDao;
import main.database.ContextOptionsDto;
import main.database.ItemDao;
import main.database.ItemDto;
import main.database.SceneryDao;
import main.database.SceneryDto;
import main.database.SpriteFrameDao;
import main.database.SpriteFrameDto;
import main.database.SpriteMapDao;
import main.database.SpriteMapDto;
import main.processing.Player;
import main.requests.Request;

@Getter
public class CachedResourcesResponse extends Response {
	private List<SpriteMapDto> spriteMaps = null;
	private List<ItemDto> items = null;
	private List<SpriteFrameDto> spriteFrames = null;
	private List<SceneryDto> scenery = null;
	private List<ContextOptionsDto> contextOptions = null;
	private static CachedResourcesResponse instance = null;

	private CachedResourcesResponse() {
		setAction("cached_resources");
	}
	
	public static CachedResourcesResponse get() {
		if (instance == null) {
			instance = new CachedResourcesResponse();
			instance.loadCachedResources();
		}
		return instance;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
	
	private void loadCachedResources() {
		spriteMaps = SpriteMapDao.getAllSpriteMaps();
		spriteFrames = SpriteFrameDao.getAllSpriteFrames();
		items = ItemDao.getAllItems();
		scenery = SceneryDao.getAllSceneryByRoom(1);// TODO dynamic based on player room when there's more rooms
		contextOptions = ContextOptionsDao.getAllContextOptions();
	}

}
