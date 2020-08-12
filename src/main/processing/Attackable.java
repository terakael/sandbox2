package main.processing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import main.responses.ResponseMaps;
import main.types.DamageTypes;
import main.types.Stats;

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
	public abstract void setStatsAndBonuses();
	public abstract int getExp();
	
	protected void popPath() {
		int nextTile = path.pop();
		setTileId(nextTile);
	}
	
	public boolean readyToHit() {
		if (cooldown == 0)
			return true;
		return --cooldown == 0;
	}
	
	public int hit() {
		cooldown = maxCooldown;
		
		int maxHitFromLevel = ((stats.get(Stats.STRENGTH) + boosts.get(Stats.STRENGTH)) / 6) + 1;
		int maxHitFromBonus = (bonuses.get(Stats.STRENGTH) * 2) / 10;
		
		int maxHit = maxHitFromLevel + maxHitFromBonus;
		
//		int bonus = (int)Math.ceil(bonuses.get(Stats.STRENGTH) * 0.10);
		
//		int maxHit = (int)Math.ceil((stats.get(Stats.STRENGTH) + boosts.get(Stats.STRENGTH)) * 0.18) + (int)Math.ceil(bonuses.get(Stats.STRENGTH) * 0.10);
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
		
		int totalAccuracy = (stats.get(Stats.ACCURACY) + boosts.get(Stats.ACCURACY));
		int totalDamage = (stats.get(Stats.STRENGTH) + boosts.get(Stats.STRENGTH));
		int spread = (stats.get(Stats.ACCURACY) + boosts.get(Stats.ACCURACY) + bonuses.get(Stats.ACCURACY)) / 10;
		int origin = (maxHit/2) + Math.min((maxHit/2), Math.max(-(maxHit/2) + spread, totalAccuracy - totalDamage));
		
		
		final List<Integer> distinctNumberLine = numberLine.stream().distinct().collect(Collectors.toList());
		List<Integer> numbersToBoost = new ArrayList<>();
		for (int i = spread, counter = 0; i >= 0; --i, ++counter) {
			if (origin + counter < distinctNumberLine.size()) {
				for (int j = 0; j < i; ++j)
					numbersToBoost.add(distinctNumberLine.get(origin + counter));
			}
			
			if (origin - (counter + 1) >= 0) {
				for (int j = 0; j < i; ++j)
					numbersToBoost.add(distinctNumberLine.get(origin - (counter + 1)));
			}
		}
		numberLine.addAll(numbersToBoost);
		Collections.sort(numberLine);

		return numberLine.get(rand.nextInt(numberLine.size()));
	}
	
	public int block() {
//		int maxBlock = (int)Math.ceil((stats.get(Stats.DEFENCE) + boosts.get(Stats.DEFENCE)) * 0.22) + (int)Math.ceil(bonuses.get(Stats.DEFENCE) * 0.25);
		
		int blockFromLevel = ((stats.get(Stats.DEFENCE) + boosts.get(Stats.DEFENCE)) / 6) + 1;
		int blockFromBonus = (bonuses.get(Stats.DEFENCE) * 2) / 10;
		
		int maxBlock = blockFromLevel + blockFromBonus;

		int distribution = 100 / (maxBlock + 1);
		ArrayList<Integer> numberLine = new ArrayList<>();
		for (int i = 0; i <= maxBlock; ++i) {
			for (int j = 0; j < distribution; ++j)
				numberLine.add(i);
		}
		
		ArrayList<Integer> numbersToBoost = new ArrayList<>();
		int accuracyFromLevel = ((stats.get(Stats.AGILITY) + boosts.get(Stats.AGILITY)) / 5) + 1;
		int accuracyFromBonus = (bonuses.get(Stats.AGILITY) * 3) / 10;
//		int acc = (int)Math.ceil((stats.get(Stats.AGILITY) + boosts.get(Stats.AGILITY)) * 0.22) + (int)Math.ceil(bonuses.get(Stats.AGILITY) * 0.25);
		int acc = accuracyFromLevel + accuracyFromBonus;
		for (int i = numberLine.size() - 1; i >= Math.max((numberLine.size() - acc), 0); --i) {
			numbersToBoost.add(numberLine.get(i));
		}
		Collections.sort(numbersToBoost, Collections.reverseOrder());
		
		for (int numberToBoost : numbersToBoost) {
			for (int j = 0; j < acc; ++j) {
				numberLine.add(numberToBoost);
			}
			--acc;
		}

		return numberLine.get(rand.nextInt(numberLine.size()));
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
}
