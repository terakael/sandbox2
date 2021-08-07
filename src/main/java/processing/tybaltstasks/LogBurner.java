package processing.tybaltstasks;

import java.util.Set;

import database.dao.PlayerTybaltsTaskDao;
import database.dto.PlayerTybaltsTaskDto;
import processing.attackable.Player;
import processing.tybaltstasks.updates.ChopTaskUpdate;
import processing.tybaltstasks.updates.ConstructTaskUpdate;
import processing.tybaltstasks.updates.TybaltsTaskUpdate;
import responses.ResponseMaps;

public class LogBurner extends TybaltsTask {
	
	@Override
	public void process(PlayerTybaltsTaskDto currentTask, Player player, TybaltsTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		// progress1: cut a log
		if (currentTask.getProgress1() == 0) {
			if (taskUpdate instanceof ChopTaskUpdate) {
				// any tree is valid.  technically you could cut a magic tree and then burn a maple log, still works, who cares.
				PlayerTybaltsTaskDao.updateProgress(player.getId(), 1, 1);
				
				message("task updated: 1/1 logs cut.", player, responseMaps);
				message("use your tinderbox on the logs to finish the task.", player, responseMaps);
			}
		} else if (currentTask.getProgress1() == 1 && currentTask.getProgress2() == 0) {
			// progress2: light it on fire
			if (taskUpdate instanceof ConstructTaskUpdate) {
				final Set<Integer> fires = Set.<Integer>of(122, 123, 124, 125, 126, 127); // regular -> magic fires
				if (fires.contains(((ConstructTaskUpdate)taskUpdate).getSceneryId())) {
					PlayerTybaltsTaskDao.updateProgress(player.getId(), 2, 1);
					taskUpdateMessage(completionMessage, player, responseMaps);
				}
			}
		}
	}

	@Override
	public void initNewTask(PlayerTybaltsTaskDto currentTask, Player player, ResponseMaps responseMaps) {
		taskUpdateMessage("new task: log burner.", player, responseMaps);
		message("use a hatchet to cut logs from the nearest tree, then burn the logs with a tinderbox.", player, responseMaps);
	}

	@Override
	public boolean isFinished(int playerId) {
		final PlayerTybaltsTaskDto currentTask = PlayerTybaltsTaskDao.getCurrentTaskByPlayerId(playerId);
		return currentTask == null || currentTask.getProgress2() == 1;
	}

}
