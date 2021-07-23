package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class PlayerStorageDto {
	private int playerId;
	private int storageTypeId;
	private int slot;
	private int itemId;
	private int count;
	private int charges;
}
