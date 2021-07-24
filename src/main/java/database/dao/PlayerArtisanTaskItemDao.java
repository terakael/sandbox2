package database.dao;

import java.util.HashMap;
import java.util.Map;

import database.DbConnection;
import database.dto.PlayerArtisanTaskItemDto;
import database.entity.insert.InsertArtisanTaskItemEntity;
import database.entity.update.UpdateArtisanTaskItemEntity;
import processing.managers.DatabaseUpdater;

public class PlayerArtisanTaskItemDao {
	private static final Map<Integer, PlayerArtisanTaskItemDto> playerTaskItem = new HashMap<>();
	
	public static void setupCaches() {
		final String query = "select player_id, item_id, assigned_amount, handed_in_amount from player_artisan_task_item";
		DbConnection.load(query, rs -> {
			playerTaskItem.put(rs.getInt("player_id"), new PlayerArtisanTaskItemDto(rs.getInt("player_id"), rs.getInt("item_id"), rs.getInt("assigned_amount"), rs.getInt("handed_in_amount")));
		});
	}
	
	public static void newTask(int playerId, int itemId, int assignedAmount) {
		if (playerTaskItem.containsKey(playerId)) {
			playerTaskItem.get(playerId).reset(itemId, assignedAmount);
			DatabaseUpdater.enqueue(UpdateArtisanTaskItemEntity.builder().playerId(playerId).itemId(itemId).assignedAmount(assignedAmount).handedInAmount(0).build());
		} else {
			playerTaskItem.put(playerId, new PlayerArtisanTaskItemDto(playerId, itemId, assignedAmount, 0));
			DatabaseUpdater.enqueue(InsertArtisanTaskItemEntity.builder().playerId(playerId).itemId(itemId).assignedAmount(assignedAmount).handedInAmount(0).build());
		}
	}
	
	public static int handInItems(int playerId, int amountToHandIn) {
		if (!playerTaskItem.containsKey(playerId))
			return 0;
		
		final int amountHandedIn = playerTaskItem.get(playerId).updateAmountToHandIn(amountToHandIn);
		DatabaseUpdater.enqueue(UpdateArtisanTaskItemEntity.builder().playerId(playerId).handedInAmount(playerTaskItem.get(playerId).getHandedInAmount()).build());
		
		return amountHandedIn;
	}
	
	public static int getTaskItemId(int playerId) {
		if (!playerTaskItem.containsKey(playerId))
			return -1;
		return playerTaskItem.get(playerId).getItemId();
	}
	
	public static PlayerArtisanTaskItemDto getTask(int playerId) {
		return playerTaskItem.get(playerId);
	}
}
