package main.responses;

import java.util.HashMap;
import java.util.Map;

import main.database.ItemDao;
import main.database.NPCDao;
import main.database.PlayerStorageDao;
import main.database.SceneryDao;
import main.processing.Player;
import main.requests.ExamineRequest;
import main.requests.Request;
import main.types.ItemAttributes;
import main.types.StorageTypes;

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
			break;
		}
		case "item": {
			// TODO because we don't know the source of the examine request, there's a small bug here.
			// basically if you have a stackable (of any size) on the ground, and also 100k+ stackable in your inventory,
			// if you examine the ground stackable it will tell you your inventory stackable count.
			// ideally we could tell the source of the request (inventory, ground, shop, bank, smithing interface etc) and handle it accordingly.
			if (ItemDao.itemHasAttribute(request.getObjectId(), ItemAttributes.STACKABLE)) {
				int stackCount = PlayerStorageDao.getNumStorageItemsByPlayerIdItemIdStorageTypeId(player.getId(), request.getObjectId(), StorageTypes.INVENTORY.getValue()); 
				if (stackCount >= 100000) {// 0 if the item isn't in the inventory
					examineText = String.format("%,d %s.", stackCount , ItemDao.getNameFromId(request.getObjectId()));
					break;
				}
			}
			examineText = itemExamineMap.get(request.getObjectId());
			break;
		}
		case "npc": {
			int npcId = NPCDao.getNpcIdFromInstanceId(player.getRoomId(), request.getObjectId());
			examineText = npcExamineMap.get(npcId);
			break;
		}
		default:
			break;
		}
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
