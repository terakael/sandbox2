package main.requests;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class RequestFactory {
	private static Gson gson = new Gson();
	private static Map<String, Class<? extends Request>> map = new HashMap<>();
	static {
		map.put("logon", LogonRequest.class);
		map.put("logoff", LogoffRequest.class);
		map.put("move", MoveRequest.class);
		map.put("message", MessageRequest.class);
		map.put("duel", DuelRequest.class);
		map.put("trade", TradeRequest.class);
		map.put("inv", InventoryUpdateRequest.class);
		map.put("invmove", InventoryMoveRequest.class);
		map.put("equip", EquipRequest.class);
		map.put("drop", DropRequest.class);
		map.put("take", TakeRequest.class);
		map.put("follow", FollowRequest.class);
		map.put("resources", ResourceRequest.class);
		map.put("examine", ExamineRequest.class);
		map.put("mine", MineRequest.class);
		map.put("use", UseRequest.class);
		map.put("smith", SmithRequest.class);
		map.put("attack", AttackRequest.class);
		map.put("toggle_attack_style", ToggleAttackStyleRequest.class);
		map.put("talk to", TalkToRequest.class);
		map.put("eat", EatRequest.class);
		map.put("dialogue", DialogueRequest.class);
		map.put("dialogue_option", DialogueOptionRequest.class);
		map.put("shop", ShopRequest.class);
		map.put("value", ShopValueRequest.class);
		map.put("buy", ShopBuyRequest.class);
		map.put("sell", ShopSellRequest.class);
		map.put("close_shop", CloseShopRequest.class);
		map.put("cancel_trade", CancelTradeRequest.class);
		map.put("offer", OfferRequest.class);
		map.put("rescind", RescindRequest.class);
		map.put("accept_trade_offer", AcceptTradeOfferRequest.class);
		map.put("drink", DrinkRequest.class);
		map.put("catch", CatchRequest.class);
		map.put("climb", ClimbRequest.class);
		map.put("pick", PickRequest.class);
		map.put("bank", BankRequest.class);
		map.put("deposit", BankDepositRequest.class);
		map.put("withdraw", BankWithdrawRequest.class);
		map.put("show_stat_window", ShowStatWindowRequest.class);
	}
	public static Request create(String action, String jsonText) {
		if (map.containsKey(action))
			return (Request) gson.fromJson(jsonText, map.get(action));
		return new UnknownRequest();
	}
	public static Request create(String action, int id) {
		Request request = new Request();
		request.setAction(action);
		request.setId(id);
		return request;
	}
}
