package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NpcDropDto {
	int npcId;
	int itemId;
	int count;
	int rate;
}
