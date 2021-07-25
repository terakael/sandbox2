package database.dao;

import java.util.HashMap;
import java.util.Map;

import database.DbConnection;
import database.dto.PlayerArtisanTaskDto;
import database.entity.insert.InsertArtisanTaskEntity;
import database.entity.update.UpdateArtisanTaskEntity;
import processing.managers.DatabaseUpdater;

public class PlayerArtisanTaskDao {
	private static final Map<Integer, PlayerArtisanTaskDto> playerTaskItem = new HashMap<>();
	
	public static void setupCaches() {
		final String query = "select player_id, item_id, assigned_amount, handed_in_amount from player_artisan_task";
		DbConnection.load(query, rs -> {
			playerTaskItem.put(rs.getInt("player_id"), new PlayerArtisanTaskDto(rs.getInt("player_id"), rs.getInt("item_id"), rs.getInt("assigned_amount"), rs.getInt("handed_in_amount")));
		});
	}
	
	public static void newTask(int playerId, int itemId, int assignedAmount) {
		if (playerTaskItem.containsKey(playerId)) {
			playerTaskItem.get(playerId).reset(itemId, assignedAmount);
			DatabaseUpdater.enqueue(UpdateArtisanTaskEntity.builder().playerId(playerId).itemId(itemId).assignedAmount(assignedAmount).handedInAmount(0).build());
		} else {
			playerTaskItem.put(playerId, new PlayerArtisanTaskDto(playerId, itemId, assignedAmount, 0));
			DatabaseUpdater.enqueue(InsertArtisanTaskEntity.builder().playerId(playerId).itemId(itemId).assignedAmount(assignedAmount).handedInAmount(0).build());
		}
	}
	
	public static int handInItems(int playerId, int amountToHandIn) {
		if (!playerTaskItem.containsKey(playerId))
			return 0;
		
		if (amountToHandIn == 0)
			return 0;
		
		final int amountHandedIn = playerTaskItem.get(playerId).updateAmountToHandIn(amountToHandIn);
		DatabaseUpdater.enqueue(UpdateArtisanTaskEntity.builder().playerId(playerId).handedInAmount(playerTaskItem.get(playerId).getHandedInAmount()).build());
		
		return amountHandedIn;
	}
	
	public static int getTaskItemId(int playerId) {
		if (!playerTaskItem.containsKey(playerId))
			return -1;
		return playerTaskItem.get(playerId).getItemId();
	}
	
	public static PlayerArtisanTaskDto getTask(int playerId) {
		return playerTaskItem.get(playerId);
	}
}
