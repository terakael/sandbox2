package main.responses;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.websocket.Session;

import lombok.Getter;
import main.Endpoint;
import main.GroundItemManager;
import main.Stats;
import main.database.AnimationDao;
import main.database.AnimationDto;
import main.database.DbConnection;
import main.database.EquipmentDao;
import main.database.ItemDao;
import main.database.ItemDto;
import main.database.PlayerDao;
import main.database.PlayerDto;
import main.database.PlayerInventoryDao;
import main.database.PlayerSessionDao;
import main.database.SpriteFrameDao;
import main.database.SpriteFrameDto;
import main.database.SpriteMapDao;
import main.database.SpriteMapDto;
import main.database.StatsDao;
import main.processing.WorldProcessor;
import main.requests.LogonRequest;
import main.requests.Request;
import main.state.Player;

public class LogonResponse extends Response {
	
	@Getter private String id;
	@Getter private String name;
	@Getter private int tileId;
	@Getter private int currentHp;
	@Getter private int maxHp;
	private Stats stats;
	private List<PlayerDto> players;
	private List<SpriteMapDto> spriteMaps;
	private List<ItemDto> items;
	private List<SpriteFrameDto> spriteFrames;
	private List<Integer> inventory;
	private List<Integer> equippedSlots;
	private List<GroundItemManager.GroundItem> groundItems;
	private AnimationDto animations;

	public LogonResponse(String action) {
		super(action);
	}
	
	@Override
	public ResponseType process(Request req, Session client, ResponseMaps responseMaps) {
		if (!(req instanceof LogonRequest)) {
			setRecoAndResponseText(0, "funny business");
			return ResponseType.client_only;
		}
		
		LogonRequest logonReq = (LogonRequest)req;
		
		PlayerDto dto = PlayerDao.getPlayerByUsernameAndPassword(logonReq.getName(), logonReq.getPassword());
		Player player = new Player(dto, client);
		if (dto == null) {
			setRecoAndResponseText(0, "invalid credentials");
			responseMaps.addClientOnlyResponse(player, this);
			return ResponseType.client_only;
		} else {
			if (PlayerSessionDao.entryExists(dto.getId())) {
				setRecoAndResponseText(0, "already logged in");
				responseMaps.addClientOnlyResponse(player, this);
				return ResponseType.client_only;
			}
			PlayerDao.updateLastLoggedIn(dto.getId());
			PlayerSessionDao.addPlayer(dto.getId());
		}
		
		id = Integer.toString(dto.getId());
		name = dto.getName();
		
		tileId = dto.getTileId();
		currentHp = dto.getCurrentHp();
		maxHp = dto.getMaxHp();
		
		Map<String, Integer> statList = StatsDao.getStatsByPlayerId(dto.getId());
		stats = new Stats(statList);
		
		players = PlayerDao.getAllPlayers();
		spriteMaps = SpriteMapDao.getAllSpriteMaps();
		spriteFrames = SpriteFrameDao.getAllSpriteFrames();
		items = ItemDao.getAllItems();
		inventory = PlayerInventoryDao.getInventoryListByPlayerId(dto.getId());
		equippedSlots = EquipmentDao.getEquippedSlotsByPlayerId(dto.getId());
		groundItems = GroundItemManager.getGroundItems();
		animations = AnimationDao.loadAnimationsByPlayerId(dto.getId());
		
		Endpoint.playerSessions.put(dto, client);// TODO remove
		WorldProcessor.playerSessions.put(client, player);
		
		responseMaps.addClientOnlyResponse(player, this);
		setRecoAndResponseText(1, "");
		return ResponseType.client_only;
	}
}
