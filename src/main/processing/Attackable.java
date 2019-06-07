package main.processing;

import java.util.HashMap;
import java.util.Random;

import lombok.Getter;
import lombok.Setter;
import main.responses.ResponseMaps;
import main.types.Stats;

public abstract class Attackable {
	private static Random rand = new Random();
	
	@Setter private HashMap<Stats, Integer> stats = null;
	@Setter private HashMap<Stats, Integer> bonuses = null;
	@Setter @Getter protected int currentHp;
	@Setter @Getter protected int tileId;
	@Setter @Getter protected Attackable target = null;
	
	@Setter protected int maxCooldown = 5;
	@Setter @Getter protected int cooldown = maxCooldown;
	
	public abstract void onDeath(ResponseMaps responseMaps);
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
		int maxHit = (int)Math.ceil(stats.get(Stats.STRENGTH) * 0.15) + (int)Math.ceil(bonuses.get(Stats.STRENGTH) * 0.15) + 1;
//		System.out.println("str: " + stats.get(1) + ", max: " + maxHit);
		//int acc = (int)Math.sqrt(stats.get("accuracy")) + this.bonuses.getAcc();
		
		return rand.nextInt(maxHit);
	}
	
	public int block() {
		int maxBlock = (int)Math.ceil(stats.get(Stats.DEFENCE) * 0.15) + (int)Math.ceil(bonuses.get(Stats.DEFENCE) * 0.15) + 1;
		//int agil = (int)Math.sqrt(stats.get("agility")) + this.bonuses.getAgil();
		return rand.nextInt(maxBlock);
	}
}
