package responses;

import lombok.Getter;
import lombok.Setter;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.FightManager;
import processing.managers.FightManager.Fight;
import requests.MessageRequest;
import requests.Request;
import types.DuelRules;

public abstract class Response {
	public enum ResponseType {
		broadcast,
		client_only,
		local,
		no_response
	};
	
	@Getter
	private int success = 1;
	
	@Setter private String responseText = "";
	@Getter @Setter private String action;
	@Setter private String colour = null;
	
	@Setter protected transient String combatLockedMessage = "you can't do that during combat!";
	@Setter private transient String noRetreatDuelMessage = null;
	@Setter private transient boolean combatInterrupt = true;

	public void setRecoAndResponseText(int success, String responseText) {
		this.success = success;
		this.responseText = responseText;
	}
	
	public final void processSuper(Request req, Player player, ResponseMaps responseMaps) {
		// message request is a special case as it's the only action that doesn't directly affect the player
		if (!(req instanceof MessageRequest) && player.getState() == PlayerState.dead)
			return;
		
		// TODO check if in fight
		if (!handleCombat(req, player, responseMaps))
			return;
		
		// TODO check if next to
		
		process(req, player, responseMaps);
	}
	
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		final Fight fight = FightManager.getFightByPlayerId(player.getId());
		if (FightManager.fightWithFighterIsBattleLocked(player) || (!combatInterrupt && fight != null)) {
			if (fight.getRules() != null && (fight.getRules() & DuelRules.no_retreat.getValue()) > 0) {
				setRecoAndResponseText(0, noRetreatDuelMessage != null ? noRetreatDuelMessage : combatLockedMessage);
			} else {
				setRecoAndResponseText(0, combatLockedMessage);
			}
			
			responseMaps.addClientOnlyResponse(player, this);
			return false;
		}
		
		if (fight != null)
			FightManager.cancelFight(player, responseMaps);
		return true;
	}

	public abstract void process(Request req, Player player, ResponseMaps responseMaps);

}
