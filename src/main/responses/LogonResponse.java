package main.responses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.websocket.Session;

import lombok.Getter;
import main.GroundItemManager;
import main.database.AnimationDao;
import main.database.AnimationDto;
import main.database.EquipmentBonusDto;
import main.database.EquipmentDao;
import main.database.GroundTextureDto;
import main.database.InventoryItemDto;
import main.database.PlayerDao;
import main.database.PlayerDto;
import main.database.PlayerSessionDao;
import main.database.PlayerStorageDao;
import main.database.StatsDao;
import main.processing.NPC;
import main.processing.NPCManager;
import main.processing.Player;
import main.processing.WorldProcessor;
import main.requests.LogonRequest;
import main.requests.Request;
import main.types.PlayerPartType;
import main.types.Stats;
import main.types.StorageTypes;

public class LogonResponse extends Response {
	private PlayerDto playerDto;
	
	private Map<Integer, Integer> stats;
	private Map<Integer, Integer> boosts;
	private Map<Integer, InventoryItemDto> inventory;
	private Map<Integer, String> attackStyles;
	private HashSet<Integer> equippedSlots;
	private EquipmentBonusDto bonuses;

	public LogonResponse() {
		setAction("logon");
	}
	
	public void processLogon(Request req, Session client, ResponseMaps responseMaps) {
		if (!(req instanceof LogonRequest)) {
			setRecoAndResponseText(0, "funny business");
			return;
		}
		
		LogonRequest logonReq = (LogonRequest)req;
		
		playerDto = PlayerDao.getPlayerByUsernameAndPassword(logonReq.getName(), logonReq.getPassword());
		
		Player player = new Player(playerDto, client);
		if (playerDto == null) {
			setRecoAndResponseText(0, "invalid credentials");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (PlayerSessionDao.entryExists(playerDto.getId())) {
			setRecoAndResponseText(0, "already logged in");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		PlayerDao.updateLastLoggedIn(playerDto.getId());
		PlayerSessionDao.addPlayer(playerDto.getId());
				
		PlayerStorageDao.createBankSlotsIfNotExists(playerDto.getId());
		
		stats = StatsDao.getAllStatExpByPlayerId(playerDto.getId());
		boosts = StatsDao.getRelativeBoostsByPlayerId(playerDto.getId())
				.entrySet()
				.stream()
				.collect(Collectors.toMap(e -> e.getKey().getValue(), Map.Entry::getValue));
		equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(playerDto.getId());
		inventory = PlayerStorageDao.getStorageDtoMapByPlayerId(playerDto.getId(), StorageTypes.INVENTORY.getValue());
		attackStyles = PlayerDao.getAttackStyles();
		bonuses = EquipmentDao.getEquipmentBonusesByPlayerId(playerDto.getId());
		player.refreshBonuses(bonuses);
		
		// if there was a bad disconnection (server crash etc) and the player was mid-trade, put the items back into the player's inventory.
		HashMap<Integer, InventoryItemDto> itemsInTrade = PlayerStorageDao.getStorageDtoMapByPlayerId(playerDto.getId(), StorageTypes.TRADE.getValue());
		for (InventoryItemDto itemInTrade : itemsInTrade.values()) {
			PlayerStorageDao.addItemToFirstFreeSlot(playerDto.getId(), StorageTypes.INVENTORY.getValue(), itemInTrade.getItemId(), itemInTrade.getCount(), itemInTrade.getCharges());
		}
		PlayerStorageDao.clearStorageByPlayerIdStorageTypeId(playerDto.getId(), StorageTypes.TRADE.getValue());
		
		WorldProcessor.playerSessions.put(client, player);
		responseMaps.addClientOnlyResponse(player, this);
		
		new LoadRoomResponse().process(null, player, responseMaps);
		new PlayerEnterResponse().process(null, player, responseMaps);
		
//		initializeNpcLocations(player, responseMaps);
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
	
//	public void initializeNpcLocations(Player player, ResponseMaps responseMaps) {
//		// initial npc location refresh response (all living npcs)
//		NpcLocationRefreshResponse npcLocationRefreshResponse = new NpcLocationRefreshResponse();
//		List<NPC> localNpcs = NPCManager.get().getNpcsNearTile(player.getRoomId(), player.getTileId(), 15);
//		localNpcs = localNpcs.stream().filter(e -> !e.isDead()).collect(Collectors.toList());
//		Set<Integer> localNpcInstanceIds = localNpcs.stream().map(NPC::getInstanceId).collect(Collectors.toSet());
//		player.updateInRangeNpcs(localNpcInstanceIds);
//		
//		ArrayList<NpcLocationRefreshResponse.NpcLocation> npcLocations = new ArrayList<>();
//		for (NPC npc : localNpcs)
//			npcLocations.add(new NpcLocationRefreshResponse.NpcLocation(npc.getId(), npc.getInstanceId(), npc.getTileId()));
//		npcLocationRefreshResponse.setNpcs(npcLocations);
//		responseMaps.addClientOnlyResponse(player, npcLocationRefreshResponse);
//	}
}
