package processing.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import database.DbConnection;
import database.dao.MinimapSegmentDao;
import database.dto.HouseInfoDto;
import database.entity.update.UpdatePlayerEntity;
import processing.PathFinder;

public class HousingManager {
	private static Map<Integer, Integer> houseOwnership; // houseId, playerId
	private static Map<Integer, Map<Integer, Integer>> housingTileInstances; // floor, <tileId, houseId>
	private static Map<Integer, Map<Integer, Set<Integer>>> walkableTilesByHouseId; // houseId, <floor, <tileIds>>
	private static Map<Integer, String> houseNamesById;
	
	private static Map<Integer, List<HouseInfoDto>> housesByRealtorInstanceId; // realtor tileId, <houses (list for random access)>
	
	public static void setupCaches() {
		Map<Integer, Integer> minTileByHouseId = new HashMap<>(); // we don't care about floor, just the min tile
		housingTileInstances = new HashMap<>();
		walkableTilesByHouseId = new HashMap<>();
		DbConnection.load("select floor, tile_id, house_id from housing_tiles", rs -> {
			// quick lookup for houseIds based on floor/tileId
			housingTileInstances.putIfAbsent(rs.getInt("floor"), new HashMap<>());
			housingTileInstances.get(rs.getInt("floor")).put(rs.getInt("tile_id"), rs.getInt("house_id"));
			
			// quick lookup for tileIds based on floor/houseId.  Note we only want walkable tiles.
			if ((PathFinder.getImpassableByTileId(rs.getInt("floor"), rs.getInt("tile_id")) & 15) != 15) {
				walkableTilesByHouseId.putIfAbsent(rs.getInt("house_id"), new HashMap<>());
				walkableTilesByHouseId.get(rs.getInt("house_id")).putIfAbsent(rs.getInt("floor"), new HashSet<>());
				walkableTilesByHouseId.get(rs.getInt("house_id")).get(rs.getInt("floor")).add(rs.getInt("tile_id"));
			}
			minTileByHouseId.put(rs.getInt("house_id"), 
					minTileByHouseId.containsKey(rs.getInt("house_id")) 
						? Math.min(rs.getInt("tile_id"), minTileByHouseId.get(rs.getInt("house_id")))
						: rs.getInt("tile_id"));
		});
		
		houseOwnership = new HashMap<>();
		DbConnection.load("select distinct housing_tiles.house_id, player.id as player_id from housing_tiles "
						+ "left outer join player on player.house_id = housing_tiles.house_id", rs -> {
			// rs doesn't allow for nullables natively, and returns an "int" type.
			// we need to check wasNull() to figure out if it was actually null...
			Integer playerId = rs.getInt("player_id");
			playerId = rs.wasNull() ? null : playerId;
			houseOwnership.put(rs.getInt("house_id"), playerId);
		});
		
		houseNamesById = new HashMap<>();
		DbConnection.load("select id, name from house_names", rs -> {
			houseNamesById.put(rs.getInt("id"), rs.getString("name"));
		});
		
		housesByRealtorInstanceId = new HashMap<>();
		DbConnection.load("select tile_id from room_npcs where npc_id=62", rs -> {
			housesByRealtorInstanceId.put(rs.getInt("tile_id"), new LinkedList<>());
		});
		
		final Set<Integer> realtorTileIds = housesByRealtorInstanceId.keySet();
		minTileByHouseId.forEach((houseId, tileId) -> {
			final int closestRealtor = realtorTileIds.stream()
					.min((i, j) -> PathFinder.getCloserTile(tileId, i, j)).get(); 
			
			housesByRealtorInstanceId.get(closestRealtor)
				.add(new HouseInfoDto(houseId, getHouseNameById(houseId), houseOwnership.get(houseId) == null, getHouseTotalSizeById(houseId), 0, getHouseImages(houseId)));
		});
	}
	
	public static List<HouseInfoDto> getPortfolioByHouseSellerInstanceId(int instanceId) {
		if (!housesByRealtorInstanceId.containsKey(instanceId))
			return new LinkedList<>();
		return housesByRealtorInstanceId.get(instanceId);
	}
	
	private static List<String> getHouseImages(int houseId) {
		List<String> base64 = new ArrayList<>();
		
		if (!walkableTilesByHouseId.containsKey(houseId))
			return base64;
		
		for (var entry : walkableTilesByHouseId.get(houseId).entrySet()) {
			int anyTileId = entry.getValue().iterator().next(); // just get the first tileId in the set
			base64.add(MinimapSegmentDao.getMinimapSegmentDataByTileId(entry.getKey(), anyTileId));
		}
		
		return base64;
	}
	
