package processing.tybaltstasks;

import database.dao.PlayerTybaltsTaskDao;
import database.dto.PlayerTybaltsTaskDto;
import processing.attackable.Player;
import processing.tybaltstasks.updates.CompleteArtisanTaskUpdate;
import processing.tybaltstasks.updates.TybaltsTaskUpdate;
import responses.ResponseMaps;

public class RealArtisan extends TybaltsTask {
	private static final int tasksToComplete = 3;

	@Override
	public void initNewTask(PlayerTybaltsTaskDto currentTask, Player player, ResponseMaps responseMaps) {
		taskUpdateMessage("new task: real artisan.", player, responseMaps);
		message("complete three artisan tasks assigned by alaina.", player, responseMaps);
	}
	
	@Override
	public void process(PlayerTybaltsTaskDto currentTask, Player player, TybaltsTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		if (taskUpdate instanceof CompleteArtisanTaskUpdate) {
			if (((CompleteArtisanTaskUpdate)taskUpdate).getAssignedMasterId() == 58) { // alaina
				if (currentTask.getProgress1() < tasksToComplete) {
					PlayerTybaltsTaskDao.updateProgress(player.getId(), 1, currentTask.getProgress1() + 1);
					if (currentTask.getProgress1() == tasksToComplete) {
						taskUpdateMessage(completionMessage, player, responseMaps);
					} else {
						final String message = String.format("task updated: %d/%d artisan tasks completed.", currentTask.getProgress1(), tasksToComplete);
						message(message, player, responseMaps);
					}
				}
			}
		}
	}

	@Override
	public boolean isFinished(int playerId) {
		final PlayerTybaltsTaskDto currentTask = PlayerTybaltsTaskDao.getCurrentTaskByPlayerId(playerId);
		return currentTask == null || currentTask.getProgress1() == tasksToComplete;
	}

}
