package database.dao;

import java.util.HashMap;
import java.util.Map;

import database.DbConnection;
import database.dto.PlayerTybaltsTaskDto;
import database.entity.insert.InsertTybaltsTaskEntity;
import database.entity.update.UpdateTybaltsTaskEntity;
import processing.attackable.Player;
import processing.managers.DatabaseUpdater;
import processing.managers.TybaltsTaskManager;
import responses.ResponseMaps;

public class PlayerTybaltsTaskDao {
	private static Map<Integer, PlayerTybaltsTaskDto> tasksByPlayerId = new HashMap<>(); // playerId, dto
	
	public static void setupCaches() {
		setupPlayerTybaltsTasks();
	}
	
	private static void setupPlayerTybaltsTasks() {
		final String query = "select player_id, task_id, progress1, progress2, progress3, progress4 from player_tybalts_tasks";
		DbConnection.load(query, rs -> {
			tasksByPlayerId.put(rs.getInt("player_id"), new PlayerTybaltsTaskDto(
					rs.getInt("player_id"), rs.getInt("task_id"), rs.getInt("progress1"), rs.getInt("progress2"), rs.getInt("progress3"), rs.getInt("progress4")));
		});
	}
	
	public static PlayerTybaltsTaskDto getCurrentTaskByPlayerId(int playerId) {
		if (!tasksByPlayerId.containsKey(playerId))
			return null;
		return tasksByPlayerId.get(playerId);
	}
	
	public static void updateProgress(int playerId, int progressNumber, int newProgress) {
		if (!tasksByPlayerId.containsKey(playerId))
			return;
		
		switch (progressNumber) {
		case 1:
			tasksByPlayerId.get(playerId).setProgress1(newProgress);
			DatabaseUpdater.enqueue(UpdateTybaltsTaskEntity.builder().playerId(playerId).progress1(newProgress).build());
			break;
		case 2:
			tasksByPlayerId.get(playerId).setProgress2(newProgress);
			DatabaseUpdater.enqueue(UpdateTybaltsTaskEntity.builder().playerId(playerId).progress2(newProgress).build());
			break;
		case 3:
			tasksByPlayerId.get(playerId).setProgress3(newProgress);
			DatabaseUpdater.enqueue(UpdateTybaltsTaskEntity.builder().playerId(playerId).progress3(newProgress).build());
			break;
		case 4:
			tasksByPlayerId.get(playerId).setProgress4(newProgress);
			DatabaseUpdater.enqueue(UpdateTybaltsTaskEntity.builder().playerId(playerId).progress4(newProgress).build());
			break;
		}
	}
	
	public static void setNewTask(Player player, int taskId, ResponseMaps responseMaps) {
		final int playerId = player.getId();
		
		if (!tasksByPlayerId.containsKey(playerId)) {
			tasksByPlayerId.put(playerId, new PlayerTybaltsTaskDto(playerId, taskId, 0, 0, 0, 0));
			DatabaseUpdater.enqueue(InsertTybaltsTaskEntity.builder()
					.playerId(playerId)
					.taskId(taskId)
					.progress1(0)
					.progress2(0)
					.progress3(0)
					.progress4(0)
					.build());
		} else {
			tasksByPlayerId.get(playerId).setTaskId(taskId);
			tasksByPlayerId.get(playerId).setProgress1(0);
			tasksByPlayerId.get(playerId).setProgress2(0);
			tasksByPlayerId.get(playerId).setProgress3(0);
			tasksByPlayerId.get(playerId).setProgress4(0);
			DatabaseUpdater.enqueue(UpdateTybaltsTaskEntity.builder()
				.playerId(playerId)
				.taskId(taskId)
				.progress1(0)
				.progress2(0)
				.progress3(0)
				.progress4(0)
				.build());
		}
		
		TybaltsTaskManager.initNewTask(player, responseMaps);
	}
}
