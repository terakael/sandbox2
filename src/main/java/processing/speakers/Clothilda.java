package processing.speakers;

import database.dto.NpcDialogueDto;
import database.dto.NpcDialogueOptionDto;
import processing.attackable.Player;
import processing.managers.ShopManager;
import responses.ResponseMaps;
import responses.ShowBaseAnimationsWindowResponse;
import responses.ShowShopResponse;

public class Clothilda implements Speaker {

	@Override
	public void preShowDialogue(NpcDialogueDto dialogueDto, Player player, ResponseMaps responseMaps) {
		
	}

	@Override
	public void postShowDialogue(NpcDialogueDto dialogueDto, Player player, ResponseMaps responseMaps) {
		switch (dialogueDto.getDialogueId()) {
		case 5: // "take a look!"
			new ShowShopResponse(ShopManager.getShopByOwnerId(59)).process(null, player, responseMaps);
			break;
			
		case 7: // "let's get started!"
			new ShowBaseAnimationsWindowResponse().process(null, player, responseMaps);
			break;
		}
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
