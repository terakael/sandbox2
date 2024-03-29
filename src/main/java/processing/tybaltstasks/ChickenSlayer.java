package processing.tybaltstasks;

import java.util.Set;

import database.dao.PlayerTybaltsTaskDao;
import database.dto.PlayerTybaltsTaskDto;
import processing.attackable.Player;
import processing.tybaltstasks.updates.KillNpcTaskUpdate;
import processing.tybaltstasks.updates.TybaltsTaskUpdate;
import responses.ResponseMaps;

public class ChickenSlayer extends TybaltsTask {
	private static Set<Integer> validChickenIds = Set.<Integer>of(4, 9); // chickens and roosters and other similar animals are all fair play
	private final static int requiredKills = 5;
	
	@Override
	public void initNewTask(PlayerTybaltsTaskDto currentTask, Player player, ResponseMaps responseMaps) {
		taskUpdateMessage("new task: chicken slayer.", player, responseMaps);
		message("go to a chicken coop and slay five chickens, tough guy.", player, responseMaps);
	}

	@Override
	public void process(PlayerTybaltsTaskDto currentTask, Player player, TybaltsTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		if (!(taskUpdate instanceof KillNpcTaskUpdate))
			return;
		
		final int npcId = ((KillNpcTaskUpdate)taskUpdate).getNpcId();
		if (currentTask.getProgress1() < 5 && validChickenIds.contains(npcId)) {
			PlayerTybaltsTaskDao.updateProgress(player.getId(), 1, currentTask.getProgress1() + 1);
			if (currentTask.getProgress1() == requiredKills) {
				taskUpdateMessage(completionMessage, player, responseMaps);
			} else {
				final String message = String.format("task updated: %d/%d chickens slayed.", currentTask.getProgress1(), requiredKills);
				message(message, player, responseMaps);
			}
		}
	}
	
	@Override
	public boolean isFinished(int playerId) {
		final PlayerTybaltsTaskDto currentTask = PlayerTybaltsTaskDao.getCurrentTaskByPlayerId(playerId);
		return currentTask == null || currentTask.getProgress1() == requiredKills;
	}

}
