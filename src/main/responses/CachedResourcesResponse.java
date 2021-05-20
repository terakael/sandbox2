package main.responses;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import main.database.ContextOptionsDao;
import main.database.ContextOptionsDto;
import main.database.GroundTextureDao;
import main.database.GroundTextureDto;
import main.database.ItemDto;
import main.database.NPCDto;
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
	private List<SpriteFrameDto> spriteFrames = null;
	private List<ContextOptionsDto> contextOptions = null;
	private Map<Integer, String> statMap = null;
	private Map<Integer, Integer> expMap = null;

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
		spriteMaps = SpriteMapDao.getAlwaysLoadedSpriteMaps();
		
		spriteFrames = SpriteFrameDao.getAllSpriteFrames().stream()
				.filter(e -> spriteMaps.stream()
						.map(SpriteMapDto::getId)
						.collect(Collectors.toSet())
				.contains(e.getSprite_map_id()))
				.collect(Collectors.toList());
		contextOptions = ContextOptionsDao.getAllContextOptions();
		statMap = StatsDao.getCachedStats();
		expMap = StatsDao.getExpMap();
	}

}
