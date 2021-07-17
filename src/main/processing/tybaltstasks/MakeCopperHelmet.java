package main.processing.tybaltstasks;

import main.database.dao.PlayerTybaltsTaskDao;
import main.database.dto.PlayerTybaltsTaskDto;
import main.processing.Player;
import main.processing.tybaltstasks.updates.MineTaskUpdate;
import main.processing.tybaltstasks.updates.SmeltTaskUpdate;
import main.processing.tybaltstasks.updates.SmithTaskUpdate;
import main.processing.tybaltstasks.updates.TybaltsTaskUpdate;
import main.responses.ResponseMaps;
import main.types.Items;

public class MakeCopperHelmet extends TybaltsTask {
	private static final int requiredOresAndBars = 3; // three ores/bars make a helmet
	
	@Override
	public void initNewTask(PlayerTybaltsTaskDto currentTask, Player player, ResponseMaps responseMaps) {
		taskUpdateMessage("new task: make a copper helmet.", player, responseMaps);
		message("first, find a pickaxe and mine three copper ores in tyrotown's south-western mines.", player, responseMaps);
	}
	
	@Override
	public void process(PlayerTybaltsTaskDto currentTask, Player player, TybaltsTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		if (taskUpdate instanceof MineTaskUpdate) {
			handleMining(currentTask, player, (MineTaskUpdate)taskUpdate, responseMaps);
		}
		
		else if (taskUpdate instanceof SmeltTaskUpdate) {
			handleSmelting(currentTask, player, (SmeltTaskUpdate)taskUpdate, responseMaps);
		}
		
		else if (taskUpdate instanceof SmithTaskUpdate) {
			handleSmithing(currentTask, player, (SmithTaskUpdate)taskUpdate, responseMaps);
		}
	}
	
	private void handleMining(PlayerTybaltsTaskDto currentTask, Player player, MineTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		if (currentTask.getProgress1() >= requiredOresAndBars)
			return; // we've done the mining part
		
		if (taskUpdate.getMinedItemId() == Items.COPPER_ORE.getValue()) {
			PlayerTybaltsTaskDao.updateProgress(player.getId(), 1, currentTask.getProgress1() + 1);
			
			final String message = String.format("task updated: %d/%d copper mined.", currentTask.getProgress1(), requiredOresAndBars);
			message(message, player, responseMaps);
			
			if (currentTask.getProgress1() == requiredOresAndBars) {
				message("there's a furnace in northern tyrotown where you can smelt the ore into bars.", player, responseMaps);
			}
		}
	}
	
	private void handleSmelting(PlayerTybaltsTaskDto currentTask, Player player, SmeltTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		if (currentTask.getProgress2() >= requiredOresAndBars)
			return;
		
		// we can mine one ore, then smelt the one ore, then mine another if we want.
		if (currentTask.getProgress2() >= currentTask.getProgress1()) {
			error("you need to mine the copper yourself in order to fulfill tybalt's task requirements.", player, responseMaps);
			return;
		}
		
		if (taskUpdate.getBarId() == Items.COPPER_BAR.getValue()) {
			// we made a copper bar so increment the counder
			PlayerTybaltsTaskDao.updateProgress(player.getId(), 2, currentTask.getProgress2() + 1);
			
			final String message = String.format("task updated: %d/%d copper ore smelted.", currentTask.getProgress2(), requiredOresAndBars);
			message(message, player, responseMaps);
			
			// because this is a beginner task, we'll give a tip to the player if they've mined less than 3 rocks
			if (currentTask.getProgress1() < requiredOresAndBars && currentTask.getProgress2() == currentTask.getProgress1()) {
				// we've just caught up our smelting to however many rocks we've mined, so future smelts aren't going to count.
				message("you've smelted all of the ore you mined - the next copper smelt won't count towards the task.", player, responseMaps);
			}
			else if (currentTask.getProgress2() == requiredOresAndBars) {
				message("you can smith a copper helmet at the anvils south-east of tybalt.", player, responseMaps);
			}
		}
	}
	
	private void handleSmithing(PlayerTybaltsTaskDto currentTask, Player player, SmithTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		if (taskUpdate.getSmithedItemId() == Items.COPPER_HELMET.getValue()) {
			if (currentTask.getProgress2() == 3) {
				PlayerTybaltsTaskDao.updateProgress(player.getId(), 3, 1);
				taskUpdateMessage(completionMessage, player, responseMaps);
			} else {
				// if the player tries to bypass the process we'll be nice about it because it's a beginner task
				// next time we'll fuckign drop their shit
				error("you need to mine and smelt the bars yourself to fulfill tybalt's task requirements.", player, responseMaps);
			}
		}
	}
}
