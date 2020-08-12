package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import main.database.entity.annotations.Column;

@AllArgsConstructor
@Getter
public class ReinforcementBonusesDto {
	private int reinforcementId;
	private int procChance;
	private float soakPct;
}
