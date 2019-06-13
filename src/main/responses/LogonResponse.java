package main.responses;

import java.util.List;
import java.util.Map;

import javax.websocket.Session;

import lombok.Getter;
import main.GroundItemManager;
import main.database.AnimationDao;
import main.database.AnimationDto;
import main.database.EquipmentDao;
import main.database.PlayerDao;
import main.database.PlayerDto;
import main.database.PlayerSessionDao;
import main.database.PlayerStorageDao;
import main.database.StatsDao;
import main.processing.Player;
import main.processing.WorldProcessor;
import main.requests.LogonRequest;
import main.requests.Request;
import main.types.Stats;

public class LogonResponse extends Response {
	
	@Getter private int id;
	@Getter private int tileId;
	private int attackStyleId;
	private Map<Integer, Integer> stats;
	private List<PlayerDto> players;
	private List<Integer> inventory;
	private AnimationDto animations;
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
		tileId = dto.getTileId();
		attackStyleId = dto.getAttackStyleId();

		stats = StatsDao.getAllStatExpByPlayerId(dto.getId());
		
		players = PlayerDao.getAllPlayers();
		inventory = PlayerStorageDao.getInventoryListByPlayerId(dto.getId());
		animations = AnimationDao.loadAnimationsByPlayerId(dto.getId());
		attackStyles = PlayerDao.getAttackStyles();
		
		WorldProcessor.playerSessions.put(client, player);
		
		responseMaps.addClientOnlyResponse(player, this);
		
		new EquipResponse().process(null, player, responseMaps);
		
		// broadcast to the rest of the players that this player has logged in
		PlayerEnterResponse playerEnter = (PlayerEnterResponse)ResponseFactory.create("playerEnter");
		playerEnter.setId(id);
		playerEnter.setName(player.getDto().getName());
		playerEnter.setTileId(tileId);
		playerEnter.setCombatLevel(StatsDao.getCombatLevelByPlayerId(player.getId()));
		playerEnter.setMaxHp(player.getStats().get(Stats.HITPOINTS));
		playerEnter.setCurrentHp(StatsDao.getCurrentHpByPlayerId(player.getId()));
		playerEnter.setAnimations(AnimationDao.loadAnimationsByPlayerId(dto.getId()));
		
		responseMaps.addBroadcastResponse(playerEnter, player);
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		
	}
}
