package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NpcMessageDto {
	private int npcId;
	private int messageId;
	private String message;
}
