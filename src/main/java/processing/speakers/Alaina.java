package processing.speakers;

import database.dao.ArtisanMasterDao;
import database.dto.NpcDialogueDto;
import database.dto.NpcDialogueOptionDto;
import processing.attackable.Player;
import processing.managers.ArtisanManager;
import responses.ResponseMaps;
import responses.ShowArtisanShopResponse;
import types.ArtisanShopTabs;

public class Alaina implements Speaker {

	@Override
	public void preShowDialogue(NpcDialogueDto dialogueDto, Player player, ResponseMaps responseMaps) {
		if (dialogueDto.getDialogueId() == 20) { // need a new task
			if (ArtisanManager.currentTaskIsFinished(player.getId())) {
				ArtisanManager.newTask(player, ArtisanMasterDao.getArtisanMasterByNpcId(dialogueDto.getNpcId()), responseMaps);
			}
		}
	}
	
	@Override
	public void postShowDialogue(NpcDialogueDto dialogueDto, Player player, ResponseMaps responseMaps) {
		if (dialogueDto.getDialogueId() == 50) {
			new ShowArtisanShopResponse(ArtisanShopTabs.task).process(null, player, responseMaps);
		}
	}

	@Override
	public boolean dialogueOptionMeetsDisplayCriteria(NpcDialogueOptionDto option, Player player) {
		if (option.getOptionId() == 2) { // i need a new task
			return ArtisanManager.currentTaskIsFinished(player.getId());
		}
		else if (option.getOptionId() == 3) { // what is my current task
			return !ArtisanManager.currentTaskIsFinished(player.getId());
		}
		return true;
	}

	@Override
	public NpcDialogueDto switchDialogue(NpcDialogueDto previousDialogueDto, Player player, ResponseMaps responseMaps) {
		return null;
	}
}
