package responses;

import java.util.Collections;
import java.util.List;

import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.SawmillableDao;
import database.dto.InventoryItemDto;
import database.dto.SawmillableDto;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.ArtisanManager;
import processing.managers.TybaltsTaskManager;
import processing.tybaltstasks.updates.SawmillTaskUpdate;
import requests.AddExpRequest;
import requests.Request;
import requests.RequestFactory;
import requests.UseRequest;
import types.Items;
import types.Stats;
import types.StorageTypes;

public class FinishSawmillResponse extends Response {
	private final boolean usingKnife;
	private final int REQUIRED_LOGS;
	private final PlayerState actionState;
	
	public FinishSawmillResponse(boolean usingKnife) {
		setAction("finish_sawmill");
		this.usingKnife = usingKnife;
		REQUIRED_LOGS = usingKnife ? 4 : 3;
		actionState = usingKnife ? PlayerState.sawmill_knife : PlayerState.sawmill;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof UseRequest))
			return;
		
		int logId = ((UseRequest)req).getSrc();
		if (usingKnife && logId == Items.PLANKCRAFTING_KNIFE.getValue())
			logId = ((UseRequest)req).getDest();
		
		final SawmillableDto sawmillable = SawmillableDao.getSawmillableByLogId(logId);
		if (sawmillable == null)
			return;
		
		List<Integer> inventoryItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		final int numRequiredLogs = Collections.frequency(inventoryItemIds, sawmillable.getLogId());
		if (numRequiredLogs < REQUIRED_LOGS) {
			final String message = player.getState() != actionState
						? String.format("you need %d logs to make a plank.", REQUIRED_LOGS)
						: "you have run out of logs.";
			setRecoAndResponseText(0, message);
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		AddExpRequest addExpReq = new AddExpRequest();
		addExpReq.setStatId(Stats.CONSTRUCTION.getValue());
		addExpReq.setExp(sawmillable.getExp());
		new AddExpResponse().process(addExpReq, player, responseMaps);
		
		if (usingKnife) {
			final int knifeSlot = ((UseRequest)req).getSrc() == Items.PLANKCRAFTING_KNIFE.getValue() ? ((UseRequest)req).getSrcSlot() : ((UseRequest)req).getSlot(); 
			
			InventoryItemDto knife = PlayerStorageDao.getStorageItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, knifeSlot);
			if (knife.getCharges() > 1) {
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, knifeSlot, Items.PLANKCRAFTING_KNIFE.getValue(), 1, knife.getCharges() - 1);
			} else {
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, knifeSlot, Items.KNIFE.getValue(), 1, 0);
			}
		}
		
		for (int i = 0; i < REQUIRED_LOGS; ++i) {
			int logIndex = inventoryItemIds.indexOf(sawmillable.getLogId());
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, logIndex, 0, 0, 0);
			inventoryItemIds.set(logIndex, 0);
		}
		
		PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, sawmillable.getResultingPlankId(), 1, ItemDao.getMaxCharges(sawmillable.getResultingPlankId()));
		
		new InventoryUpdateResponse().process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
		
		TybaltsTaskManager.check(player, new SawmillTaskUpdate(sawmillable.getResultingPlankId()), responseMaps);
		ArtisanManager.check(player, sawmillable.getResultingPlankId(), responseMaps);
	}

}
