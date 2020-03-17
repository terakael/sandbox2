package main;

import java.util.Map;

public class Stats {
	int strength = 0;
	int accuracy = 0;
	int defence = 0;
	int agility = 0;
	int hitpoints = 10;
	int magic = 0;
	int mining = 0;
	int smithing = 0;
	int herblore = 0;
	int fishing = 0;
	int cooking = 0;
	int total = 0;
	public Stats(Map<String, Integer> statList) {
		this.strength = statList.get("strength");
		this.accuracy = statList.get("accuracy");
		this.defence = statList.get("defence");
		this.agility = statList.get("agility");
		this.hitpoints = statList.get("hitpoints");
		this.magic = statList.get("magic");
		this.mining = statList.get("mining");
		this.smithing = statList.get("smithing");
		this.herblore = statList.get("herblore");
		this.fishing = statList.get("fishing");
		this.cooking = statList.get("cooking");
		this.total = statList.get("total");
	}
}
