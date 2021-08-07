package database.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NpcDialogueDto {
	private final int npcId;
	private final int pointId;
	private final int dialogueId;
	private final String dialogue;
}
