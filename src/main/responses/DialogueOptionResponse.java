package main.responses;

import java.util.ArrayList;

import lombok.Setter;
import main.database.DialogueDao;
import main.database.NpcDialogueDto;
import main.database.NpcDialogueOptionDto;
import main.processing.Player;
import main.requests.DialogueOptionRequest;
import main.requests.Request;

public class DialogueOptionResponse extends Response {
	@Setter private ArrayList<NpcDialogueOptionDto> options;
	public DialogueOptionResponse() {
		setAction("dialogue_option");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		NpcDialogueDto dialogue = player.getCurrentDialogue();
		if (dialogue == null)
			return;
		
		if (!(req instanceof DialogueOptionRequest))
			return;
		
		NpcDialogueOptionDto selectedOption = DialogueDao.getDialogueOption(dialogue.getNpcId(), dialogue.getPointId(), req.getId());
		if (selectedOption == null) {
			// setting to null causes the DialogueResponse to send an empty message to the player, cleaning up the client side
			player.setCurrentDialogue(null);
			new DialogueResponse().process(null, player, responseMaps);
			return;
		}
		
		NpcDialogueDto newDialogue = DialogueDao.getDialogue(dialogue.getNpcId(), dialogue.getPointId(), selectedOption.getDialogueDest());
		// sometimes the chosen dialogue triggers a new conversation point.
		// next time the player talks to the npc, it'll start on that point.
		if (newDialogue.getPointId() != 0)
			DialogueDao.setPlayerNpcDialogueEntryPoint(player.getId(), newDialogue.getNpcId(), selectedOption.getNextPointId());
		
		player.setCurrentDialogue(newDialogue);
		DialogueResponse dialogueResponse = new DialogueResponse();
		dialogueResponse.setDialogue(newDialogue.getDialogue());
		responseMaps.addClientOnlyResponse(player, dialogueResponse);
	}
}
