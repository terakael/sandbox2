package responses;

import java.util.HashMap;
import java.util.Map;

import database.dao.ConstructableDao;
import database.dao.ItemDao;
import database.dao.NPCDao;
import database.dao.PlayerStorageDao;
import database.dao.SceneryDao;
import database.dto.ConstructableDto;
import processing.attackable.NPC;
import processing.attackable.Player;
import processing.managers.ConstructableManager;
import processing.managers.NPCManager;
import requests.ExamineRequest;
import requests.Request;
import types.ItemAttributes;
import types.StorageTypes;

@SuppressWarnings("unused")
public class ExamineResponse extends Response {
	private String examineText;
	
	private static Map<Integer, String> sceneryExamineMap = new HashMap<>();
	private static Map<Integer, String> itemExamineMap = new HashMap<>();
	private static Map<Integer, String> npcExamineMap = new HashMap<>();
	public static void initializeExamineMap() {
		sceneryExamineMap = SceneryDao.getExamineMap();
		itemExamineMap = ItemDao.getExamineMap();
		npcExamineMap = NPCDao.getExamineMap();
	}

	public ExamineResponse() {
		setAction("examine");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ExamineRequest))
			return;
		
		ExamineRequest request = (ExamineRequest)req;
		if (request.getType() == null)
			return;
		
		switch (request.getType()) {
		case "scenery": { 
			examineText = sceneryExamineMap.get(request.getObjectId());
			
			// constructables show their timer as well
			final int remainingTicks = ConstructableManager.getRemainingTicks(player.getFloor(), request.getTileId());
			if (remainingTicks > 0) {
				if (remainingTicks > 100) {
					final int remainingMinutes = remainingTicks / 100;
					examineText += String.format(" (%d minute%s remain%s)", remainingMinutes, remainingMinutes == 1 ? "" : "s", remainingMinutes == 1 ? "s" : "");
				} else {
					final int remainingSeconds = (int)(remainingTicks * 0.6);
					examineText += String.format(" (%d second%s remain%s)", remainingSeconds, remainingSeconds == 1 ? "" : "s", remainingSeconds == 1 ? "s" : "");
				}
			}
			break;
		}
		case "item": {
			if (ItemDao.itemHasAttribute(request.getObjectId(), ItemAttributes.STACKABLE)) {
				int stackCount = PlayerStorageDao.getStorageItemCountByPlayerIdItemIdStorageTypeId(player.getId(), request.getObjectId(), StorageTypes.INVENTORY); 
				if (stackCount >= 100000) {
					examineText = String.format("%,d %s.", stackCount , ItemDao.getNameFromId(request.getObjectId(), stackCount != 1));
					break;
				}
			}
			examineText = itemExamineMap.get(request.getObjectId());
			break;
		}
		case "grounditem": {
			// you can't count a stackable item if it's on the ground, that's just silly.
			// you need to be holding it to count it.
			examineText = itemExamineMap.get(request.getObjectId());
			break;
		}
		case "npc": {
			NPC npc = NPCManager.get().getNpcByInstanceId(player.getFloor(), request.getObjectId());
			if (npc != null) {
//				int npcId = NPCDao.getNpcIdFromInstanceId(player.getFloor(), request.getObjectId());
				examineText = npcExamineMap.get(npc.getId());
			}
			break;
		}
		default:
			break;
		}
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
