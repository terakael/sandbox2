package responses;

import java.util.List;
import java.util.stream.Collectors;

import database.dao.DialogueDao;
import database.dto.NpcDialogueDto;
import database.dto.NpcDialogueOptionDto;
import lombok.Setter;
import processing.attackable.Player;
import processing.managers.DialogueManager;
import processing.speakers.SpeakerManager;
import requests.Request;

@Setter
public class NextDialogueResponse extends Response {
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		NpcDialogueDto currentDialogue = player.getCurrentDialogue();
		if (currentDialogue == null) {
			// player must have a current dialogue in order to move to the next dialogue.
			// sending an empty dialogue response tells the client to close the window
			DialogueManager.showDialogue(null, player, responseMaps);
			return;
		}
		
		// sometimes after clicking a dialogue, the next dialogue is dialogue options.		
		if (handleDialogueOptions(currentDialogue, player, responseMaps))
			return;
		
		final NpcDialogueDto nextDialogue = DialogueDao.getDialogue(currentDialogue.getNpcId(), currentDialogue.getPointId(), currentDialogue.getDialogueId() + 1);
		DialogueManager.showDialogue(nextDialogue, player, responseMaps);
	}
	
	private static boolean handleDialogueOptions(NpcDialogueDto currentDialogue, Player player, ResponseMaps responseMaps) {
		List<NpcDialogueOptionDto> options = DialogueDao.getDialogueOptionsBySrcDialogueId(
				currentDialogue.getNpcId(), 
				currentDialogue.getPointId(), 
				currentDialogue.getDialogueId());
		
		if (options.isEmpty())
			return false;
		
		options = options.stream()
					.filter(option -> SpeakerManager.dialogueOptionMeetsDisplayCriteria(option, player))
					.collect(Collectors.toList());
		
		if (options.isEmpty()) {
			// if none of the options meet the display criteria, then end the conversation
			DialogueManager.showDialogue(null, player, responseMaps);
		} else {
			new ShowDialogueOptionsResponse(options.stream()
					.collect(Collectors.toMap(e -> e.getOptionId(), e -> e.getOptionText())))
			.process(null, player, responseMaps);
		}
		
		return true;
	}
}
