package main.processing.tybaltstasks;

import java.util.Set;

import main.database.dao.PlayerTybaltsTaskDao;
import main.database.dto.PlayerTybaltsTaskDto;
import main.processing.attackable.Player;
import main.processing.tybaltstasks.updates.KillNpcTaskUpdate;
import main.processing.tybaltstasks.updates.TybaltsTaskUpdate;
import main.responses.ResponseMaps;

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

}
