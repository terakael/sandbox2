package responses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;
import database.DbConnection;
import database.dao.ContextOptionsDao;
import database.dao.SpriteFrameDao;
import database.dao.SpriteMapDao;
import database.dao.StatsDao;
import database.dto.ContextOptionsDto;
import database.dto.SpriteFrameDto;
import database.dto.SpriteMapDto;
import processing.attackable.Player;
import requests.Request;

@Getter
public class CachedResourcesResponse extends Response {
	private static CachedResourcesResponse instance = null;
	
	private List<SpriteMapDto> spriteMaps = null;
	private List<SpriteFrameDto> spriteFrames = null;
	private Map<String, List<ContextOptionsDto>> contextOptions = new HashMap<>();
	private Map<Integer, String> statMap = null;
	private Map<Integer, Integer> expMap = null;
	private Map<Integer, String> attackStyles = new HashMap<>();

	private CachedResourcesResponse() {
		setAction("cached_resources");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
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
				
		contextOptions.put("item", ContextOptionsDao.getItemContextOptions());
		contextOptions.put("npc", ContextOptionsDao.getNpcContextOptions());
		contextOptions.put("scenery", ContextOptionsDao.getSceneryContextOptions());
		statMap = StatsDao.getCachedStats();
		expMap = StatsDao.getExpMap();
		DbConnection.load("select id, name from attack_styles", 
				rs -> attackStyles.put(rs.getInt("id"), rs.getString("name")));
	}

}
