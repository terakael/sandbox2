package processing.managers;

import database.dao.NPCDao;
import database.dto.NpcDialogueDto;
import processing.attackable.Player;
import processing.speakers.SpeakerManager;
import responses.ResponseMaps;
import responses.ShowDialogueResponse;

public class DialogueManager {
	public static void showDialogue(NpcDialogueDto dto, Player player, ResponseMaps responseMaps) {
		NpcDialogueDto previousDialogue = player.getCurrentDialogue();
		final ShowDialogueResponse dialogueResponse = new ShowDialogueResponse();
		
		// in cases such as tybalt's tasks, we want to be able to switch the dialogue based on the current task.
		// e.g. when we click "i've finished my task!" and then tybalt says "great job!", the following dialogue
		// is switched based on whatever the new task should be.
		NpcDialogueDto switchDialogue = SpeakerManager.switchDialogue(previousDialogue, player, responseMaps);
		if (switchDialogue != null)
			dto = switchDialogue;
		
		player.setCurrentDialogue(dto);
		if (dto != null) {
			SpeakerManager.preShowDialogue(dto, player, responseMaps);
			dialogueResponse.setDialogue(replaceVariables(dto, player));
			dialogueResponse.setSpeaker(NPCDao.getNpcNameById(dto.getNpcId()));
		}
		
		responseMaps.addClientOnlyResponse(player, dialogueResponse);
		
		if (previousDialogue != null)
			SpeakerManager.postShowDialogue(previousDialogue, player, responseMaps);
	}
	
	private static String replaceVariables(NpcDialogueDto dialogueDto, Player player) {
		String dialogue = dialogueDto.getDialogue();
		
		dialogue = dialogue.replace("${playerName}", player.getDto().getName());
		dialogue = dialogue.replace("${artisanTask}", ArtisanManager.getTaskString(player.getId()));
		
		return dialogue;
	}
}
