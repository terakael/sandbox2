package processing.attackable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.stream.Collectors;

import database.dao.DoorDao;
import lombok.Getter;
import lombok.Setter;
import processing.PathFinder;
import processing.managers.FightManager;
import responses.ResponseMaps;
import types.DamageTypes;
import types.Stats;
import utils.RandomUtil;

public abstract class Attackable {
	private static Random rand = new Random();
	
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
	
	protected boolean popPath() {
		if (path.isEmpty())
			return false;
		
		int nextTileId = path.peek();
		
		int currentTileDoorStatus = DoorDao.getDoorImpassableByTileId(floor, tileId);
		int nextTileDoorStatus = DoorDao.getDoorImpassableByTileId(floor, nextTileId);
		
		if (currentTileDoorStatus > 0 || nextTileDoorStatus > 0) {
			// one of these tiles is a door; check if we can pass through.
			// if it's diagonal we won't bother re-checking, I don't think it's possible for a door opening to mess up a diagonal move?
			if (!PathFinder.isDiagonal(tileId, nextTileId) && !PathFinder.isNextTo(floor, tileId, nextTileId))
				path.clear();
		}
		
		if (!path.isEmpty()) {
			setTileId(path.pop());
			return true;
		}
		
		return false;
	}
	
	public boolean readyToHit() {
		if (cooldown == 0)
			return true;
		return --cooldown == 0;
	}
	
	public int hit(Attackable target, ResponseMaps responseMaps) {
		cooldown = maxCooldown;
		
		int maxHitFromLevel = ((getStats().get(Stats.STRENGTH) + getBoosts().get(Stats.STRENGTH)) / 6) + 1;
		int maxHitFromBonus = (bonuses.get(Stats.STRENGTH) * 2) / 10;
		
		int maxHit = maxHitFromLevel + maxHitFromBonus;
		maxHit = postMaxHitModifications(maxHit);
		
		// number line starts with 100 evenly distributed elements
		// e.g. if your max hit is 1, it will have 50 0s and 50 1s
		// if your max hit is 9, it will have 10 0s, 10 1s etc
		
		// include 0 so maxHit + 1
		int distribution = 100 / (maxHit + 1);
		List<Integer> numberLine = new ArrayList<>();
		for (int i = 0; i <= maxHit; ++i) {
			for (int j = 0; j < distribution; ++j)
				numberLine.add(i);
		}
		
		while (numberLine.size() < 100)
			numberLine.add(0);
		Collections.sort(numberLine);
		
		int totalAccuracy = getStats().get(Stats.ACCURACY) + getBoosts().get(Stats.ACCURACY);
		int totalDamage = getStats().get(Stats.STRENGTH) + getBoosts().get(Stats.STRENGTH);
		int spread = (totalAccuracy + bonuses.get(Stats.ACCURACY)) / 10;
		int origin = (maxHit/2) + Math.min((maxHit/2), Math.max(-(maxHit/2) + spread, totalAccuracy - totalDamage));
		origin = postAccuracyModifications(origin);
		
		
		final List<Integer> distinctNumberLine = numberLine.stream().distinct().collect(Collectors.toList());
		List<Integer> numbersToBoost = new ArrayList<>();
		for (int i = spread, counter = 0; i >= 0; --i, ++counter) {
			if (origin + counter < distinctNumberLine.size()) {
				for (int j = 0; j < i; ++j)
					numbersToBoost.add(distinctNumberLine.get(origin + counter));
			}
			
			if (origin - (counter + 1) >= 0 && origin - (counter + 1) < distinctNumberLine.size()) {
				for (int j = 0; j < i; ++j)
					numbersToBoost.add(distinctNumberLine.get(origin - (counter + 1)));
			}
		}
		numberLine.addAll(numbersToBoost);
		Collections.sort(numberLine);

		return numberLine.get(rand.nextInt(numberLine.size()));
	}
	
	public boolean block(Attackable attacker) {
		int attackBonus = attacker.getStats().get(Stats.ACCURACY) + attacker.getBoosts().get(Stats.ACCURACY) + attacker.getBonuses().get(Stats.ACCURACY);
		int defenceBonus = getStats().get(Stats.DEFENCE) + getBoosts().get(Stats.DEFENCE) + getBonuses().get(Stats.DEFENCE);
		
		int baseBlockChance = 25;
		int blockChance = baseBlockChance + Math.max(-baseBlockChance, (defenceBonus - attackBonus) / 2);
		blockChance = postBlockChanceModifications(blockChance);
		
		return RandomUtil.getRandom(0, 100) < blockChance;
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
	
	protected void clearPoison() {
		poisonDamage = 0;
		poisonDamageRemainingTicks = 0;
		poisonImmunityRemainingTicks = 0;
	}
	
	public void setTarget(Attackable target) {
		this.target = target; // this one gets cleared as soon as the fight stops
		
		if (target != null)
			this.lastTarget = target;// this one remains until death (in case the player dies of poison or in some way when they're not under attack)
	}
	
	protected int postMaxHitModifications(int maxHit) {
		return maxHit;
	}
	
	protected int postAccuracyModifications(int accuracy) {
		return accuracy;
	}
	
	protected int postBlockChanceModifications(int blockChance) {
		return blockChance;
	}
}
