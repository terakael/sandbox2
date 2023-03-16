package processing.attackable;

import java.util.HashMap;
import java.util.Stack;

import database.dao.DoorDao;
import lombok.Getter;
import lombok.Setter;
import processing.PathFinder;
import processing.managers.FightManager;
import processing.managers.LockedDoorManager;
import requests.OpenRequest;
import requests.Request;
import responses.OpenCloseResponse;
import responses.ResponseMaps;
import types.DamageTypes;
import types.Stats;
import utils.RandomUtil;

public abstract class Attackable {
	@Setter protected Stack<Integer> path = new Stack<>();// stack of tile_ids
	@Setter @Getter protected HashMap<Stats, Integer> stats = new HashMap<>();
	@Setter @Getter protected HashMap<Stats, Integer> bonuses = new HashMap<>();
	@Setter @Getter private HashMap<Stats, Integer> boosts = null;
	@Setter @Getter protected int currentHp;
	@Setter @Getter protected int tileId;
	@Setter @Getter protected int floor;
	@Getter protected Attackable target = null;
	@Getter protected Attackable lastTarget = null;// record the last person to attack in the case we die of poison (and therefore have no current target)
	
	@Setter protected int maxCooldown = 5;
	@Setter @Getter protected int cooldown = maxCooldown;
	
	private int poisonDamage = 0;// decreases by one each time it hits, until it hits zero.
	
	// remaining ticks until poison hits again.
	private int poisonDamageRemainingTicks = 0;
	private int poisonDamageTicks = 10;// the amount of damage between poison hits
	
	// immunity kicks off as soon as you're poisoned; you cannot be repoisoned until this wears off.
	// this could wear off mid-poison, or a while after the poison is worn off (e.g. if you drank an antipoison or something)
	private int poisonImmunityRemainingTicks = 0;
	
	public abstract void onDeath(Attackable killer, ResponseMaps responseMaps);
	public abstract void onKill(Attackable killed, ResponseMaps responseMaps);
	public abstract void onHit(int damage, DamageTypes type, ResponseMaps responseMaps);
	public abstract void onAttack(int damage, DamageTypes type, ResponseMaps responseMaps);
	public abstract void setStatsAndBonuses();
	public abstract int getExp();
	
	protected boolean popPath(ResponseMaps responseMaps) {
		if (path.isEmpty())
			return false;
		
		int nextTileId = path.peek();
		
		int currentTileDoorStatus = DoorDao.getDoorImpassableByTileId(floor, tileId);
		int nextTileDoorStatus = DoorDao.getDoorImpassableByTileId(floor, nextTileId);
		
		if (currentTileDoorStatus > 0 || nextTileDoorStatus > 0) {
			// one of these tiles is a door; check if we can pass through.
			// if it's diagonal we won't bother re-checking, I don't think it's possible for a door opening to mess up a diagonal move?
			if (!PathFinder.isDiagonal(tileId, nextTileId) && !PathFinder.isNextTo(floor, tileId, nextTileId))
				handleWalkingThroughClosedDoor(currentTileDoorStatus > 0 ? tileId : nextTileId, responseMaps);
		}
		
		if (!path.isEmpty()) {
			setTileId(path.pop());
			return true;
		}
		
		return false;
	}
	
	void handleWalkingThroughClosedDoor(int doorTileId, ResponseMaps responseMaps) {
		path.clear();
	}
	
	public boolean readyToHit() {
		if (cooldown == 0)
			return true;
		return --cooldown == 0;
	}
	
	public int hit(Attackable target, ResponseMaps responseMaps) {
		cooldown = 3;// TODO testing one speed fits all; maxCooldown;
		
		int totalStrength = getStats().get(Stats.STRENGTH) + getBoosts().get(Stats.STRENGTH) + bonuses.get(Stats.STRENGTH);
		totalStrength = postDamageModifications(totalStrength);
		
		int totalAccuracy = getStats().get(Stats.ACCURACY) + getBoosts().get(Stats.ACCURACY) + (bonuses.get(Stats.ACCURACY) * 2);
		totalAccuracy = postAccuracyModifications(totalAccuracy);
		
		int opponentTotalDefence =  target.getStats().get(Stats.DEFENCE) + target.getBoosts().get(Stats.DEFENCE) + (target.getBonuses().get(Stats.DEFENCE) * 2);
		opponentTotalDefence = target.postBlockChanceModifications(opponentTotalDefence);
		
		final float ratio = opponentTotalDefence == 0 ? 100 : (float)totalAccuracy / (float)opponentTotalDefence;
		final int blockChance = Math.round(ratio/(ratio+1) * 100);
		
		final int maxHit = (int)Math.ceil(totalStrength/7.0);
		return RandomUtil.chance(blockChance) ? RandomUtil.getRandom(1, maxHit + 1) : 0;
	}
	
	public DamageTypes getDamageType() {
		return DamageTypes.STANDARD;
	}
	
	public boolean isInCombat() {
		return FightManager.fightWithFighterExists(this);
	}
	
	public boolean isImmuneToPoison() {
		return poisonImmunityRemainingTicks > 0;
	}
	
	public void inflictPoison(int maxDamage) {
		if (isImmuneToPoison())
			return;
		
		poisonImmunityRemainingTicks = (maxDamage * poisonDamageTicks);// just until the poison wears off in the default case.
		
		poisonDamageRemainingTicks = poisonDamageTicks;// don't poison right away.
		poisonDamage = maxDamage;
	}
	
	protected void processPoison(ResponseMaps responseMaps) {
		if (poisonImmunityRemainingTicks > 0)
			--poisonImmunityRemainingTicks;
		
		if (poisonDamage == 0)
			return;
		
		if (--poisonDamageRemainingTicks == 0) {
			poisonDamageRemainingTicks = poisonDamageTicks;
			onHit(poisonDamage--, DamageTypes.POISON, responseMaps);
			if (currentHp == 0)
				onDeath(lastTarget, responseMaps);// last target is the same as current target, but remains after the fight is cancelled
		}
	}
	
	public void clearPoison() {
		poisonDamage = 0;
		poisonDamageRemainingTicks = 0;
		poisonImmunityRemainingTicks = 0;
	}
	
	public void setPoisonImmunity(int ticks) {
		poisonImmunityRemainingTicks = ticks;
	}
	
	public boolean isPoisoned() {
		return poisonDamage > 0;
	}
	
	public void setTarget(Attackable target) {
		this.target = target; // this one gets cleared as soon as the fight stops
		
		if (target != null)
			this.lastTarget = target;// this one remains until death (in case the player dies of poison or in some way when they're not under attack)
	}
	
	protected int postDamageModifications(int maxHit) {
		return maxHit;
	}
	
	protected int postAccuracyModifications(int accuracy) {
		return accuracy;
	}
	
	protected int postBlockChanceModifications(int blockChance) {
		return blockChance;
	}
}
