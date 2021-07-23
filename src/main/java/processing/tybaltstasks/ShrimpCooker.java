package processing.tybaltstasks;

import database.dao.PlayerTybaltsTaskDao;
import database.dto.PlayerTybaltsTaskDto;
import processing.attackable.Player;
import processing.tybaltstasks.updates.CookTaskUpdate;
import processing.tybaltstasks.updates.FishTaskUpdate;
import processing.tybaltstasks.updates.TybaltsTaskUpdate;
import responses.ResponseMaps;
import types.Items;

public class ShrimpCooker extends TybaltsTask {
	private static final int shrimpToCook = 10;
	
	@Override
	public void initNewTask(PlayerTybaltsTaskDto currentTask, Player player, ResponseMaps responseMaps) {
		taskUpdateMessage("new task: shrimp on the barbie.", player, responseMaps);
		message("first, find a net and catch some shrimp near the water in tyrotown.", player, responseMaps);
	}

	@Override
	public void process(PlayerTybaltsTaskDto currentTask, Player player, TybaltsTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		if (taskUpdate instanceof FishTaskUpdate)
			processFishing(currentTask, player, (FishTaskUpdate)taskUpdate, responseMaps);
		
		else if (taskUpdate instanceof CookTaskUpdate)
			processCooking(currentTask, player, (CookTaskUpdate)taskUpdate, responseMaps);
	}
	
	private void processFishing(PlayerTybaltsTaskDto currentTask, Player player, FishTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		if (currentTask.getProgress1() < shrimpToCook && taskUpdate.getFishedItemId() == Items.RAW_SHRIMPS.getValue()) {
			PlayerTybaltsTaskDao.updateProgress(player.getId(), 1, currentTask.getProgress1() + 1);
			message(String.format("task updated: %d/%d shrimp caught.", currentTask.getProgress1(), shrimpToCook), player, responseMaps);
			if (currentTask.getProgress1() == shrimpToCook) {
				// caught all our shrimp
				message("you can cook the shrimp on a fire to finish the task.", player, responseMaps);
			}
		}
	}
	
	private void processCooking(PlayerTybaltsTaskDto currentTask, Player player, CookTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		if (currentTask.getProgress2() >= shrimpToCook)
			return;
		
		if (currentTask.getProgress2() >= currentTask.getProgress1()) {
			message("you need to catch the shrimp yourself in order to fulfill tybalt's task requirements.", player, responseMaps);
			return;
		}
		
		if (taskUpdate.getCookedItemId() == Items.SHRIMPS.getValue()) {
			if (!taskUpdate.isBurnt()) { 
				PlayerTybaltsTaskDao.updateProgress(player.getId(), 2, currentTask.getProgress2() + 1);
				
				final String message = String.format("task updated: %d/%d shrimp cooked.", currentTask.getProgress2(), shrimpToCook);
				message(message, player, responseMaps);
				
				// because this is a beginner task, we'll give a tip to the player if they've fished less than 10 shrimp
				if (currentTask.getProgress1() < shrimpToCook && currentTask.getProgress2() == currentTask.getProgress1()) {
					message("you've cooked all of the shrimp you caught - the next shrimp you cook won't count towards the task.", player, responseMaps);
				}
				else if (currentTask.getProgress2() == shrimpToCook) {
					taskUpdateMessage(completionMessage, player, responseMaps);
				}
			} else {
				message("oops, you burnt it.", player, responseMaps);
				message("burnt shrimp doesn't count towards the task; you'll need to buy or fish some more.", player, responseMaps);
			}
		}
	}
}
