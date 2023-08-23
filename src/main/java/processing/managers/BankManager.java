package processing.managers;

import java.util.HashMap;
import java.util.Map;

import database.DbConnection;
import database.entity.update.UpdatePlayerStorageEntity;
import types.Storage;
import types.StorageTypes;

public class BankManager {
	private static Map<Integer, Storage> banksByPlayerId = new HashMap<>();
	public static void setupCaches() {
		DbConnection.load("select player_id, slot, item_id, count, charges from player_storage where storage_id=2 order by player_id, slot", rs -> {
			banksByPlayerId.putIfAbsent(rs.getInt("player_id"), new Storage(35));
			
			final Storage storage = banksByPlayerId.get(rs.getInt("player_id"));
			storage.setAllItemsStackable(true); // all items are stackable in bank
			storage.set(rs.getInt("item_id"),
						rs.getInt("slot"),
						rs.getInt("count"),
						rs.getInt("charges"));
			addCallbacks(rs.getInt("player_id"), storage);
		});
	}
	
	private static void addCallbacks(int playerId, Storage storage) {
//		storage.setAddStackableCallback(item -> {
//			DatabaseUpdater.enqueue(UpdatePlayerStorageEntity.builder()
//				.playerId(playerId)
//				.storageId(StorageTypes.BANK.getValue())
//				.slot(item.getSlot())
//				.itemId(item.getItemId())
//				.count(item.getCount())
//				.charges(item.getCharges())
//				.build());
//		});
//		
		storage.setAddCallback(item -> {
			DatabaseUpdater.enqueue(UpdatePlayerStorageEntity.builder()
				.playerId(playerId)
				.storageId(StorageTypes.BANK.getValue())
				.slot(item.getSlot())
				.itemId(item.getItemId())
				.count(item.getCount())
				.charges(item.getCharges())
				.build());
		});

//		storage.setSwapSlotCallback((newSrcItem, newDestItem) -> {
//			DatabaseUpdater.enqueue(UpdatePlayerStorageEntity.builder()
//				.playerId(playerId)
//				.storageId(StorageTypes.BANK.getValue())
//				.slot(newSrcItem.getSlot())
//				.itemId(newSrcItem.getItemId())
//				.count(newSrcItem.getCount())
//				.charges(newSrcItem.getCharges())
//				.build());
//			
//			DatabaseUpdater.enqueue(UpdatePlayerStorageEntity.builder()
//				.playerId(playerId)
//				.storageId(StorageTypes.BANK.getValue())
//				.slot(newDestItem.getSlot())
//				.itemId(newDestItem.getItemId())
//				.count(newDestItem.getCount())
//				.charges(newDestItem.getCharges())
//				.build());
//		});
	}

	public static Storage getStorage(int playerId) {
		return banksByPlayerId.get(playerId);
	}
}
