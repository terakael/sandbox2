package responses;

import java.util.List;

import database.dao.DialogueDao;
import database.dao.NPCDao;
import database.dao.NpcMessageDao;
import database.dto.NpcDialogueDto;
import processing.PathFinder;
import processing.attackable.NPC;
import processing.attackable.Player;
import processing.managers.DialogueManager;
import processing.managers.FightManager;
import processing.managers.NPCManager;
import requests.Request;
import requests.TalkToRequest;
import utils.RandomUtil;

@SuppressWarnings("unused")
public class TalkToResponse extends Response {
	private int objectId;
	private String message = "";
	
	public TalkToResponse() {
		setAction("talk to");
	}
	
	public TalkToResponse(int objectId, String message) {
		setAction("talk to");
		this.objectId = objectId;
		this.message = message;
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof TalkToRequest))
			return;
		
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you're too busy fighting!");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		TalkToRequest request = (TalkToRequest)req;
		NPC npc = NPCManager.get().getNpcByInstanceId(player.getFloor(), request.getObjectId());// request tileid is the instnace id
		if (npc == null) {
			setRecoAndResponseText(0, "you can't talk to that.");
			return;
		}
		
		if (!PathFinder.isNextTo(player.getFloor(), npc.getTileId(), player.getTileId())) {
			player.setTarget(npc);	
			player.setSavedRequest(request);
		} else {
			// talk to it
			player.faceDirection(npc.getTileId(), responseMaps);
			handleTalkTo(npc, player, responseMaps);
		}
		
	}
	
	private void handleTalkTo(NPC npc, Player player, ResponseMaps responseMaps) {
		player.setCurrentDialogue(null); // reset the dialogue as this is a new convo
		
		NpcDialogueDto initialDialogue = DialogueDao.getEntryDialogueByPlayerIdNpcId(player.getId(), npc.getId());
		if (initialDialogue == null)
			initialDialogue = DialogueDao.getDialogue(npc.getId(), 1, 1);

		if (initialDialogue != null) {
			DialogueManager.showDialogue(initialDialogue, player, responseMaps);
			return;
		}
		
		// if there's no dialogue, maybe there's a simple message from the npc
		objectId = npc.getInstanceId();
		List<String> messages = NpcMessageDao.getMessagesByNpcId(npc.getId()); 
		if (!messages.isEmpty()) {
			message = messages.get(RandomUtil.getRandom(0, messages.size()));
			responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), this);
		} else {
			setRecoAndResponseText(0, "they don't seem interested in talking.");
			responseMaps.addClientOnlyResponse(player, this);
		}
	}
	
}
