package main.processing;

import java.util.HashMap;
import java.util.Map;

import main.database.dao.PlayerTybaltsTaskDao;
import main.database.dto.PlayerTybaltsTaskDto;
import main.responses.ResponseMaps;
import main.tybaltstasks.BoneTotem;
import main.tybaltstasks.BrewerOfStank;
import main.tybaltstasks.ChickenSlayer;
import main.tybaltstasks.LogBurner;
import main.tybaltstasks.MakeCopperHelmet;
import main.tybaltstasks.NefariousNuisance;
import main.tybaltstasks.ReinforceCopperHelmet;
import main.tybaltstasks.ShrimpCooker;
import main.tybaltstasks.ShrineMaker;
import main.tybaltstasks.TybaltsTask;
import main.tybaltstasks.updates.TybaltsTaskUpdate;

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
