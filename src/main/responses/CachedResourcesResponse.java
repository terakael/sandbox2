package main.responses;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import main.database.ContextOptionsDao;
import main.database.ContextOptionsDto;
import main.database.GroundTextureDao;
import main.database.GroundTextureDto;
import main.database.ItemDao;
import main.database.ItemDto;
import main.database.NPCDao;
import main.database.NPCDto;
import main.database.PrayerDao;
import main.database.PrayerDto;
import main.database.SceneryDao;
import main.database.SceneryDto;
import main.database.SpriteFrameDao;
import main.database.SpriteFrameDto;
import main.database.SpriteMapDao;
import main.database.SpriteMapDto;
import main.database.StatsDao;
import main.processing.Player;
import main.requests.Request;

@Getter
public class CachedResourcesResponse extends Response {
	private static CachedResourcesResponse instance = null;
	
	private List<SpriteMapDto> spriteMaps = null;
	private List<ItemDto> items = null;
	private List<SpriteFrameDto> spriteFrames = null;
	private List<ContextOptionsDto> contextOptions = null;
	private Map<Integer, String> statMap = null;
	private Map<Integer, Integer> expMap = null;
	private List<NPCDto> npcs = null;
	private List<SceneryDto> scenery = null;
	private List<GroundTextureDto> groundTextures = null;

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
		spriteMaps = SpriteMapDao.getSpriteMaps(); // TODO pull sprite maps local to the player and request new ones as needed
		spriteFrames = SpriteFrameDao.getAllSpriteFrames();
		items = ItemDao.getAllItems();
		contextOptions = ContextOptionsDao.getAllContextOptions();
		statMap = StatsDao.getCachedStats();
		npcs = NPCDao.getNpcList();
		expMap = StatsDao.getExpMap();
		scenery = SceneryDao.getAllScenery();
		groundTextures = GroundTextureDao.getGroundTextures();
	}

}