	private static int getHouseTotalSizeById(int houseId) {
		// just using walkableTiles (i.e. ignoring immovable furniture like tables) because its easier go fuck yourself
		return (int)walkableTilesByHouseId.get(houseId).values().stream()
				.flatMap(Set::stream)
				.count();
	}
	
	public static int getHouseIdFromFloorAndTileId(int floor, int tileId) {
		if (!housingTileInstances.containsKey(floor) || !housingTileInstances.get(floor).containsKey(tileId))
			return -1;
		return housingTileInstances.get(floor).get(tileId);
	}
	
	public static Set<Integer> getWalkableTilesByHouseId(int houseId, int floor) {
		if (walkableTilesByHouseId.containsKey(houseId) && walkableTilesByHouseId.get(houseId).containsKey(floor))
			return walkableTilesByHouseId.get(houseId).get(floor);
		return null;
	}
	
	public static int[] getRandomWalkableTileByPlayerId(int playerId) {
		final int houseId = HousingManager.getHouseIdByPlayerId(playerId);
		if (houseId == -1)
			return null;
		
		// this is a mess, there must be a better way
		Map<Integer, Set<Integer>> allWalkableTiles = getAllWalkableTilesByHouseId(houseId);
		List<Integer> floorList = new ArrayList<>(allWalkableTiles.keySet());
		int randomFloor = floorList.get(new Random().nextInt(floorList.size()));
		List<Integer> tileList = new ArrayList<>(allWalkableTiles.get(randomFloor));
		int randomTile = tileList.get(new Random().nextInt(tileList.size()));
		
		return new int[] {randomFloor, randomTile};
	}
	
	public static Map<Integer, Set<Integer>> getAllWalkableTilesByHouseId(int houseId) {
		return walkableTilesByHouseId.get(houseId);
	}
	
	public static boolean assignHouseToPlayer(int houseId, int playerId) {
		if (!houseOwnership.containsKey(houseId))
			return false; // house doesn't exist
		
		if (houseOwnership.get(houseId) != null)
			return false; // house is already assigned
		
		houseOwnership.put(houseId, playerId);
		setHouseForSaleAtRealtor(houseId, false);
		
		DatabaseUpdater.enqueue(UpdatePlayerEntity.builder().id(playerId).houseId(houseId).build());
		return true;
	}
	
	public static boolean unassignHouseFromPlayer(int playerId) {
		int houseId = -1;
		for (var entry : houseOwnership.entrySet()) {
			if (entry.getValue() != null && entry.getValue() == playerId) {
				houseId = entry.getKey();
				break;
			}
		}
		
		if (houseId == -1) {
			// couldn't find player in list?
			return false;
		}
		
		if (!houseOwnership.containsKey(houseId))
			return false; // house doesn't exist
		
		if (houseOwnership.get(houseId) != playerId)
			return false; // house isn't assigned to player
		
		houseOwnership.put(houseId, null);
		
		setHouseForSaleAtRealtor(houseId, true);
		
		DatabaseUpdater.enqueue(UpdatePlayerEntity.builder().id(playerId).houseId(0).build());
		return true;
	}
	
	private static void setHouseForSaleAtRealtor(int houseId, boolean forSale) {
		for (var entry : housesByRealtorInstanceId.entrySet()) {
			final HouseInfoDto house = entry.getValue().stream()
					.filter(e -> e.getId() == houseId)
					.findFirst()
					.orElse(null);
			
			if (house != null) {
				house.setForSale(forSale);
				break;
			}
		}
	}
	
	public static int getOwningPlayerId(int floor, int tileId) {
		int houseId = getHouseIdFromFloorAndTileId(floor, tileId);
		if (!houseOwnership.containsKey(houseId) || houseOwnership.get(houseId) == null)
			return -1;
		return houseOwnership.get(houseId);
	}
	
	public static int getHouseIdByPlayerId(int playerId) {
		return houseOwnership.entrySet().stream()
				.filter(entry -> entry.getValue() == playerId)
				.map(Map.Entry::getKey)
				.findFirst()
				.orElse(-1);
	}
	
	public static String getHouseNameById(int id) {
		return houseNamesById.get(id);
	}
}
