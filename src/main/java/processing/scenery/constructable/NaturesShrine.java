package processing.scenery.constructable;

import java.util.List;
import java.util.stream.Collectors;

import database.dao.ItemDao;
import database.dao.SceneryDao;
import database.dto.ConstructableDto;
import processing.PathFinder;
import responses.ResponseMaps;
import system.GroundItemManager;
import utils.RandomUtil;
import utils.Utils;

public class NaturesShrine extends Constructable {
	private List<Integer> spawnableTileIds; // list due to random access
	private List<Integer> flowerIds; // list due to random access
	
	private final static int GROW_TIMER = 5;
	private int growOffset;
	
	public NaturesShrine(int floor, int tileId, int lifetimeTicks, ConstructableDto dto) {
		super(floor, tileId, lifetimeTicks, dto);
		
		growOffset = RandomUtil.getRandom(0, GROW_TIMER); // used so flowers grow on different ticks when there's multiple Natures Shrines around.  Looks weird otherwise.
		
		spawnableTileIds = findSpawnableTileIds();
		
		flowerIds = List.<Integer>of(
					ItemDao.getIdFromName("orange harnia"),
					ItemDao.getIdFromName("red russine"),
					ItemDao.getIdFromName("sky flower"),
					ItemDao.getIdFromName("dark bluebell"),
					ItemDao.getIdFromName("starflower")
				);
	}

	@Override
	public void processConstructable(int tickId, ResponseMaps responseMaps) {		
		if (tickId % GROW_TIMER == growOffset) {
			if (spawnableTileIds.isEmpty()) {
				spawnableTileIds = findSpawnableTileIds(); 
			}
			
			final int itemId = flowerIds.get(RandomUtil.getRandom(0, flowerIds.size()));
			
			final int tileIndex = RandomUtil.getRandom(0, spawnableTileIds.size());
			final int tileIdToSpawnOn = spawnableTileIds.get(tileIndex);
			GroundItemManager.addGlobally(floor, tileIdToSpawnOn, itemId, 1, ItemDao.getMaxCharges(itemId));
			spawnableTileIds.remove(tileIndex);
		}
	}
	
	private List<Integer> findSpawnableTileIds() {
		return Utils.getLocalTiles(tileId, 2).stream()
				.filter(e -> {
					return PathFinder.tileIsValid(floor, e) && SceneryDao.getSceneryIdByTileId(floor, e) == -1 && e != tileId;
				}).collect(Collectors.toList());
	}

}
