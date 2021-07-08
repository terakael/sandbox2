package main.tybaltstasks;

import main.database.dao.PlayerTybaltsTaskDao;
import main.database.dto.PlayerTybaltsTaskDto;
import main.processing.Player;
import main.responses.ResponseMaps;
import main.tybaltstasks.updates.TybaltsTaskUpdate;
import main.tybaltstasks.updates.UseItemOnItemTaskUpdate;
import main.types.Items;

public class ReinforceCopperHelmet extends TybaltsTask {
	
	@Override
	public void initNewTask(PlayerTybaltsTaskDto currentTask, Player player, ResponseMaps responseMaps) {
		taskUpdateMessage("new task: reinforce a copper helmet.", player, responseMaps);
		message("take the reinforcement tybalt gave you, and use it on your newly made copper helmet.", player, responseMaps);
	}

	@Override
	public void process(PlayerTybaltsTaskDto currentTask, Player player, TybaltsTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		if (!(taskUpdate instanceof UseItemOnItemTaskUpdate))
			return;
		
		if (currentTask.getProgress1() == 0 && ((UseItemOnItemTaskUpdate)taskUpdate).getResultingItemId() == Items.REINFORCED_COPPER_HELMET.getValue()) {
			// of course the task check is only executed on success so we don't need to check anything like the required materials
			PlayerTybaltsTaskDao.updateProgress(player.getId(), 1, 1);
			taskUpdateMessage(completionMessage, player, responseMaps);
		}
	}

}
