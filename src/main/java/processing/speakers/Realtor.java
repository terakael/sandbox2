package processing.speakers;

import database.dto.NpcDialogueDto;
import database.dto.NpcDialogueOptionDto;
import processing.attackable.Player;
import requests.GetHouseInfoRequest;
import responses.GetHouseInfoResponse;
import responses.ResponseMaps;

public class Realtor implements Speaker {

	@Override
	public void preShowDialogue(NpcDialogueDto dialogueDto, Player player, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postShowDialogue(NpcDialogueDto dialogueDto, Player player, ResponseMaps responseMaps) {
		new GetHouseInfoResponse().process(new GetHouseInfoRequest(), player, responseMaps);
	}

	@Override
	public boolean dialogueOptionMeetsDisplayCriteria(NpcDialogueOptionDto option, Player player) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public NpcDialogueDto switchDialogue(NpcDialogueDto previousDialogueDto, Player player, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
		return null;
	}

}
