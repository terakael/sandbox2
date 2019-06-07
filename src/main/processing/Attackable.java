package main.processing;

import java.util.HashMap;
import java.util.Random;

import lombok.Getter;
import lombok.Setter;
import main.responses.ResponseMaps;

public abstract class Attackable {
	private static Random rand = new Random();
	
	@Setter private HashMap<String, Integer> stats = null;
	@Setter private HashMap<String, Integer> bonuses = null;
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
		int maxHit = (int)Math.ceil(stats.get("strength") * 0.15) + (int)Math.ceil(bonuses.get("strength") * 0.15) + 1;
		System.out.println("str: " + stats.get("strength") + ", max: " + maxHit);
		//int acc = (int)Math.sqrt(stats.get("accuracy")) + this.bonuses.getAcc();
		
		return rand.nextInt(maxHit);
	}
	
	public int block() {
		int maxBlock = (int)Math.ceil(stats.get("defence") * 0.15) + (int)Math.ceil(bonuses.get("defence") * 0.15) + 1;
		//int agil = (int)Math.sqrt(stats.get("agility")) + this.bonuses.getAgil();
		return rand.nextInt(maxBlock);
	}
}
