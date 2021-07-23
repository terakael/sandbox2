package processing.tybaltstasks;

import java.util.Set;

import database.dao.PlayerTybaltsTaskDao;
import database.dto.PlayerTybaltsTaskDto;
import processing.attackable.Player;
import processing.managers.ConstructableManager;
import processing.tybaltstasks.updates.ConstructTaskUpdate;
import processing.tybaltstasks.updates.KillNpcTaskUpdate;
import processing.tybaltstasks.updates.SawmillTaskUpdate;
import processing.tybaltstasks.updates.TybaltsTaskUpdate;
import responses.ResponseMaps;
import types.Items;

public class BoneTotem extends TybaltsTask {
	private static Set<Integer> validChickenIds = Set.<Integer>of(4, 9); // chickens and roosters and other similar animals are all fair play
	private final static int requiredPlanks = 2;
	private final static int requiredKills = 5;

	@Override
	public void process(PlayerTybaltsTaskDto currentTask, Player player, TybaltsTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		if (taskUpdate instanceof ConstructTaskUpdate)
			handleConstruct(currentTask, player, (ConstructTaskUpdate)taskUpdate, responseMaps);
		
		else if (taskUpdate instanceof KillNpcTaskUpdate)
			handleNpcKill(currentTask, player, (KillNpcTaskUpdate)taskUpdate, responseMaps);
		
		else if (taskUpdate instanceof SawmillTaskUpdate)
			handleSawmill(currentTask, player, (SawmillTaskUpdate)taskUpdate, responseMaps);
	}

	@Override
	public void initNewTask(PlayerTybaltsTaskDto currentTask, Player player, ResponseMaps responseMaps) {
		taskUpdateMessage("new task: the bone totem pole.", player, responseMaps);
		message("first task: gather some logs and make two planks at the sawmill in north-east tyrotown.", player, responseMaps);
	}
	
	private void handleConstruct(PlayerTybaltsTaskDto currentTask, Player player, ConstructTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		if (currentTask.getProgress2() == 1) // already done
			return;
		
		if (taskUpdate.getSceneryId() == 129) { // bone totem pole
			if (currentTask.getProgress1() < requiredPlanks) {
				error("you need to build the bone totem pole using planks you made in order to satisfy tybalt's requirements.", player, responseMaps);
				return;
			}
			
			PlayerTybaltsTaskDao.updateProgress(player.getId(), 2, 1);
			taskUpdateMessage("task updated: bone totem pole built.", player, responseMaps);
			taskUpdateMessage("next task: kill five chickens under the aura of the bone totem pole.", player, responseMaps);
		}
	}
	
	private void handleSawmill(PlayerTybaltsTaskDto currentTask, Player player, SawmillTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		if (currentTask.getProgress1() < requiredPlanks) {
			if (taskUpdate.getCreatedPlankId() == Items.PLANK.getValue()) {
				PlayerTybaltsTaskDao.updateProgress(player.getId(), 1, currentTask.getProgress1() + 1);
				taskUpdateMessage(String.format("task updated: %d/%d planks made.", currentTask.getProgress1(), requiredPlanks), player, responseMaps);
				
				if (currentTask.getProgress1() == requiredPlanks) {
					taskUpdateMessage("next task: build the bone totem at the nearest chicken coop.", player, responseMaps);
					message("a bone totem pole requires two planks and three bones, so you'll need to get some bones if you don't have any.", player, responseMaps);
				}
			}
		}
	}
	
	private void handleNpcKill(PlayerTybaltsTaskDto currentTask, Player player, KillNpcTaskUpdate taskUpdate, ResponseMaps responseMaps) {		
		if (!validChickenIds.contains(taskUpdate.getNpcId()) || currentTask.getProgress3() == requiredKills)
			return;
		
		final boolean totemPoleBuilt = currentTask.getProgress2() == 1;
		
		final boolean totemPoleInRange = ConstructableManager.constructableIsInRadius(taskUpdate.getNpcDeathFloor(), taskUpdate.getNpcDeathTileId(), 129, 3);
		
		// if you kill a chicken anywhere during this task it will be annoying to get the message saying you're out of range
		// so we check if we are within six tiles of one.
		final boolean totemPoleWithinSixTiles = ConstructableManager.constructableIsInRadius(taskUpdate.getNpcDeathFloor(), taskUpdate.getNpcDeathTileId(), 129, 6);
		
		if (totemPoleInRange) {
			if (!totemPoleBuilt) {
				error("you need to build your own totem pole in order to satisfy tybalt's requirements.", player, responseMaps);
				return;
			} else {
				PlayerTybaltsTaskDao.updateProgress(player.getId(), 3, currentTask.getProgress3() + 1);
				final String message = String.format("task updated: %d/%d chickens killed near the bone totem pole.", currentTask.getProgress3(), requiredKills);
				taskUpdateMessage(message, player, responseMaps);
				
				if (currentTask.getProgress3() == requiredKills)
					taskUpdateMessage(completionMessage, player, responseMaps);
			}
		} else if (totemPoleWithinSixTiles && totemPoleBuilt) {
			// you've built a totem pole but you're not within range
			error("you aren't in range of a bone totem pole so the kill doesn't count towards your task.", player, responseMaps);
			return;
		}		
	}
}
