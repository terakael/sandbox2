package main.database;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NpcDialogueOptionDto {
	private int npcId;
	private int optionId;
	private String optionText;
	
	// client doesn't need to know about these
	private transient int pointId;
	private transient int dialogueSrc;
	private transient int dialogueDest;
	private transient int nextPointId;
	private transient int requiredItem1;
	private transient int requiredItem2;
	private transient int requiredItem3;
}
