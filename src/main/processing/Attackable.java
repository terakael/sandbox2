package main.processing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import lombok.Getter;
import lombok.Setter;
import main.responses.ResponseMaps;
import main.types.Stats;

public abstract class Attackable {
	private static Random rand = new Random();
	
	@Setter @Getter protected HashMap<Stats, Integer> stats = new HashMap<>();
	@Setter private HashMap<Stats, Integer> bonuses = null;
	@Setter private HashMap<Stats, Integer> boosts = null;
	@Setter @Getter protected int currentHp;
	@Setter @Getter protected int tileId;
	@Setter @Getter protected Attackable target = null;
	
	@Setter protected int maxCooldown = 5;
	@Setter @Getter protected int cooldown = maxCooldown;
	
	public abstract void onDeath(Attackable killer, ResponseMaps responseMaps);
	public abstract void onKill(Attackable killed, ResponseMaps responseMaps);
	public abstract void onHit(int damage, ResponseMaps responseMaps);
	public abstract void setStatsAndBonuses();
	public abstract int getExp();
	
	public boolean readyToHit() {
		if (cooldown == 0)
			return true;
		return --cooldown == 0;
	}
	
	public int hit() {
		cooldown = maxCooldown;
		int maxHit = (int)Math.ceil((stats.get(Stats.STRENGTH) + boosts.get(Stats.STRENGTH)) * 0.18) + (int)Math.ceil(bonuses.get(Stats.STRENGTH) * 0.10);
		// number line starts with 100 evenly distributed elements
		// e.g. if your max hit is 1, it will have 50 0s and 50 1s
		// if your max hit is 9, it will have 10 0s, 10 1s etc
		
		// include 0 so maxHit + 1
		int distribution = 100 / (maxHit + 1);
		ArrayList<Integer> numberLine = new ArrayList<>();
		for (int i = 0; i <= maxHit; ++i) {
			for (int j = 0; j < distribution; ++j)
				numberLine.add(i);
		}
		
		ArrayList<Integer> numbersToBoost = new ArrayList<>();
		int acc = (int)Math.ceil((stats.get(Stats.ACCURACY) + boosts.get(Stats.ACCURACY)) * 0.18) + (int)Math.ceil(bonuses.get(Stats.ACCURACY) * 0.10);
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
	
	public int block() {
		int maxBlock = (int)Math.ceil((stats.get(Stats.DEFENCE) + boosts.get(Stats.DEFENCE)) * 0.18) + (int)Math.ceil(bonuses.get(Stats.DEFENCE) * 0.10);

		int distribution = 100 / (maxBlock + 1);
		ArrayList<Integer> numberLine = new ArrayList<>();
		for (int i = 0; i <= maxBlock; ++i) {
			for (int j = 0; j < distribution; ++j)
				numberLine.add(i);
		}
		
		ArrayList<Integer> numbersToBoost = new ArrayList<>();
		int acc = (int)Math.ceil((stats.get(Stats.AGILITY) + boosts.get(Stats.AGILITY)) * 0.18) + (int)Math.ceil(bonuses.get(Stats.AGILITY) * 0.10);
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
}
