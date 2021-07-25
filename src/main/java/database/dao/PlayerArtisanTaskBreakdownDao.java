package database.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import database.DbConnection;
import database.dto.PlayerArtisanTaskBreakdownDto;
import database.dto.PlayerArtisanTaskDto;
import database.entity.delete.DeleteArtisanTaskBreakdownEntity;
import database.entity.insert.InsertArtisanTaskBreakdownEntity;
import database.entity.update.UpdateArtisanTaskBreakdownEntity;
import processing.managers.DatabaseUpdater;

public class PlayerArtisanTaskBreakdownDao {
	private static final Map<Integer, List<PlayerArtisanTaskBreakdownDto>> playerArtisanTasks = new HashMap<>();
	
	public static void setupCaches() {
		final String query = "select player_id, item_id, amount from player_artisan_task_breakdown";
		DbConnection.load(query, rs -> {
			playerArtisanTasks.putIfAbsent(rs.getInt("player_id"), new ArrayList<>());
			playerArtisanTasks.get(rs.getInt("player_id")).add(new PlayerArtisanTaskBreakdownDto(rs.getInt("player_id"), rs.getInt("item_id"), rs.getInt("amount")));
		});
	}
	
	public static void updateTask(int playerId, int itemId, int countToSubtract) {
		if (!playerArtisanTasks.containsKey(playerId))
			return;
		
		PlayerArtisanTaskBreakdownDto dto = playerArtisanTasks.get(playerId).stream()
				.filter(e -> e.getItemId() == itemId)
				.findFirst().orElse(null);
		
		if (dto == null)
			return;
		
		
		
		// we max-cap it at 0 as it could potentially go negative:
		// if the player's task is to make a steel bar (one iron, two coal) and they mine one coal, then smelt a steel bar
		// this function would have a -2 on the coal, but there's only 1 in the dto, causing it to become -1.
		int newCount = Math.max(dto.getAmount() - countToSubtract, 0);
		dto.setAmount(newCount);
		DatabaseUpdater.enqueue(UpdateArtisanTaskBreakdownEntity.builder().playerId(playerId).itemId(itemId).amount(newCount).build());
	}
	
	public static void clearTask(int playerId) {
		if (!playerArtisanTasks.containsKey(playerId))
			return;
		
		playerArtisanTasks.remove(playerId).forEach(dto -> {
			DatabaseUpdater.enqueue(DeleteArtisanTaskBreakdownEntity.builder().playerId(playerId).itemId(dto.getItemId()).build());
		});
	}
	
	public static void addTaskItem(int playerId, int itemId, int amount) {
		playerArtisanTasks.putIfAbsent(playerId, new ArrayList<>());
		playerArtisanTasks.get(playerId).add(new PlayerArtisanTaskBreakdownDto(playerId, itemId, amount));
		
		DatabaseUpdater.enqueue(InsertArtisanTaskBreakdownEntity.builder().playerId(playerId).itemId(itemId).amount(amount).build());
	}
	
	public static List<PlayerArtisanTaskBreakdownDto> getTaskList(int playerId) {
		return playerArtisanTasks.get(playerId);
	}
	
	public static boolean taskIsValid(int playerId, int itemId) {
		if (!playerArtisanTasks.containsKey(playerId))
			return false;
		Optional<PlayerArtisanTaskBreakdownDto> dto = playerArtisanTasks.get(playerId).stream().filter(e -> e.getItemId() == itemId).findFirst();
		return dto.isPresent() && dto.get().getAmount() > 0;
	}
	
	public static boolean taskInProgress(int playerId) {
		PlayerArtisanTaskDto task = PlayerArtisanTaskDao.getTask(playerId);
		if (task == null)
			return false;
		
		return taskIsValid(playerId, task.getItemId());
	}
	
	public static int getRemainingTaskCount(int playerId, int itemId) {
		if (!playerArtisanTasks.containsKey(playerId))
			return 0;
		Optional<PlayerArtisanTaskBreakdownDto> dto = playerArtisanTasks.get(playerId).stream().filter(e -> e.getItemId() == itemId).findFirst();
		if (!dto.isPresent())
			return 0;
		return dto.get().getAmount();
	}
}
