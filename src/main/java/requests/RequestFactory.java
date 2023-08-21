package requests;

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
		map.put("next_dialogue", NextDialogueRequest.class);
		map.put("select_dialogue_option", SelectDialogueOptionRequest.class);
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
		map.put("climb through", ClimbThroughRequest.class);
		map.put("pick", PickRequest.class);
		map.put("deposit", DepositRequest.class);
		map.put("withdraw", WithdrawRequest.class);
		map.put("show_stat_window", ShowStatWindowRequest.class);
		map.put("fish", FishRequest.class);
		map.put("enter", EnterPortalRequest.class);
		map.put("toggle_prayer", TogglePrayerRequest.class);
		map.put("pray at", PrayAtRequest.class);
		map.put("bury", BuryRequest.class);
		map.put("open", OpenRequest.class);
		map.put("toggle_duel_rule", ToggleDuelRuleRequest.class);
		map.put("chop", ChopRequest.class);
		map.put("construction", ConstructionRequest.class);
		map.put("drink from", DrinkFromRequest.class);
		map.put("show_construction_materials", ShowConstructionMaterialsRequest.class);
		map.put("storage_move", StorageMoveRequest.class);
		map.put("repair", RepairRequest.class);
		map.put("assemble", AssembleRequest.class);
		map.put("empty", EmptyRequest.class);
		map.put("check", CheckRequest.class);
		map.put("loot", LootRequest.class);
		map.put("throw", ThrowRequest.class);
		map.put("task", TaskRequest.class);
		map.put("show_base_animations_window", ShowBaseAnimationsWindowRequest.class);
		map.put("cycle_base_animation", CycleBaseAnimationRequest.class);
		map.put("save_base_animations", SaveBaseAnimationsRequest.class);
		map.put("makeover", MakeoverRequest.class);
		map.put("switch_artisan_shop_tab", SwitchArtisanShopTabRequest.class);
		map.put("buy_artisan_stock", BuyArtisanStockRequest.class);
		map.put("enhance_item", EnhanceItemRequest.class);
		map.put("block_artisan_task", BlockArtisanTaskRequest.class);
		map.put("unblock_artisan_task", UnblockArtisanTaskRequest.class);
		map.put("skip_artisan_task", SkipArtisanTaskRequest.class);
		map.put("show_smithing_skill_window", ShowSmithingSkillWindowRequest.class);
		map.put("show_construction_skill_window", ShowConstructionSkillWindowRequest.class);
		map.put("pick up", PickUpRequest.class);
		map.put("buy_house", BuyHouseRequest.class);
		map.put("sell_house", SellHouseRequest.class);
		map.put("get_house_info", GetHouseInfoRequest.class);
		map.put("build", BuildRequest.class);
		map.put("board", BoardRequest.class);
		map.put("show_ship_accessory_materials", ShowShipAccessoryMaterialsRequest.class);
		map.put("storage", OpenShipStorageRequest.class);
		map.put("cast net", CastNetRequest.class);
	}
	public static Request create(String action, String jsonText) {
		if (map.containsKey(action))
			return (Request) gson.fromJson(jsonText, map.get(action));
		return new UnknownRequest();
	}
	public static Request create(String action, int id) {
		Request request = new DummyRequest();
		request.setAction(action);
		return request;
	}
}
