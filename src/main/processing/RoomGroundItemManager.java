package main.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import main.database.dao.ItemDao;
import main.database.dao.RespawnableDao;
import main.database.dto.RespawnableDto;
import main.types.ItemAttributes;

public class RoomGroundItemManager {
	private static final int LIFETIME = 100;
	private int floor = 0;
	
	@Setter @Getter @AllArgsConstructor
	public static class GroundItem {
		private int id;
		private int lifetime;
		private int count;
		private int charges;
	}
	
	@Setter @Getter @AllArgsConstructor
	public static class RespawnableGroundItem {
		private int id;
		private int lifetime;
		private int count;
		private int tileId;
	}
	
	public RoomGroundItemManager(int floor) {
		this.floor = floor;
	}
	
	// a map containing a list of ground items per tileId: Map<TileId, ItemList>
	@Getter private HashMap<Integer, List<GroundItem>> globalGroundItems = new HashMap<>();
	
	// similar to the map above, but grouped by player: Map<PlayerId, AboveMap>
	private HashMap<Integer, HashMap<Integer, List<GroundItem>>> playerGroundItems = new HashMap<>();
	
	private HashSet<RespawnableGroundItem> respawnableGroundItems = new HashSet<>();
	
	public void setupRespawnables() {
		if (!RespawnableDao.getCachedRespawnables().containsKey(floor))
			return;
		
		for (RespawnableDto dto : RespawnableDao.getCachedRespawnables().get(floor)) {
			respawnableGroundItems.add(new RespawnableGroundItem(dto.getItemId(), 0, dto.getCount(), dto.getTileId()));
			
			if (!globalGroundItems.containsKey(dto.getTileId()))
				globalGroundItems.put(dto.getTileId(), new ArrayList<>());
			globalGroundItems.get(dto.getTileId()).add(new GroundItem(dto.getItemId(), 0, dto.getCount(), ItemDao.getMaxCharges(dto.getItemId())));
		}
	}
	
	public boolean itemIsRespawnable(int tileId, int itemId) {
		for (RespawnableGroundItem item : respawnableGroundItems) {
			if (item.getTileId() == tileId && item.getId() == itemId && item.lifetime == 0)
				return true;
		}
		return false;
	}
	
	public void process() {
		List<Integer> emptyTiles = new ArrayList<>();
		for (HashMap.Entry<Integer, List<GroundItem>> entry : globalGroundItems.entrySet()) {
			List<GroundItem> updated = entry.getValue()
					.stream()
					.filter(groundItem -> itemIsRespawnable(entry.getKey(), groundItem.id) || --groundItem.lifetime > 0)
					.collect(Collectors.toList());
			
			entry.getValue().clear();
			entry.getValue().addAll(updated);
			
			if (entry.getValue().isEmpty())
				emptyTiles.add(entry.getKey());
		}
		
		for (Integer emptyTile : emptyTiles)
			globalGroundItems.remove(emptyTile);
		emptyTiles.clear();
		
		List<RespawnableGroundItem> itemsToRespawn = respawnableGroundItems
			.stream()
			.filter(groundItem -> groundItem.lifetime > 0)
			.filter(groundItem -> --groundItem.lifetime == 0)
			.collect(Collectors.toList());
		
		for (RespawnableGroundItem toRespawn : itemsToRespawn) {
			if (!globalGroundItems.containsKey(toRespawn.getTileId()))
				globalGroundItems.put(toRespawn.getTileId(), new ArrayList<>());
			globalGroundItems.get(toRespawn.getTileId()).add(new GroundItem(toRespawn.getId(), toRespawn.getLifetime(), toRespawn.getCount(), ItemDao.getMaxCharges(toRespawn.getId())));
		}
		
		// loop through all the players
		for (HashMap.Entry<Integer, HashMap<Integer, List<GroundItem>>> playerEntry : playerGroundItems.entrySet()) {
			// for each player, loop through all the tiles with private ground items
			for (HashMap.Entry<Integer, List<GroundItem>> tileEntry : playerEntry.getValue().entrySet()) {
				List<GroundItem> entriesToMove = tileEntry.getValue()
						.stream()
						.filter(groundItem -> {
							return --groundItem.lifetime <= 0 &&
									ItemDao.itemHasAttribute(groundItem.getId(), ItemAttributes.TRADEABLE);
						})
						.collect(Collectors.toList());
				
				// reset the lifetime now that the're on the global list
				entriesToMove.forEach(item -> item.lifetime = LIFETIME);
				
				// if there's no global ground items at this current tile, then add an entry for the tile
				if (!globalGroundItems.containsKey(tileEntry.getKey()))
					globalGroundItems.put(tileEntry.getKey(), new ArrayList<>());
				globalGroundItems.get(tileEntry.getKey()).addAll(entriesToMove);

				// all the tradeable items are now in the globalGroundItems list so remove them from the player list.
				tileEntry.getValue().removeAll(entriesToMove);
				
				// untradeables don't get moved to the globalGroundItem list so they're still in the player list.
				tileEntry.getValue().removeAll(tileEntry.getValue()
						.stream()
						.filter(groundItem -> groundItem.lifetime == 0)
						.collect(Collectors.toList()));
				
				if (tileEntry.getValue().isEmpty())
					emptyTiles.add(tileEntry.getKey());
			}
			
			for (Integer emptyTile : emptyTiles)
				playerEntry.getValue().remove(emptyTile);
			emptyTiles.clear();
		}
	}

