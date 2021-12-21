package database.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NpcDialogueOptionDto {
	private final int npcId;
	private final int optionId;
	private final String optionText;
	
	// client doesn't need to know about these
	private transient final int pointId;
	private transient final int dialogueSrc;
	private transient final int dialogueDest;
	private transient final int nextPointId;
}
