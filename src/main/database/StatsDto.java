package main.database;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatsDto {
	private int playerId;
	private int statId;
	private int exp;
}
