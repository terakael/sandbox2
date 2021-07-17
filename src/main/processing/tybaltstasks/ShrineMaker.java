package main.processing.tybaltstasks;

import main.database.dao.PlayerTybaltsTaskDao;
import main.database.dto.PlayerTybaltsTaskDto;
import main.processing.Player;
import main.processing.tybaltstasks.updates.ConstructTaskUpdate;
import main.processing.tybaltstasks.updates.PickTaskUpdate;
import main.processing.tybaltstasks.updates.SawmillTaskUpdate;
import main.processing.tybaltstasks.updates.TybaltsTaskUpdate;
import main.responses.ResponseMaps;
import main.types.Items;

public class ShrineMaker extends TybaltsTask {
	private static final int requiredPlanks = 3;
	private static final int requiredFlowers = 5;
	
	private static final String buildTask = "next task: use a hammer on the planks to build the nature's shrine.";

	@Override
	public void process(PlayerTybaltsTaskDto currentTask, Player player, TybaltsTaskUpdate taskUpdate, ResponseMaps responseMaps) {		
		if (taskUpdate instanceof SawmillTaskUpdate)
			handlePlankMake(currentTask, player, (SawmillTaskUpdate)taskUpdate, responseMaps);
		
		else if (taskUpdate instanceof PickTaskUpdate)
			handlePick(currentTask, player, (PickTaskUpdate)taskUpdate, responseMaps);
		
		else if (taskUpdate instanceof ConstructTaskUpdate)
			handleConstruct(currentTask, player, (ConstructTaskUpdate)taskUpdate, responseMaps);
	}

	@Override
	public void initNewTask(PlayerTybaltsTaskDto currentTask, Player player, ResponseMaps responseMaps) {
		taskUpdateMessage("new task: build a nature's shrine.", player, responseMaps);
		message("first task: make three planks on the sawmill in north-east tyrotown.", player, responseMaps);
		message("it takes three logs to make a plank, so you need nine logs total.", player, responseMaps);
	}

	private void handlePlankMake(PlayerTybaltsTaskDto currentTask, Player player, SawmillTaskUpdate taskUpdate, ResponseMaps responseMaps) {		
		// note that for this task they don't need to cut the logs themselves if they don't want to.
		if (currentTask.getProgress1() < requiredPlanks) {
			if (taskUpdate.getCreatedPlankId() != Items.PLANK.getValue())
				return;
			
			PlayerTybaltsTaskDao.updateProgress(player.getId(), 1, currentTask.getProgress1() + 1);
			taskUpdateMessage(String.format("task updated: %s/%s planks made.", currentTask.getProgress1(), requiredPlanks), player, responseMaps);
			
			if (currentTask.getProgress1() == requiredPlanks) {
				if (currentTask.getProgress2() < requiredFlowers) {
					taskUpdateMessage("next task: pick five red russines.", player, responseMaps);
					message("a red russine patch can be found south-east of the marketplace, near the oak trees.", player, responseMaps);
				} else {
					taskUpdateMessage(buildTask, player, responseMaps);
					message("you can buy a hammer at a general store if you don't already have one.", player, responseMaps);
				}
			}
		}
	}
	
	private void handlePick(PlayerTybaltsTaskDto currentTask, Player player, PickTaskUpdate taskUpdate, ResponseMaps responseMaps) {		
		if (currentTask.getProgress2() < requiredFlowers) {
			if (taskUpdate.getPickedItemId() != Items.RED_RUSSINE.getValue())
				return;
			
			PlayerTybaltsTaskDao.updateProgress(player.getId(), 2, currentTask.getProgress2() + 1);
			taskUpdateMessage(String.format("task updated: %s/%s red russines picked.", currentTask.getProgress2(), requiredFlowers), player, responseMaps);
			
			if (currentTask.getProgress2() == requiredFlowers) {
				if (currentTask.getProgress1() < requiredPlanks) {
					taskUpdateMessage("next task: make three planks on the sawmill in north-east tyrotown.", player, responseMaps);
					message("it takes three logs to make a plank, so you need nine logs total.", player, responseMaps);
				} else {
					taskUpdateMessage(buildTask, player, responseMaps);
					message("you can buy a hammer at a general store if you don't already have one.", player, responseMaps);
				}
			}
		}
	}
	
	private void handleConstruct(PlayerTybaltsTaskDto currentTask, Player player, ConstructTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		if (currentTask.getProgress3() == 1) // already done
			return;
		
		if (taskUpdate.getSceneryId() != 140) // nature's shrine
			return;
		
		if (currentTask.getProgress1() < requiredPlanks) {
			error("you need to use planks you made yourself in order to fulfill tybalt's task requirement.", player, responseMaps);
			return;
		}
		
		if (currentTask.getProgress2() < requiredFlowers) {
			error("you need to use flowers you picked yourself in order to fulfill tybalt's task requirement.", player, responseMaps);
			return;
		}
		
		PlayerTybaltsTaskDao.updateProgress(player.getId(), 3, 1);
		taskUpdateMessage(completionMessage, player, responseMaps);
	}
}
