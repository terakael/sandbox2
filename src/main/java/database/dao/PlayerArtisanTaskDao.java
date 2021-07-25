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
		final String query = "select player_id, assigned_master_id, item_id, assigned_amount, handed_in_amount, total_tasks, total_points from player_artisan_task";
		DbConnection.load(query, rs -> {
			playerTaskItem.put(rs.getInt("player_id"), 
					new PlayerArtisanTaskDto(rs.getInt("player_id"), 
							rs.getInt("assigned_master_id"),
							rs.getInt("item_id"),
							rs.getInt("assigned_amount"), 
							rs.getInt("handed_in_amount"),
							rs.getInt("total_tasks"),
							rs.getInt("total_points")));
		});
	}
	
	public static void newTask(int playerId, int assignedMasterId, int itemId, int assignedAmount) {
		if (playerTaskItem.containsKey(playerId)) {
			playerTaskItem.get(playerId).reset(assignedMasterId, itemId, assignedAmount);
			DatabaseUpdater.enqueue(UpdateArtisanTaskEntity.builder().playerId(playerId).assignedMasterId(assignedMasterId).itemId(itemId).assignedAmount(assignedAmount).handedInAmount(0).build());
		} else {
			playerTaskItem.put(playerId, new PlayerArtisanTaskDto(playerId, assignedMasterId, itemId, assignedAmount, 0, 0, 0));
			DatabaseUpdater.enqueue(InsertArtisanTaskEntity.builder()
					.playerId(playerId)
					.assignedMasterId(assignedMasterId)
					.itemId(itemId)
					.assignedAmount(assignedAmount)
					.handedInAmount(0)
					.totalTasks(0)
					.totalPoints(0)
					.build());
		}
	}
	
	public static int handInItems(int playerId, int amountToHandIn) {
		if (!playerTaskItem.containsKey(playerId))
			return 0;
		
		if (amountToHandIn == 0)
			return 0;
		
		final int amountHandedIn = playerTaskItem.get(playerId).updateAmountToHandIn(amountToHandIn);
		DatabaseUpdater.enqueue(UpdateArtisanTaskEntity.builder()
				.playerId(playerId)
				.handedInAmount(playerTaskItem.get(playerId).getHandedInAmount())
				.totalPoints(playerTaskItem.get(playerId).getTotalPoints())
				.build());
		
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
