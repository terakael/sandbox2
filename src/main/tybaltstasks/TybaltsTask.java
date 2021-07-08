package main.tybaltstasks;

import main.database.dto.PlayerTybaltsTaskDto;
import main.processing.Player;
import main.responses.MessageResponse;
import main.responses.ResponseMaps;
import main.tybaltstasks.updates.TybaltsTaskUpdate;

public abstract class TybaltsTask {
	final static String newTaskColour = "#23f5b4";
	final static String taskMessageColour = "#bada55"; // badass bruh
	final static String taskErrorColour = "#c48000";
	final static String completionMessage = "task complete; return to tybalt to claim your reward.";
	
	public abstract void process(PlayerTybaltsTaskDto currentTask, Player player, TybaltsTaskUpdate taskUpdate, ResponseMaps responseMaps);
	public abstract void initNewTask(PlayerTybaltsTaskDto currentTask, Player player, ResponseMaps responseMaps);
	
	protected void taskUpdateMessage(String message, Player player, ResponseMaps responseMaps) {
		responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(message, newTaskColour));
	}
	
	protected void message(String message, Player player, ResponseMaps responseMaps) {
		responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(message, taskMessageColour));
	}
	
	protected void error(String message, Player player, ResponseMaps responseMaps) {
		responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(message, taskErrorColour));
	}
}
