package responses;

import java.util.Collections;
import java.util.List;

import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.SawmillableDao;
import database.dao.StatsDao;
import database.dto.SawmillableDto;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import requests.Request;
import requests.UseRequest;
import types.Items;
import types.Stats;
import types.StorageTypes;

public class SawmillResponse extends Response {
	private final boolean usingKnife;
	private final int REQUIRED_LOGS;
	private final String startActionMessage;
	private final PlayerState actionState;
	
	public SawmillResponse(boolean usingKnife) {
		setAction("sawmill");
		this.usingKnife = usingKnife;
		REQUIRED_LOGS = usingKnife ? 4 : 3;
		startActionMessage = usingKnife ? "you begin carving the logs..." : "you run the logs through the sawmill...";
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
		if (sawmillable == null) {
			setRecoAndResponseText(0, "nothing interesting happens.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		// level check
		if (StatsDao.getStatLevelByStatIdPlayerId(Stats.CONSTRUCTION, player.getId()) < sawmillable.getRequiredLevel()) {
			setRecoAndResponseText(0, String.format("you need %d construction to make these planks.", sawmillable.getRequiredLevel()));
			responseMaps.addClientOnlyResponse(player, this);
			player.setState(PlayerState.idle);
			return;
		}
		
		// need three or four of a log, depending on if we're using the plankcrafting knife or sawmill
		List<Integer> inventoryItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		final int numRequiredLogs = Collections.frequency(inventoryItemIds, sawmillable.getLogId());
		if (numRequiredLogs < REQUIRED_LOGS) {
			final String message = player.getState() != actionState
						? String.format("you need %d logs to make a plank.", REQUIRED_LOGS)
						: "you have run out of logs.";
			setRecoAndResponseText(0, message);
			responseMaps.addClientOnlyResponse(player, this);
			player.setState(PlayerState.idle);
			return;
		}
		
		if (player.getState() != actionState) {
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(startActionMessage, null));
			player.setState(actionState);
			player.setSavedRequest(req);
		}
		player.setTickCounter(5);
		
		responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
				new ActionBubbleResponse(player, ItemDao.getItem(usingKnife ? Items.PLANKCRAFTING_KNIFE.getValue() : sawmillable.getLogId())));
	}

}
