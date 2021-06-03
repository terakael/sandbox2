package main.responses;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;
import main.database.dao.ContextOptionsDao;
import main.database.dao.SpriteFrameDao;
import main.database.dao.SpriteMapDao;
import main.database.dao.StatsDao;
import main.database.dto.ContextOptionsDto;
import main.database.dto.SpriteFrameDto;
import main.database.dto.SpriteMapDto;
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
//	private List<ItemDto> items = null;

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
//		items = Collections.singletonList(ItemDao.getItem(0));
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
