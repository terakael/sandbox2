package responses;

import database.dao.DialogueDao;
import database.dto.NpcDialogueDto;
import database.dto.NpcDialogueOptionDto;
import processing.attackable.Player;
import processing.managers.DialogueManager;
import requests.Request;
import requests.SelectDialogueOptionRequest;

public class SelectDialogueOptionResponse extends Response {
	public SelectDialogueOptionResponse() {
		setAction("select_dialogue_option");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof SelectDialogueOptionRequest))
			return;
		
		NpcDialogueDto dialogue = player.getCurrentDialogue();
		if (dialogue == null)
			return;
		
		NpcDialogueOptionDto selectedOption = DialogueDao.getDialogueOption(dialogue.getNpcId(), dialogue.getPointId(), ((SelectDialogueOptionRequest)req).getOptionId(), dialogue.getDialogueId());
		if (selectedOption == null) {
			// setting to null causes the DialogueResponse to send an empty message to the player, cleaning up the client side
			DialogueManager.showDialogue(null, player, responseMaps);
			return;
		}
		
		// sometimes the chosen dialogue triggers a new conversation point.
		// next time the player talks to the npc, it'll start on that point.
		if (DialogueDao.getPlayerNpcDialogueEntryPoint(player.getId(), dialogue.getNpcId()) != selectedOption.getNextPointId())
			DialogueDao.setPlayerNpcDialogueEntryPoint(player.getId(), dialogue.getNpcId(), selectedOption.getNextPointId());
			
		final NpcDialogueDto nextDialogue = DialogueDao.getDialogue(dialogue.getNpcId(), dialogue.getPointId(), selectedOption.getDialogueDest());
		DialogueManager.showDialogue(nextDialogue, player, responseMaps);
	}
}
