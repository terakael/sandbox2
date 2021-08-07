package processing.tybaltstasks;

import database.dto.PlayerTybaltsTaskDto;
import processing.attackable.Player;
import processing.tybaltstasks.updates.TybaltsTaskUpdate;
import responses.MessageResponse;
import responses.ResponseMaps;

public abstract class TybaltsTask {
	final static String newTaskColour = "#23f5b4";
	final static String taskMessageColour = "#bada55"; // badass bruh
	final static String taskErrorColour = "#c48000";
	final static String completionMessage = "task complete; return to tybalt to claim your reward.";
	
	public abstract void process(PlayerTybaltsTaskDto currentTask, Player player, TybaltsTaskUpdate taskUpdate, ResponseMaps responseMaps);
	public abstract void initNewTask(PlayerTybaltsTaskDto currentTask, Player player, ResponseMaps responseMaps);
	public abstract boolean isFinished(int playerId);
	
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