	public void add(int playerId, int itemId, int tileId, int count, int charges) {
		if (!playerGroundItems.containsKey(playerId))
			playerGroundItems.put(playerId, new HashMap<>());
		
		if (!playerGroundItems.get(playerId).containsKey(tileId))
			playerGroundItems.get(playerId).put(tileId, new ArrayList<>());
		
		if (ItemDao.itemHasAttribute(itemId, ItemAttributes.STACKABLE)) {
			// if there's already an item then combine it
			List<GroundItem> items = playerGroundItems.get(playerId).get(tileId);
			for (GroundItem item : items) {
				if (item.id == itemId) {
					item.count += count;
					item.lifetime = LIFETIME;
					return;
				}
			}
			
			// otherwise add it as usual
			playerGroundItems.get(playerId).get(tileId).add(new GroundItem(itemId, LIFETIME, count, charges));
		} else {
			for (int i = 0; i < count; ++i)
				playerGroundItems.get(playerId).get(tileId).add(new GroundItem(itemId, LIFETIME, 1, charges));
		}
	}

	public void remove(int playerId, int tileId, int itemId, int count, int charges) {
		if (removeFromPlayerGroundItems(playerId, tileId, itemId, count, charges))
			return;
		
		if (removeFromGlobalGroundItems(playerId, tileId, itemId, count, charges))
			return;		
	}
	
	private boolean removeFromGlobalGroundItems(int playerId, int tileId, int itemId, int count, int charges) {
		if (!globalGroundItems.containsKey(tileId))
			return false;
		
		GroundItem toRemove = null;
		for (GroundItem item : globalGroundItems.get(tileId)) {
			if (item.id == itemId && item.count == count && item.charges == charges) {
				toRemove = item;
				break;
			}
		}
		
		if (toRemove != null) {
			globalGroundItems.get(tileId).remove(toRemove);
			
			for (RespawnableGroundItem item : respawnableGroundItems) {
				if (item.tileId == tileId && item.id == toRemove.id && item.lifetime == 0) {
					item.lifetime = RespawnableDao.getCachedRespawnableByFloorAndTileId(floor, tileId).getRespawnTicks();
					break;
				}
			}
			return true;
		}
		
		return false;
	}
	
