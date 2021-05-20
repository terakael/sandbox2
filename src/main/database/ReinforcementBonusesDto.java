package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ReinforcementBonusesDto {
	private int reinforcementId;
	private int procChance;
	private float soakPct;
}
