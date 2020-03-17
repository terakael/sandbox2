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
	
	@Getter private int id;
	@Getter private int tileId;
	@Getter private int roomId;
	private int attackStyleId;
	private Map<Integer, Integer> stats;
	private Map<Integer, Integer> boosts;
	private List<PlayerDto> players;
	private Map<Integer, InventoryItemDto> inventory;
	private Map<PlayerPartType, AnimationDto> baseAnimations;
	private Map<PlayerPartType, AnimationDto> equipAnimations;
	private Map<Integer, String> attackStyles;

	public LogonResponse() {
		setAction("logon");
	}
	
	public void processLogon(Request req, Session client, ResponseMaps responseMaps) {
		if (!(req instanceof LogonRequest)) {
			setRecoAndResponseText(0, "funny business");
			return;
		}
		
		LogonRequest logonReq = (LogonRequest)req;
		
		PlayerDto dto = PlayerDao.getPlayerByUsernameAndPassword(logonReq.getName(), logonReq.getPassword());
		Player player = new Player(dto, client);
		if (dto == null) {
			setRecoAndResponseText(0, "invalid credentials");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		} else {
			if (PlayerSessionDao.entryExists(dto.getId())) {
				setRecoAndResponseText(0, "already logged in");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			PlayerDao.updateLastLoggedIn(dto.getId());
			PlayerSessionDao.addPlayer(dto.getId());
		}
		
		id = dto.getId();
		PlayerStorageDao.createBankSlotsIfNotExists(id);
		
		tileId = dto.getTileId();
		roomId = dto.getRoomId();
		attackStyleId = dto.getAttackStyleId();

		stats = StatsDao.getAllStatExpByPlayerId(dto.getId());
		boosts = StatsDao.getRelativeBoostsByPlayerId(dto.getId())
				.entrySet()
				.stream()
				.collect(Collectors.toMap(e -> e.getKey().getValue(), Map.Entry::getValue));
		
		// if there was a bad disconnection (server crash etc) and the player was mid-trade, put the items back into the player's inventory.
		HashMap<Integer, InventoryItemDto> itemsInTrade = PlayerStorageDao.getStorageDtoMapByPlayerId(id, StorageTypes.TRADE.getValue());
		for (InventoryItemDto itemInTrade : itemsInTrade.values()) {
			PlayerStorageDao.addItemToFirstFreeSlot(id, StorageTypes.INVENTORY.getValue(), itemInTrade.getItemId(), itemInTrade.getCount(), itemInTrade.getCharges());
		}
		PlayerStorageDao.clearStorageByPlayerIdStorageTypeId(id, StorageTypes.TRADE.getValue());
		
		players = PlayerDao.getAllPlayers();
		inventory = PlayerStorageDao.getStorageDtoMapByPlayerId(dto.getId(), StorageTypes.INVENTORY.getValue());
		baseAnimations = AnimationDao.loadAnimationsByPlayerId(dto.getId());
		equipAnimations = AnimationDao.getEquipmentAnimationsByPlayerId(player.getId());
		attackStyles = PlayerDao.getAttackStyles();
		
		WorldProcessor.playerSessions.put(client, player);
		responseMaps.addClientOnlyResponse(player, this);
		
		new LoadRoomResponse().process(null, player, responseMaps);
		
		initializeNpcLocations(player, responseMaps);
		
		PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
		playerUpdate.setId(player.getId());
		playerUpdate.setName(player.getDto().getName());
		playerUpdate.setTileId(tileId);
		playerUpdate.setRoomId(roomId);
		playerUpdate.setCurrentHp(StatsDao.getCurrentHpByPlayerId(player.getId()));
		playerUpdate.setMaxHp(player.getStats().get(Stats.HITPOINTS));
		playerUpdate.setCombatLevel(StatsDao.getCombatLevelByPlayerId(player.getId()));
		playerUpdate.setEquipAnimations(equipAnimations);
		playerUpdate.setBaseAnimations(baseAnimations);
//		responseMaps.addLocalResponse(player.getRoomId(), player.getTileId(), playerUpdate);
		responseMaps.addBroadcastResponse(playerUpdate, player);
		
		new EquipResponse().process(null, player, responseMaps);
		
		// broadcast to the rest of the players that this player has logged in
//		PlayerEnterResponse playerEnter = (PlayerEnterResponse)ResponseFactory.create("playerEnter");
//		playerEnter.setId(id);
//		playerEnter.setName(player.getDto().getName());
//		playerEnter.setTileId(tileId);
//		playerEnter.setRoomId(roomId);
//		playerEnter.setCombatLevel(StatsDao.getCombatLevelByPlayerId(player.getId()));
//		playerEnter.setMaxHp(player.getStats().get(Stats.HITPOINTS));
//		playerEnter.setCurrentHp(StatsDao.getCurrentHpByPlayerId(player.getId()));
//		playerEnter.setBaseAnimations(baseAnimations);
//		playerEnter.setEquipAnimations(equipAnimations);
//		
//		responseMaps.addBroadcastResponse(playerEnter, player);
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
	
	public void initializeNpcLocations(Player player, ResponseMaps responseMaps) {
		// initial npc location refresh response (all living npcs)
		NpcLocationRefreshResponse npcLocationRefreshResponse = new NpcLocationRefreshResponse();
		List<NPC> localNpcs = NPCManager.get().getNpcsNearTile(player.getRoomId(), player.getTileId(), 15);
		localNpcs = localNpcs.stream().filter(e -> !e.isDead()).collect(Collectors.toList());
		Set<Integer> localNpcInstanceIds = localNpcs.stream().map(NPC::getInstanceId).collect(Collectors.toSet());
		player.updateInRangeNpcs(localNpcInstanceIds);
		
		ArrayList<NpcLocationRefreshResponse.NpcLocation> npcLocations = new ArrayList<>();
		for (NPC npc : localNpcs)
			npcLocations.add(new NpcLocationRefreshResponse.NpcLocation(npc.getId(), npc.getInstanceId(), npc.getTileId()));
		npcLocationRefreshResponse.setNpcs(npcLocations);
		responseMaps.addClientOnlyResponse(player, npcLocationRefreshResponse);
	}
}
