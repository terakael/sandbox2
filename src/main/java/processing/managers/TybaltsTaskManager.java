package processing.managers;

import java.util.HashMap;
import java.util.Map;

import database.dao.PlayerTybaltsTaskDao;
import database.dto.PlayerTybaltsTaskDto;
import processing.attackable.Player;
import processing.tybaltstasks.BoneTotem;
import processing.tybaltstasks.BrewerOfStank;
import processing.tybaltstasks.ChickenSlayer;
import processing.tybaltstasks.LogBurner;
import processing.tybaltstasks.MakeCopperHelmet;
import processing.tybaltstasks.NefariousNuisance;
import processing.tybaltstasks.ReinforceCopperHelmet;
import processing.tybaltstasks.ShrimpCooker;
import processing.tybaltstasks.ShrineMaker;
import processing.tybaltstasks.TybaltsTask;
import processing.tybaltstasks.updates.TybaltsTaskUpdate;
import responses.ResponseMaps;

public class TybaltsTaskManager {	
	private static Map<Integer, TybaltsTask> tasks;
	static {
		tasks = new HashMap<>();
		tasks.put(1, new LogBurner());
		tasks.put(2, new MakeCopperHelmet());
		tasks.put(3, new ReinforceCopperHelmet());
		tasks.put(4, new ChickenSlayer());
		tasks.put(5, new ShrimpCooker());
		tasks.put(6, new BoneTotem());
		tasks.put(7, new BrewerOfStank());
		tasks.put(8, new ShrineMaker());
		tasks.put(9, new NefariousNuisance());
	}
	
	public static void check(Player player, TybaltsTaskUpdate taskUpdate, ResponseMaps responseMaps) {
		PlayerTybaltsTaskDto currentTask = PlayerTybaltsTaskDao.getCurrentTaskByPlayerId(player.getId());
		if (currentTask == null)
			return;
		
		if (tasks.containsKey(currentTask.getTaskId()))
			tasks.get(currentTask.getTaskId()).process(currentTask, player, taskUpdate, responseMaps);
	}
	
	public static void initNewTask(Player player, ResponseMaps responseMaps) {
		PlayerTybaltsTaskDto currentTask = PlayerTybaltsTaskDao.getCurrentTaskByPlayerId(player.getId());
		if (currentTask == null)
			return;
		
		if (tasks.containsKey(currentTask.getTaskId()))
			tasks.get(currentTask.getTaskId()).initNewTask(currentTask, player, responseMaps);
	}
}