	private boolean removeFromPlayerGroundItems(int playerId, int tileId, int itemId, int count, int charges) {
		if (!playerGroundItems.containsKey(playerId))
			return false;
		
		if (!playerGroundItems.get(playerId).containsKey(tileId))
			return false;
		
		GroundItem toRemove = null;
		for (GroundItem item : playerGroundItems.get(playerId).get(tileId)) {
			if (item.id == itemId && item.count == count && item.charges == charges) {
				toRemove = item;
				break;
			}
		}
		
		if (toRemove != null) {
			playerGroundItems.get(playerId).get(tileId).remove(toRemove);
			return true;
		}
		
		return false;
	}
	
	public boolean itemIsOnGround(int playerId, int itemId) {		
		// check player store
		if (playerGroundItems.containsKey(playerId)) {
			for (HashMap.Entry<Integer, List<GroundItem>> tileEntry : playerGroundItems.get(playerId).entrySet()) {
				for (GroundItem item : tileEntry.getValue()) {
					if (item.id == itemId)
						return true;
				}
			}
		}
		
		// check global store
		for (HashMap.Entry<Integer, List<GroundItem>> tileEntry : globalGroundItems.entrySet()) {
			for (GroundItem item : tileEntry.getValue()) {
				if (item.id == itemId)
					return true;
			}
		}

		return false;
	}
	
	public Map<Integer, List<Integer>> getItemIdsNearTile(int playerId, int tileId, int proximity) {
		Map<Integer, List<Integer>> localTiles = new HashMap<>();
		
		final int tileX = tileId % PathFinder.LENGTH;
		final int tileY = tileId / PathFinder.LENGTH;
		
		final int minX = tileX - proximity;
		final int maxX = tileX + proximity;
		final int minY = tileY - proximity;
		final int maxY = tileY + proximity;
		
		// check private player store first
		if (playerGroundItems.containsKey(playerId)) {
			for (HashMap.Entry<Integer, List<GroundItem>> tileEntry : playerGroundItems.get(playerId).entrySet()) {
				if (tileEntry.getValue().isEmpty())
					continue;// don't add any empty lists (these get cleaned up in process())
				
				final int itemTileX = tileEntry.getKey() % PathFinder.LENGTH;
				final int itemTileY = tileEntry.getKey() / PathFinder.LENGTH;
				
				if (itemTileX >= minX && itemTileX <= maxX && itemTileY >= minY && itemTileY <= maxY) {
					if (!localTiles.containsKey(tileEntry.getKey()))
						localTiles.put(tileEntry.getKey(), new ArrayList<>());

					for (GroundItem item : tileEntry.getValue())
						localTiles.get(tileEntry.getKey()).add(item.getId());
				}
					
			}
		}
		
		// now global store
		for (HashMap.Entry<Integer, List<GroundItem>> tileEntry : globalGroundItems.entrySet()) {
			if (tileEntry.getValue().isEmpty())
				continue;// don't add any empty lists (these get cleaned up in process())
			final int itemTileX = tileEntry.getKey() % PathFinder.LENGTH;
			final int itemTileY = tileEntry.getKey() / PathFinder.LENGTH;
			
			if (itemTileX >= minX && itemTileX <= maxX && itemTileY >= minY && itemTileY <= maxY) {
				if (!localTiles.containsKey(tileEntry.getKey()))
					localTiles.put(tileEntry.getKey(), new ArrayList<>());

				for (GroundItem item : tileEntry.getValue())
					localTiles.get(tileEntry.getKey()).add(item.getId());
			}
				
		}
		
		return localTiles;
	}
	
	public GroundItem getItemAtTileId(int playerId, int itemId, int tileId) {
		if (playerGroundItems.containsKey(playerId)) {
			if (playerGroundItems.get(playerId).containsKey(tileId)) {
				for (GroundItem item : playerGroundItems.get(playerId).get(tileId)) {
					if (item.id == itemId)
						return item;
				}
			}
		}
		
		if (globalGroundItems.containsKey(tileId)) {
			for (GroundItem item : globalGroundItems.get(tileId)) {
				if (item.id == itemId)
					return item;
			}
		}
				
		return null;
	}
}
