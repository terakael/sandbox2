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
import main.database.ItemDao;
import main.database.MineableDao;
import main.database.MineableDto;
import main.database.NpcDialogueDto;
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
import main.types.Stats;

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
	@Setter @Getter private NpcDialogueDto currentDialogue = null;
	
	@Getter private HashMap<Stats, Integer> stats = new HashMap<>();// cached so we don't have to keep polling the db
	
	public Player(PlayerDto dto, Session session) {
		this.dto = dto;
		tileId = dto.getTileId();
		this.session = session;
		
		refreshStats();
		
		currentHp = StatsDao.getStatLevelByStatIdPlayerId(5, dto.getId()) + StatsDao.getRelativeBoostsByPlayerId(dto.getId()).get(5);
	}
	
	public void refreshStats(Map<Integer, Integer> statExp) {
		for (Map.Entry<Integer, Integer> stat : statExp.entrySet())
			stats.put(Stats.withValue(stat.getKey()), StatsDao.getLevelFromExp(stat.getValue()));
	}
	
	public void refreshStats() {
		refreshStats(StatsDao.getAllStatExpByPlayerId(dto.getId()));
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
				responseMaps.addLocalResponse(getTileId(), playerUpdateResponse);
				
				if (path.isEmpty()) {
					if (savedRequest == null) // if not null then reprocess the saved request; this is a walkandaction.
						state = PlayerState.idle;
					else {
						Response response = ResponseFactory.create(savedRequest.getAction());
						response.process(savedRequest, this, responseMaps);
					}
				}
			}
			
			break;
		}
		case following: {
			if (!path.isEmpty()) {
				setTileId(path.pop());
				PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
				playerUpdateResponse.setId(getId());
				playerUpdateResponse.setTile(getTileId());
				responseMaps.addLocalResponse(getTileId(), playerUpdateResponse);
			}

			// maybe the target player logged out
			if (target == null) {
				state = PlayerState.idle;
				break;
			}
			
			if (!PathFinder.isNextTo(tileId, target.getTileId())) {
				path = PathFinder.findPath(tileId, target.getTileId(), false);
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
				path = PathFinder.findPath(tileId, target.getTileId(), false);
			} else {
				// start the fight
				if (savedRequest != null) {
					Request req = savedRequest;
					savedRequest = null;
					
					Response response = ResponseFactory.create(req.getAction());
					response.process(req, this, responseMaps);
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
				responseMaps.addLocalResponse(getTileId(), playerUpdateResponse);
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
				PlayerStorageDao.addItemByPlayerIdItemId(getId(), mineable.getItemId());
				
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
	public void onDeath(Attackable killer, ResponseMaps responseMaps) {
		// unequip and drop all the items in inventory
		EquipmentDao.clearAllEquppedItems(getId());
		
		List<Integer> inventoryList = PlayerStorageDao.getInventoryListByPlayerId(getId());
		for (int itemId : inventoryList) {
			if (itemId != 0)
				GroundItemManager.add(getId(), itemId, tileId);
		}
		PlayerStorageDao.clearInventoryByPlayerId(getId());
		
		// update the player inventory to show there's no more items
		new InventoryUpdateResponse().process(RequestFactory.create("dummy", getId()), this, responseMaps);
		
		StatsDao.setRelativeBoostByPlayerIdStatId(getId(), 5, 0);
		currentHp = StatsDao.getStatLevelByStatIdPlayerId(5, dto.getId());
		
		// let everyone know you died lmao
		DeathResponse deathResponse = new DeathResponse();
		deathResponse.setId(getId());
		deathResponse.setCurrentHp(currentHp);
		deathResponse.setTileId(37611);
		setTileId(37611);
		responseMaps.addBroadcastResponse(deathResponse);
		
		state = PlayerState.idle;
	}
	
	@Override
	public void onKill(Attackable killed, ResponseMaps responseMaps) {
		state = PlayerState.idle;
		int totalExp = killed.getExp();
		float points = (float)totalExp / 5;
		
		// exp is doled out based on attackStyle and weapon type.
		// exp is split into five parts (called points) and the points are stored as follows:
		int weaponId = EquipmentDao.getWeaponIdByPlayerId(getId());
		String weaponName = ItemDao.getNameFromId(weaponId);
		if (weaponName == null) {
			// error: invalid weaponId (0 is no weapon and returns the string "null")
			return;
		}
		
		Map<Integer, Integer> expBefore = StatsDao.getAllStatExpByPlayerId(getId());
		
		// hammer/aggressive: 4str, 1hp
		// hammer/defensive: 4def, 1hp
		// hammer/shared: 2str, 2def, 1hp
		if (weaponName.contains(" hammer" )) {// TODO add weapon_type enum
			switch (getDto().getAttackStyleId()) {
				case 1:// aggressive
					StatsDao.addExpToPlayer(getId(), Stats.STRENGTH, points * 4);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
				break;
				case 2: // defensive
					StatsDao.addExpToPlayer(getId(), Stats.DEFENCE, points * 4);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
					break;
				default: // shared or other
					StatsDao.addExpToPlayer(getId(), Stats.STRENGTH, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.DEFENCE, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
					break;
			}
		}
		
		// daggers/aggressive: 4acc, 1hp
		// daggers/defensive: 4agil, 1hp
		// daggers/shared: 2acc, 2agil, 1hp
		else if (weaponName.contains(" daggers")) {// TODO ad weapon_type enum
			switch (getDto().getAttackStyleId()) {
				case 1:// aggressive
					StatsDao.addExpToPlayer(getId(), Stats.ACCURACY, points * 4);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
				break;
				case 2: // defensive
					StatsDao.addExpToPlayer(getId(), Stats.AGILITY, points * 4);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
					break;
				default: // shared or other
					StatsDao.addExpToPlayer(getId(), Stats.ACCURACY, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.AGILITY, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
					break;
			}
		}
		
		// sword/aggressive: 2str, 2acc, 1hp
		// sword/defensive: 2def, 2agil, 1hp
		// sword/shared: 1str, 1acc, 1def, 1agil, 1hp
		else {
			switch (getDto().getAttackStyleId()) {
				case 1:// aggressive
					StatsDao.addExpToPlayer(getId(), Stats.STRENGTH, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.ACCURACY, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
				break;
				case 2: // defensive
					StatsDao.addExpToPlayer(getId(), Stats.DEFENCE, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.AGILITY, points * 2);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
					break;
				default: // shared or other
					StatsDao.addExpToPlayer(getId(), Stats.STRENGTH, points);
					StatsDao.addExpToPlayer(getId(), Stats.ACCURACY, points);
					StatsDao.addExpToPlayer(getId(), Stats.DEFENCE, points);
					StatsDao.addExpToPlayer(getId(), Stats.AGILITY, points);
					StatsDao.addExpToPlayer(getId(), Stats.HITPOINTS, points);
					break;
			}
		}
		
		Map<Integer, Integer> currentStatExp = StatsDao.getAllStatExpByPlayerId(getId());
		
		AddExpResponse response = new AddExpResponse();
		for (Map.Entry<Integer, Integer> statExp : currentStatExp.entrySet()) {
			int diff = statExp.getValue() - expBefore.get(statExp.getKey()); 
			if (diff > 0)
				response.addExp(statExp.getKey(), diff);
		}		
		responseMaps.addClientOnlyResponse(this, response);
		
		// TODO update stat cache if the exp gain gives us a level
		refreshStats(currentStatExp);
		PlayerUpdateResponse playerUpdate = new PlayerUpdateResponse();
		playerUpdate.setId(getId());
		playerUpdate.setCmb(StatsDao.getCombatLevelByPlayerId(getId()));
		responseMaps.addBroadcastResponse(playerUpdate);// should be local
	}
	
	@Override
	public void onHit(int damage, ResponseMaps responseMaps) {
		currentHp -= damage;
		if (currentHp < 0)
			currentHp = 0;
		
		// you have 10 hp max, 1hp remaining
		// relative boost should be -9
		// therefore: -max + current
		
		StatsDao.setRelativeBoostByPlayerIdStatId(getId(), Stats.HITPOINTS.getValue(), -this.dto.getMaxHp() + currentHp);
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
		HashMap<Stats, Integer> stats = new HashMap<>();
		for (Map.Entry<Integer, Integer> entry : StatsDao.getStatsByPlayerId(getId()).entrySet())
			stats.put(Stats.withValue(entry.getKey()), StatsDao.getLevelFromExp(entry.getValue()));
		setStats(stats);
		
		EquipmentBonusDto equipment = EquipmentDao.getEquipmentBonusesByPlayerId(getId());
		HashMap<Stats, Integer> bonuses = new HashMap<>();
		bonuses.put(Stats.STRENGTH, equipment.getStr());
		bonuses.put(Stats.ACCURACY, equipment.getAcc());
		bonuses.put(Stats.DEFENCE, equipment.getDef());
		bonuses.put(Stats.AGILITY, equipment.getAgil());
		bonuses.put(Stats.HITPOINTS, equipment.getHp());
		setBonuses(bonuses);
		
		int weaponCooldown = equipment.getSpeed();
		if (weaponCooldown == 0)
			weaponCooldown = 3;// no weapon equipped, default to speed between sword/daggers
		setMaxCooldown(weaponCooldown);
	}
	
	@Override
	public int getExp() {
		return StatsDao.getCombatLevelByPlayerId(getId());
	}
}
