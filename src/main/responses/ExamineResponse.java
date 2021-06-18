package main.responses;

import java.util.HashMap;
import java.util.Map;

import main.GroundItemManager;
import main.database.dao.ItemDao;
import main.database.dao.NPCDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.SceneryDao;
import main.processing.Player;
import main.processing.RoomGroundItemManager.GroundItem;
import main.requests.ExamineRequest;
import main.requests.Request;
import main.types.ItemAttributes;
import main.types.StorageTypes;

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
			break;
		}
		case "item": {
			if (ItemDao.itemHasAttribute(request.getObjectId(), ItemAttributes.STACKABLE)) {
				int stackCount = PlayerStorageDao.getStorageItemCountByPlayerIdItemIdStorageTypeId(player.getId(), request.getObjectId(), StorageTypes.INVENTORY); 
				if (stackCount >= 100000) {
					examineText = String.format("%,d %s.", stackCount , ItemDao.getNameFromId(request.getObjectId()));
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
			int npcId = NPCDao.getNpcIdFromInstanceId(player.getFloor(), request.getObjectId());
			examineText = npcExamineMap.get(npcId);
			break;
		}
		default:
			break;
		}
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
