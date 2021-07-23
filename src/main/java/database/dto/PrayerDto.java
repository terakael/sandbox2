package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PrayerDto {
	private int id;
	private String name;
	private String description;
	private int iconId;
	private int level;
	private float drainRate;
}
