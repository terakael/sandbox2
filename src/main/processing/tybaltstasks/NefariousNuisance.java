package main.processing.tybaltstasks;

import main.database.dao.PlayerTybaltsTaskDao;
import main.database.dto.PlayerTybaltsTaskDto;
import main.processing.Player;
import main.processing.tybaltstasks.updates.KillNpcTaskUpdate;
import main.processing.tybaltstasks.updates.TybaltsTaskUpdate;
import main.responses.ResponseMaps;

public class NefariousNuisance extends TybaltsTask {

	@Override
	public void process(PlayerTybaltsTaskDto currentTask, Player player, TybaltsTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		// TODO this task splits the character path.
		// this task begins by tybalt telling the player to kill the nefarious man, and do not talk to him as he's a deciever.
		
		// path 1: killing the nefarious man
		// - "imposter" tybalt starts giving out more and more evil tasks
		// - killing guards
		// - poisoning people
		// - casting spells on people
		// - eventually killing the king.
		// - turns out the imposter isn't an imposter, and he actually has noble reasons for the above evil tasks (in cahoots with the nefarious man).
		
		// path 2: talking to the nefarious man
		// - nefarious man knows you and why you're here.  claims he's tybalt.
		// - the imposter tybalt is a demon who overpowered him and took his form, and he needs to be stopped.
		// - you start taking tasks from the nefarious man (in the basement of the house to the south)
		// - the tasks all lead up to killing the "imposter" tybalt, who actually turns out to be the real tybalt.
		// - as you kill tybalt, the nefarious man's spell breaks and you realise you've been decieved.  just like that dream.
		// - you talked to the nefarious man, who tybalt said is a deciever, and you believed him.  fool.
		
		if (taskUpdate instanceof KillNpcTaskUpdate) {
			if (currentTask.getProgress1() == 0 && ((KillNpcTaskUpdate)taskUpdate).getNpcId() == 47) {
				PlayerTybaltsTaskDao.updateProgress(player.getId(), 1, 1);
				taskUpdateMessage(completionMessage, player, responseMaps);
			}
		}
	}

	@Override
	public void initNewTask(PlayerTybaltsTaskDto currentTask, Player player, ResponseMaps responseMaps) {
		taskUpdateMessage("new task: kill the nefarious man lurking in north-east tyrotown.", player, responseMaps);
		message("he only seems to show up at night, so be prepared when the sun falls.", player, responseMaps);
	}

}
