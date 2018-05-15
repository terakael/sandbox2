package main.responses;

import java.util.List;
import java.util.Map;

import javax.websocket.Session;

import lombok.Getter;
import main.Stats;
import main.database.DbConnection;
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
import main.requests.LogonRequest;
import main.requests.Request;

public class LogonResponse extends Response {
	
	@Getter private String id;
	@Getter private String name;
	@Getter private int x;
	@Getter private int y;
	@Getter private int currentHp;
	@Getter private int maxHp;
	private Stats stats;
	private List<PlayerDto> players;
	private List<SpriteMapDto> spriteMaps;
	private List<ItemDto> items;
	private List<SpriteFrameDto> spriteFrames;
	private List<Integer> inventory;

	public LogonResponse(String action) {
		super(action);
	}
	
	@Override
	public ResponseType process(Request req, Session client) {
		if (!(req instanceof LogonRequest)) {
			setRecoAndResponseText(0, "funny business");
			return ResponseType.client_only;
		}
		
		LogonRequest logonReq = (LogonRequest)req;
		
		PlayerDto dto = PlayerDao.getPlayerByUsernameAndPassword(logonReq.getName(), logonReq.getPassword());
		if (dto == null) {
			setRecoAndResponseText(0, "invalid credentials");
			return ResponseType.client_only;
		} else {
			if (PlayerSessionDao.entryExists(dto.getId())) {
				setRecoAndResponseText(0, "already logged in");
				return ResponseType.client_only;
			}
			PlayerDao.updateLastLoggedIn(dto.getId());
		}
		
		id = Integer.toString(dto.getId());
		name = dto.getName();
		
		x = dto.getX();
		y = dto.getY();
		currentHp = dto.getCurrentHp();
		maxHp = dto.getMaxHp();
		
		Map<String, Integer> statList = StatsDao.getStatsByPlayerId(dto.getId());
		stats = new Stats(statList);
		
		players = PlayerDao.getAllPlayers();
		spriteMaps = SpriteMapDao.getAllSpriteMaps();
		spriteFrames = SpriteFrameDao.getAllSpriteFrames();
		items = ItemDao.getAllItems();
		inventory = PlayerInventoryDao.getInventoryListByPlayerId(dto.getId());

		setRecoAndResponseText(1, "");
		return ResponseType.client_only;
	}
}
