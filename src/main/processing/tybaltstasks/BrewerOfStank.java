package main.processing.tybaltstasks;

import main.database.dao.PlayerTybaltsTaskDao;
import main.database.dto.PlayerTybaltsTaskDto;
import main.processing.attackable.Player;
import main.processing.tybaltstasks.updates.ItemDropFromNpcUpdate;
import main.processing.tybaltstasks.updates.TakeTaskUpdate;
import main.processing.tybaltstasks.updates.TybaltsTaskUpdate;
import main.processing.tybaltstasks.updates.UseItemOnItemTaskUpdate;
import main.responses.ResponseMaps;
import main.types.Items;

public class BrewerOfStank extends TybaltsTask {
	private static final String killGoblinTask = "next task: kill goblins south of tyrotown until you receive goblin nails.";
	private static final String makeBluebellMixTask = "next task: mix the dark bluebell into a vial.";
	private static final String mixInGoblinNails = "next task: add the goblin nails into the bluebell mix to make the goblin stank.";
	
	@Override
	public void initNewTask(PlayerTybaltsTaskDto currentTask, Player player, ResponseMaps responseMaps) {
		taskUpdateMessage("new task: brewer of stank.", player, responseMaps);
		message("firstly, kill goblins south of tyrotown until you receive goblin nails.", player, responseMaps);
	}

	@Override
	public void process(PlayerTybaltsTaskDto currentTask, Player player, TybaltsTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		if (taskUpdate instanceof ItemDropFromNpcUpdate)
			processNpcDropTask(currentTask, player, (ItemDropFromNpcUpdate)taskUpdate, responseMaps);
		
		else if (taskUpdate instanceof TakeTaskUpdate)
			processTakeTask(currentTask, player, (TakeTaskUpdate)taskUpdate, responseMaps);
		
		else if (taskUpdate instanceof UseItemOnItemTaskUpdate)
			processUseItemOnItemTask(currentTask, player, (UseItemOnItemTaskUpdate)taskUpdate, responseMaps);
	}
	
	private void processNpcDropTask(PlayerTybaltsTaskDto currentTask, Player player, ItemDropFromNpcUpdate taskUpdate, ResponseMaps responseMaps) {
		if (currentTask.getProgress1() == 2) // already done
			return;
		
		if (taskUpdate.getItemId() == Items.GOBLIN_NAILS.getValue()) {
			// we'll show the goblin nails message every time they drop until the player picks some up
			if (currentTask.getProgress1() == 0)
				PlayerTybaltsTaskDao.updateProgress(player.getId(), 1, 1);
			message("task updated: goblin nails dropped - pick them up!", player, responseMaps);
		}
	}
	
	private void processTakeTask(PlayerTybaltsTaskDto currentTask, Player player, TakeTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		if (currentTask.getProgress1() != 1) // not dropped from goblin (0) or already picked up (2)
			return;
		
		if (taskUpdate.getPickedUpItemId() == Items.GOBLIN_NAILS.getValue()) {
			PlayerTybaltsTaskDao.updateProgress(player.getId(), 1, 2);
			message("task updated: goblin nails acquired.", player, responseMaps);
			
			// killed the goblin and picked the bluebell but haven't mixed the bluebell into the vial
			if (currentTask.getProgress2() == 0)
				taskUpdateMessage(makeBluebellMixTask, player, responseMaps);
			
			// killed the goblin, picked the bluebell, mixed it into the vial
			else 
				taskUpdateMessage(mixInGoblinNails, player, responseMaps);
		}
	}
	
	private void processUseItemOnItemTask(PlayerTybaltsTaskDto currentTask, Player player, UseItemOnItemTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		// progress1 p1: get goblin nail drop from goblin
		// progress1 p2: take goblin nails from ground
		// progress2 p1: mix bluebell into vial
		// progress2 p2: mix goblin nail into bluebell mix
		final Items resultingItemId = Items.withValue(taskUpdate.getResultingItemId());
		if (resultingItemId == null)
			return;
		
		switch (resultingItemId)  {
		case DARK_BLUEBELL_MIX: {
			if (currentTask.getProgress2() == 1) // already done
				return;
			
			PlayerTybaltsTaskDao.updateProgress(player.getId(), 2, 1);
			message("task updated: dark bluebell mix created.", player, responseMaps);
			
			if (currentTask.getProgress1() < 2) // 0 means not dropped from goblin yet, 1 means dropped but not picked up
				taskUpdateMessage(killGoblinTask, player, responseMaps);
			else
				taskUpdateMessage(mixInGoblinNails, player, responseMaps);
			
			break;
		}
		
		case GOBLIN_STANK_4: {
			if (currentTask.getProgress2() == 2) // already done
				return;
			
			if (currentTask.getProgress1() < 2) {
				error("you need to use goblin nails you got from a goblin to satisfy tybalt's requirements.", player, responseMaps);
				return;
			}
			
			if (currentTask.getProgress2() == 0) {
				error("you need to mix your own bluebell mix to satisfy tybalt's requirements.", player, responseMaps);
				return;
			}
			
			PlayerTybaltsTaskDao.updateProgress(player.getId(), 2, 2);
			taskUpdateMessage(completionMessage, player, responseMaps);
			break;
		}
		
		default:
			break;
		}
	}
}
