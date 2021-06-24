package main.scenery.constructable;

import java.util.List;
import java.util.stream.Collectors;

import main.GroundItemManager;
import main.database.dao.ItemDao;
import main.database.dao.SceneryDao;
import main.database.dto.ConstructableDto;
import main.processing.PathFinder;
import main.processing.WorldProcessor;
import main.responses.ResponseMaps;
import main.utils.RandomUtil;

public class NaturesShrine extends Constructable {
	private List<Integer> spawnableTileIds; // list due to random access
	private List<Integer> flowerIds; // list due to random access
	
	public NaturesShrine(int floor, int tileId, ConstructableDto dto) {
		super(floor, tileId, dto);
		
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
		if (tickId % 5 == 0) {
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
		return WorldProcessor.getLocalTiles(tileId, 2).stream()
				.filter(e -> {
					return PathFinder.tileIsValid(floor, e) && SceneryDao.getSceneryIdByTileId(floor, e) == -1 && e != tileId;
				}).collect(Collectors.toList());
	}

}
