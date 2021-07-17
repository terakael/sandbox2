package main.responses;

import java.util.HashMap;
import java.util.Map;

import main.processing.attackable.Player;
import main.processing.managers.TradeManager;
import main.processing.managers.TradeManager.Trade;
import main.requests.Request;
import main.requests.ToggleDuelRuleRequest;
import main.types.DuelRules;

public class ToggleDuelRuleResponse extends Response {
	private Map<Integer, Integer> rules;
	
	public ToggleDuelRuleResponse() {
		setAction("toggle_duel_rule");
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		Trade trade = TradeManager.getTradeWithPlayer(player);
		if (!trade.isDuel())
			return;
		
		ToggleDuelRuleRequest request = (ToggleDuelRuleRequest)req;
		if (!DuelRules.isValidRule(request.getRule()))
			return;
		
		if (trade.playerIsP1(player)) {
			int currentp1Rules = trade.getP1Rules();
			
			if ((currentp1Rules & request.getRule()) > 0) {
				currentp1Rules &= ~request.getRule();
			} else {
				currentp1Rules |= request.getRule();
			}
			
			trade.setP1Rules(currentp1Rules);
		} else {
			int currentp2Rules = trade.getP2Rules();
			
			if ((currentp2Rules & request.getRule()) > 0) {
				currentp2Rules &= ~request.getRule();
			} else {
				currentp2Rules |= request.getRule();
			}
			
			trade.setP2Rules(currentp2Rules);
		}
		
		trade.cancelAccepts();
		
		final Player otherPlayer = trade.getOtherPlayer(player);
		rules = new HashMap<>();
		if (trade.playerIsP1(player)) {
			rules.put(player.getId(), trade.getP1Rules());
			rules.put(otherPlayer.getId(), trade.getP2Rules());
		} else {
			rules.put(player.getId(), trade.getP2Rules());
			rules.put(otherPlayer.getId(), trade.getP1Rules());
		}
		
		responseMaps.addClientOnlyResponse(player, this);
		responseMaps.addClientOnlyResponse(otherPlayer, this);
	}

}
