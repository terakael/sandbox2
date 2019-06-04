package main.processing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.websocket.Session;


import lombok.Getter;
import lombok.Setter;
import main.GroundItemManager;
import main.database.EquipmentBonusDto;
import main.database.EquipmentDao;
import main.database.MineableDao;
import main.database.MineableDto;
import main.database.PlayerDao;
import main.database.PlayerDto;
import main.database.PlayerStorageDao;
import main.database.StatsDao;
import main.database.StatsDto;
import main.requests.AddExpRequest;
import main.requests.MineRequest;
import main.requests.Request;
import main.requests.RequestFactory;
import main.responses.AddExpResponse;
import main.responses.DeathResponse;
import main.responses.DropResponse;
import main.responses.FinishMiningResponse;
import main.responses.InventoryUpdateResponse;
import main.responses.MineResponse;
import main.responses.PlayerUpdateResponse;
import main.responses.Response;
import main.responses.ResponseFactory;
import main.responses.ResponseMaps;

public class Player extends Attackable {
	public enum PlayerState {
		idle,
		walking,
		chasing,// used for walking to a moving target (following, moving to attack something etc)
		following,
		mining,
		fighting
	};
	
	@Getter private PlayerDto dto;
	@Getter private Session session;
	@Setter private Stack<Integer> path = new Stack<>();// stack of tile_ids
	@Setter private PlayerState state = PlayerState.idle;
	@Setter private Request savedRequest = null;
	@Setter private int tickCounter = 0;
	
	public Player(PlayerDto dto, Session session) {
		this.dto = dto;
		tileId = dto.getTileId();
		this.session = session;
	}
	
	public void process(ResponseMaps responseMaps) {
		// called each tick; build a response where necessary
		switch (state) {
		case walking: {
			// if the player path stack isn't empty, then pop one off and create a player_updates response entry.
			if (!path.isEmpty()) {				
				setTileId(path.pop());

				PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
				playerUpdateResponse.setId(dto.getId());
				playerUpdateResponse.setTile(getTileId());
				responseMaps.addLocalResponse(this, playerUpdateResponse);
				
				if (path.isEmpty()) {
					if (savedRequest == null) // if not null then reprocess the saved request; this is a walkandaction.
						state = PlayerState.idle;
					else {
						Response response = ResponseFactory.create(savedRequest.getAction());
						response.process(savedRequest, this, responseMaps);
//						savedRequest = null;
					}
				}
			}
			
			break;
		}
		case following: {
			if (!path.isEmpty()) {
				setTileId(path.pop());
				PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
				playerUpdateResponse.setId(dto.getId());
				playerUpdateResponse.setTile(getTileId());
				responseMaps.addLocalResponse(this, playerUpdateResponse);
			}

			// maybe the target player logged out
			if (target == null) {
				state = PlayerState.idle;
				break;
			}
			
			if (!PathFinder.isNextTo(dto.getTileId(), target.getTileId())) {
				path = PathFinder.findPath(dto.getTileId(), target.getTileId(), false);
			}
			break;
		}
		case chasing: {
			if (target == null) {
				// player could have logged out/died as they were chasing
				state = PlayerState.idle;
				break;
			}
			
			if (!PathFinder.isNextTo(tileId, target.getTileId())) {
				path = PathFinder.findPath(tileId, target.getTileId(), true);
			} else {
				// start the fight
				if (savedRequest != null) {
					Response response = ResponseFactory.create(savedRequest.getAction());
					response.process(savedRequest, this, responseMaps);
					savedRequest = null;
				}
				state = PlayerState.fighting;
				path.clear();
			}
			
			// similar to walking, but need to recalculate path each tick due to moving target
			if (!path.isEmpty()) {
				setTileId(path.pop());
				PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
				playerUpdateResponse.setId(dto.getId());
				playerUpdateResponse.setTile(getTileId());
				responseMaps.addLocalResponse(this, playerUpdateResponse);
			}

			break;
		}
		case mining:
			// waiting until the tick counter hits zero, then do the actual mining and create a finish_mining response
			if (--tickCounter <= 0) {
				// TODO do checks:
				// is player close enough to target tile?
				// is target tile a rock?
				// does player have level to mine this rock?
				// does player have inventory space for the loot?
				
				// if yes to all above:
				// add rock loot to first empty inventory space
				// create storage_update response
				// create finish_mining response
				// set state to idle.
				if (!(savedRequest instanceof MineRequest)) {
					savedRequest = null;
					state = PlayerState.idle;
					return;
				}
				
				MineRequest mineRequest = (MineRequest)savedRequest;
				MineableDto mineable = MineableDao.getMineableDtoByTileId(mineRequest.getTileId());
				if (mineable == null) {
					MineResponse mineResponse = new MineResponse();
					mineResponse.setRecoAndResponseText(0, "you can't mine that.");
					responseMaps.addClientOnlyResponse(this, mineResponse);
					return;
				}
				PlayerStorageDao.addItemByItemIdPlayerId(getId(), mineable.getItemId());
				
				AddExpRequest addExpReq = new AddExpRequest();
				addExpReq.setId(getId());
				addExpReq.setStatId(6);// mining
				addExpReq.setExp(mineable.getExp());
				
				new AddExpResponse().process(addExpReq, this, responseMaps);
				new FinishMiningResponse().process(savedRequest, this, responseMaps);
				new InventoryUpdateResponse().process(RequestFactory.create("dummy", getId()), this, responseMaps);
				
				savedRequest = null;
				state = PlayerState.idle;
			}
			break;
		case fighting:
			if (--tickCounter <= 0) {
				// calculate the actual attack, create hitspat_update response, reset tickCounter.
			}
			break;
			
		case idle:// fall through
		default:
			break;
		}
	}
	
