package processing.speakers;

import database.dto.NpcDialogueDto;
import database.dto.NpcDialogueOptionDto;
import processing.attackable.Player;
import responses.ResponseMaps;

public interface Speaker {
	public void preShowDialogue(NpcDialogueDto dialogueDto, Player player, ResponseMaps responseMaps);
	public void postShowDialogue(NpcDialogueDto dialogueDto, Player player, ResponseMaps responseMaps);
	public boolean dialogueOptionMeetsDisplayCriteria(NpcDialogueOptionDto option, Player player);
	public NpcDialogueDto switchDialogue(NpcDialogueDto previousDialogueDto, Player player, ResponseMaps responseMaps);
}
