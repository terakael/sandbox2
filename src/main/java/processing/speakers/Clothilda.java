package processing.speakers;

import database.dto.NpcDialogueDto;
import database.dto.NpcDialogueOptionDto;
import processing.attackable.Player;
import responses.ResponseMaps;
import responses.ShowBaseAnimationsWindowResponse;

public class Clothilda implements Speaker {

	@Override
	public void preShowDialogue(NpcDialogueDto dialogueDto, Player player, ResponseMaps responseMaps) {
		
	}

	@Override
	public void postShowDialogue(NpcDialogueDto dialogueDto, Player player, ResponseMaps responseMaps) {
		if (dialogueDto.getDialogueId() == 7) // "take a look!"
			new ShowBaseAnimationsWindowResponse().process(null, player, responseMaps);
	}

	@Override
	public boolean dialogueOptionMeetsDisplayCriteria(NpcDialogueOptionDto option, Player player) {
		return true;
	}

	@Override
	public NpcDialogueDto switchDialogue(NpcDialogueDto previousDialogueDto, Player player, ResponseMaps responseMaps) {
		return null;
	}

}
