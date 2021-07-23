package database.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NpcDialogueDto {
	private int npcId;
	private int pointId;
	private int dialogueId;
	private String dialogue;
}
