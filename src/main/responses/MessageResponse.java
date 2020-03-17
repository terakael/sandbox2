package main.responses;

import java.util.ArrayList;
import java.util.Collections;

import lombok.Setter;
import main.database.ItemDao;
import main.database.PlayerDao;
import main.database.PlayerStorageDao;
import main.database.StatsDao;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.WorldProcessor;
import main.requests.MessageRequest;
import main.requests.Request;
import main.requests.RequestFactory;
import main.types.ItemAttributes;
import main.types.Stats;
import main.types.StorageTypes;

public class MessageResponse extends Response {
	private String name;
	private int id;
	@Setter private String message;

	public MessageResponse() {
		setAction("message");
		setColour("yellow");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof MessageRequest)) {
			setRecoAndResponseText(0, "funny business");
			return;
		}
		
		MessageRequest messageReq = (MessageRequest)req;
		
		String msg = messageReq.getMessage();
		id = messageReq.getId();
		
		if (msg.length() >= 2 && msg.substring(0, 2).equals("::")) {
			//handleDebugCommand(id, msg, client);
			handleDebugCommand(player, msg.substring(2), responseMaps);
			return;
		}
		
		if (msg.length() > 100)
			msg = msg.substring(0, 100);
		
		name = PlayerDao.getNameFromId(id);
		message = msg;
		
		responseMaps.addLocalResponse(player.getRoomId(), player.getTileId(), this);
	}
	
	private void handleDebugCommand(Player player, String msg, ResponseMaps responseMaps) {
		String[] msgParts = msg.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");// the :: prefix should already be removed here
		if (msgParts[0].equals("tele") || msgParts[0].equals("home")) {
			handleDebugTele(player, msgParts, responseMaps);
			return;
		}
		
		// below are the god-only commands
		if (!player.isGod()) {
			setRecoAndResponseText(0, "You can't do that.  You're not god.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (msgParts[0].matches("^att|str|def|hp|agil|acc|mage|herb|mine|smith|fish|cook$")) {
			
			if (msgParts.length >= 2) {
				handleAddExp(player, msgParts, responseMaps);
			} else {
				setRecoAndResponseText(0, "invalid syntax.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
		}
		
		if (msgParts[0].equals("give")) {
			handleGive(player, msgParts, responseMaps);
			return;
		}
		
		if (msgParts[0].equals("heal")) {
			handleHeal(player, msgParts, responseMaps);
			return;
		}
	}
	
	private void handleDebugTele(Player player, String[] msgParts, ResponseMaps responseMaps) {		
		// syntax: ::tele (destPlayerName|tileId)[ srcPlayerName]
		if (msgParts.length == 1) {
			if (msgParts[0].equals("home")) {
				PlayerUpdateResponse playerUpdate = (PlayerUpdateResponse)ResponseFactory.create("player_update");
				playerUpdate.setId(player.getId());
				playerUpdate.setTileId(36859);
				player.setTileId(36859);
				responseMaps.addBroadcastResponse(playerUpdate);
				return;
			}
			// invalid syntax
			setRecoAndResponseText(0, "syntax: ::tele (destPlayerName|tileId)[ srcPlayerName]");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		int destTileId = -1;
		int destPlayerId = PlayerDao.getIdFromName(msgParts[1].replaceAll("\"", ""));
		if (destPlayerId == -1) {
			// no player exists, maybe it's a tileId
			try {
				destTileId = Integer.parseInt(msgParts[1]);
			} catch (NumberFormatException e) {}
		} else {
			// it's a valid player - but are they logged in?
			Player destPlayer = WorldProcessor.getPlayerById(destPlayerId);
			if (destPlayer != null)
				destTileId = destPlayer.getTileId();
		}
		
		if (destTileId < 0 || destTileId > PathFinder.LENGTH * PathFinder.LENGTH) {
			setRecoAndResponseText(0, "invalid teleport destination.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		Player targetPlayer = null;
		if (msgParts.length == 3 && player.isGod()) {
			// there's a srcPlayerName
			int targetPlayerId = PlayerDao.getIdFromName(msgParts[2]);
			if (targetPlayerId == -1) {
				setRecoAndResponseText(0, "could not find player: " + msgParts[2]);
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			targetPlayer = WorldProcessor.getPlayerById(targetPlayerId);			
			if (targetPlayer == null) {
				setRecoAndResponseText(0, msgParts[2] + " is not currently logged in.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
		} else {
			targetPlayer = player;
		}
		
		PlayerUpdateResponse playerUpdate = (PlayerUpdateResponse)ResponseFactory.create("player_update");
		playerUpdate.setId(targetPlayer.getDto().getId());
		playerUpdate.setTileId(destTileId);
		targetPlayer.setTileId(destTileId);
		responseMaps.addBroadcastResponse(playerUpdate);
	}
	
	private void handleAddExp(Player player, String[] msgParts, ResponseMaps responseMaps) {
		// msgParts[0] = att/str/def etc
		// msgParts[1] = 234 etc
		// msgParts[2] = playerName (optional)
		if (!msgParts[1].matches("-?\\d+")) {
			setRecoAndResponseText(0, "invalid syntax.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		int targetPlayerId = -1;
		if (msgParts.length == 3) {
			targetPlayerId = PlayerDao.getIdFromName(msgParts[2]);
			if (targetPlayerId == -1) {
				setRecoAndResponseText(0, "could not find player: " + msgParts[2]);
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
		}
		
		if (targetPlayerId == -1)
			targetPlayerId = player.getDto().getId();
		
		int statId = StatsDao.getStatIdByName(msgParts[0]);
		int exp = Integer.parseInt(msgParts[1]);
		if (Stats.withValue(statId) != null)
			StatsDao.addExpToPlayer(targetPlayerId, Stats.withValue(statId), exp);
		
		Player targetPlayer = targetPlayerId == player.getDto().getId() ? player : WorldProcessor.getPlayerById(targetPlayerId); 
		
		// inform god that the target player has received the exp
		if (targetPlayer != player) {
			setRecoAndResponseText(1, String.format("%s has been granted %dexp in %s, my lord.", msgParts[2], exp, msgParts[0]));
			responseMaps.addClientOnlyResponse(player, this);
		}
		
		if (targetPlayer == null)
			return;// targetPlayer is valid based on the id check above, but they're not logged in so we can't send them their response
		
		AddExpResponse resp = new AddExpResponse();
		resp.addExp(statId, exp);
		responseMaps.addClientOnlyResponse(targetPlayer, resp);
		
		MessageResponse messageResponse = new MessageResponse();
		messageResponse.setRecoAndResponseText(1, String.format("Your god has granted you %dexp in %s; %s him!", exp, msgParts[0], exp <= 0 ? "fear" : "praise"));
		messageResponse.setColour("red");
		responseMaps.addClientOnlyResponse(targetPlayer, messageResponse);
		
		PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
		playerUpdate.setId(targetPlayer.getId());
		playerUpdate.setCombatLevel(StatsDao.getCombatLevelByPlayerId(targetPlayer.getId()));
		responseMaps.addBroadcastResponse(playerUpdate);// should be local
		
		targetPlayer.refreshStats();
	}
	
	private void handleGive(Player player, String[] msgParts, ResponseMaps responseMaps) {
		// ::give [itemId|itemName] ([count])
		if (msgParts.length < 2)
			return;
		
		Integer itemId;
		try {
			itemId = Integer.parseInt(msgParts[1]);
		} catch (NumberFormatException e) {
			// not a number, maybe its the name?
			itemId = ItemDao.getIdFromName(msgParts[1].replaceAll("\"", ""));
			if (itemId == null) {
				setRecoAndResponseText(0, "invalid item.");
				responseMaps.addClientOnlyResponse(player, this);
				return;	
			}
		}
		
		if (ItemDao.getNameFromId(itemId) == null) {
			setRecoAndResponseText(0, "invalid itemId.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		int count = 1;
		if (msgParts.length > 2) {
			try {
				count = Integer.parseInt(msgParts[2]);
			} catch (NumberFormatException e) {}
		}
		
		ArrayList<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY.getValue());
		
		if (ItemDao.itemHasAttribute(itemId, ItemAttributes.STACKABLE)) {
			int invItemIndex = invItemIds.indexOf(itemId);
			if (invItemIndex >= 0) {
				PlayerStorageDao.addCountToStorageItemSlot(player.getId(), StorageTypes.INVENTORY.getValue(), invItemIndex, count);
			} else {
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY.getValue(), itemId, count, 0);
			}
		} else {
			int numFreeSlots = Collections.frequency(invItemIds, 0);
			if (count > numFreeSlots)
				count = numFreeSlots;
			
			for (int i = 0; i < invItemIds.size() && count > 0; ++i) {
				if (invItemIds.get(i) == 0) {
					PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY.getValue(), i, itemId, 1, ItemDao.getMaxCharges(itemId));
					--count;
				}
			}
		}
		
		
		new InventoryUpdateResponse().process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
	}
	
	private void handleHeal(Player player, String[] msgParts, ResponseMaps responseMaps) {
		// ::heal
		
		Player targetPlayer = player;
		if (msgParts.length == 2) {
			targetPlayer = WorldProcessor.getPlayerById(PlayerDao.getIdFromName(msgParts[1].replaceAll("\"", "")));
			if (targetPlayer == null) {
				setRecoAndResponseText(0, "invalid player.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
		}
		
		StatsDao.setRelativeBoostByPlayerIdStatId(targetPlayer.getId(), Stats.HITPOINTS, 0);
		int maxHp = StatsDao.getMaxHpByPlayerId(targetPlayer.getId());
		targetPlayer.setCurrentHp(maxHp);
		
		PlayerUpdateResponse updateResponse = new PlayerUpdateResponse();
		updateResponse.setId(targetPlayer.getId());
		updateResponse.setCurrentHp(maxHp);
		responseMaps.addLocalResponse(targetPlayer.getRoomId(), targetPlayer.getTileId(), updateResponse);
		
		if (targetPlayer != player) {
			setRecoAndResponseText(1, "your god has given you life.");
			setColour("red");
			responseMaps.addClientOnlyResponse(targetPlayer, this);
			
			MessageResponse godResponse = new MessageResponse();
			godResponse.setRecoAndResponseText(1, String.format("you have given %s new life, my lord.", targetPlayer.getDto().getName()));
			responseMaps.addClientOnlyResponse(player, godResponse);
		}
	}
}