	public void setTileId(int tileId) {
		this.tileId = tileId;
		PlayerDao.updateTileId(dto.getId(), tileId);
	}
	
	public int getTileId() {
		return tileId;
	}
	
	public boolean isGod() {
		return dto.getId() == 3;// god id
	}
	
	public int getId() {
		return dto.getId();
	}
	
	@Override
	public void onDeath(ResponseMaps responseMaps) {
		// unequip and drop all the items in inventory
		EquipmentDao.clearAllEquppedItems(getId());
		
		List<Integer> inventoryList = PlayerStorageDao.getInventoryListByPlayerId(getId());
		for (int itemId : inventoryList) {
			if (itemId != 0)
				GroundItemManager.add(itemId, tileId);
		}
		PlayerStorageDao.clearInventoryByPlayerId(getId());
		
		// update the player inventory to show there's no more items
		new InventoryUpdateResponse().process(RequestFactory.create("dummy", getId()), this, responseMaps);
		
		// let everyone know about all the shit on the floor
		// TODO don't use dropResponse, use a new ground_update response
		DropResponse dropResponse = new DropResponse();
		dropResponse.setGroundItems(GroundItemManager.getGroundItems());
		responseMaps.addBroadcastResponse(dropResponse);
		
		// let everyone know you died lmao
		DeathResponse deathResponse = new DeathResponse();
		deathResponse.setId(getId());
		deathResponse.setCurrentHp(dto.getMaxHp());
		deathResponse.setTileId(31375);
		setTileId(31375);
		
		responseMaps.addBroadcastResponse(deathResponse);
		
		StatsDao.setRelativeBoostByPlayerIdStatId(getId(), 5, 0);
		
		state = PlayerState.idle;
		
//		if (fightOver) {
//		// send the death response to the dead player
//		FightingPlayer deadPlayer = player1turn ? player2 : player1;
//		
//		// clear all equipped items from dead player
//		EquipmentDao.clearAllEquppedItems(deadPlayer.getId());
//		
//		// drop all dead players items on ground
//		List<Integer> inventoryList = PlayerStorageDao.getInventoryListByPlayerId(deadPlayer.getId());
//		for (int itemId : inventoryList) {
//			if (itemId != 0)
//				GroundItemManager.add(itemId, deadPlayer.getRawPlayer().getTileId());
//		}
//		PlayerStorageDao.clearInventoryByPlayerId(deadPlayer.getId());
//		
//		Request req = new Request();
//		req.setId(deadPlayer.getId());
//		
//		// this pulls the equipped items and inventory list by playerId (set in the req above)
//		new InventoryUpdateResponse().process(req, deadPlayer.getRawPlayer(), responseMaps);
//		
//		// TODO don't use dropResponse, use a new ground_update response
//		DropResponse dropResponse = new DropResponse();
//		dropResponse.setGroundItems(GroundItemManager.getGroundItems());
//		responseMaps.addBroadcastResponse(dropResponse);
//		
//		// broadcast that the player died
//		DeathResponse deathResponse = new DeathResponse();
//		deathResponse.setId(deadPlayer.getId());
//		deathResponse.setCurrentHp(deadPlayer.getMaxHp());
//		deathResponse.setTileId(31375);
//		deadPlayer.getRawPlayer().setTileId(31375);
//		
//		p1.setState(PlayerState.idle);
//		p2.setState(PlayerState.idle);
//		
//		responseMaps.addBroadcastResponse(deathResponse);
//	}
	}
	
	@Override
	public void onKill(Attackable killed, ResponseMaps responseMaps) {
		state = PlayerState.idle;
	}
	
	@Override
	public void onHit(int damage, ResponseMaps responseMaps) {
		currentHp -= damage;
		if (currentHp < 0)
			currentHp = 0;
		
		// you have 10 hp max, 1hp remaining
		// relative boost should be -9
		// therefore: -max + current
		
		StatsDao.setRelativeBoostByPlayerIdStatId(getId(), 5, -this.dto.getMaxHp() + currentHp);
		PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
		playerUpdateResponse.setId(getId());
		playerUpdateResponse.setDamage(damage);
		playerUpdateResponse.setHp(currentHp);
		responseMaps.addBroadcastResponse(playerUpdateResponse);
		
	}
	
	@Override
	public void setTarget(Attackable target) {
		this.target = target;
		this.state = PlayerState.chasing;
	}
	
	@Override
	public void setStatsAndBonuses() {
		HashMap<String, Integer> stats = new HashMap<>();
		for (Map.Entry<String, Integer> entry : StatsDao.getStatsByPlayerId(getId()).entrySet())
			stats.put(entry.getKey(), StatsDao.getLevelFromExp(entry.getValue()));
		setStats(stats);
		
		EquipmentBonusDto equipment = EquipmentDao.getEquipmentBonusesByPlayerId(getId());
		HashMap<String, Integer> bonuses = new HashMap<>();
		bonuses.put("strength", equipment.getStr());
		bonuses.put("accuracy", equipment.getAcc());
		bonuses.put("defence", equipment.getDef());
		bonuses.put("agility", equipment.getAgil());
//		stats.put("hitpoints", dto.getHp());
		setBonuses(bonuses);
		setCurrentHp(dto.getCurrentHp());
		
		int weaponCooldown = 2;// TODO weapon speed based off equipped weapon
		setMaxCooldown(weaponCooldown);
	}
}
