package processing.tybaltstasks;

import database.dao.PlayerTybaltsTaskDao;
import database.dto.PlayerTybaltsTaskDto;
import processing.attackable.Player;
import processing.tybaltstasks.updates.TybaltsTaskUpdate;
import processing.tybaltstasks.updates.UseItemOnItemTaskUpdate;
import responses.ResponseMaps;
import types.Items;

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
