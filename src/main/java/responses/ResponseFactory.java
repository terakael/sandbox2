package responses;

public class ResponseFactory {	
	public static Response create(String action) {
		if (action == null)
			return new UnknownResponse();
		
		Response response;
		
		switch (action) {			
		case "logon":
			response = new LogonResponse();
			break;
		case "logoff":
			response = new LogoffResponse();
			break;
		case "move":
			response = new MoveResponse();
			break;
		case "message":
			response = new MessageResponse();
			break;
		case "addexp":
			response = new AddExpResponse();
			break;
		case "duel":
			response = new DuelResponse();
			break;
		case "trade":
			response = new TradeResponse();
			break;
		case "follow":
			response = new FollowResponse();
			break;
		case "inv":// fall through
		case "invmove":// fall through
		case "invadd":// fall through
		case "invdrop":// fall through
		case "invupdate":
			response = new InventoryUpdateResponse();
			break;
		case "equip":
			response = new EquipResponse();
			break;
		case "drop":
			response = new DropResponse();
			break;
		case "take":
			response = new TakeResponse();
			break;
		case "player_leave":
			response = new PlayerLeaveResponse();
			break;
		case "player_enter":
			response = new PlayerEnterResponse();
			break;
		case "player_update":
			response = new PlayerUpdateResponse();
			break;
		case "examine":
			response = new ExamineResponse();
			break;
		case "mine":
			response = new MineResponse();
			break;
		case "finish_mining":
			response = new FinishMiningResponse();
			break;
		case "use":
			response = new UseResponse();
			break;
		case "smith":
			response = new SmithResponse();
			break;
		case "attack":
			response = new AttackResponse();
			break;
		case "toggle_attack_style":
			response = new ToggleAttackStyleResponse();
			break;
		case "talk to":
			response = new TalkToResponse();
			break;
		case "eat":
			response = new EatResponse();
			break;
		case "drink":
			response = new DrinkResponse();
			break;
		case "next_dialogue":
			response = new NextDialogueResponse();
			break;
		case "select_dialogue_option":
			response = new SelectDialogueOptionResponse();
			break;
		case "shop":
			response = new ShopResponse();
			break;
		case "value":
			response = new ShopValueResponse();
			break;
		case "buy":
			response = new ShopBuyResponse();
			break;
		case "sell":
			response = new ShopSellResponse();
			break;
		case "close_shop":
			response = new CloseShopResponse();
			break;
		case "cancel_trade":
			response = new CancelTradeResponse();
			break;
		case "offer":
			response = new OfferResponse();
			break;
		case "rescind":
			response = new RescindResponse();
			break;
		case "accept_trade_offer":
			response = new AcceptTradeOfferResponse();
			break;
		case "catch":
			response = new CatchResponse();
			break;
		case "climb":
			response = new ClimbResponse();
			break;
		case "climb through":
			response = new ClimbThroughResponse();
			break;
		case "pick":
			response = new PickResponse();
			break;
		case "flower_deplete":
			response = new FlowerDepleteResponse();
			break;
		case "flower_respawn":
			response = new FlowerRespawnResponse();
			break;
		case "deposit":
			response = new DepositResponse();
			break;
		case "withdraw":
			response = new WithdrawResponse();
			break;
		case "show_stat_window":
			response = new ShowStatWindowResponse();
			break;
		case "fish":
			response = new FishResponse();
			break;
		case "enter":
			response = new EnterPortalResponse();
			break;
		case "toggle_prayer":
			response = new TogglePrayerResponse();
			break;
		case "pray at":
			response = new PrayAtResponse();
			break;
		case "bury":
			response = new BuryResponse();
			break;
		case "open":
			response = new OpenResponse();
			break;
		case "toggle_duel_rule":
			response = new ToggleDuelRuleResponse();
			break;
		case "chop":
			response = new ChopResponse();
			break;
		case "drink from":
			response = new DrinkFromResponse();
			break;
		case "construction":
			response = new ConstructionResponse();
			break;
		case "show_construction_materials":
			response = new ShowConstructionMaterialsResponse();
			break;
		case "storage_move":
			response = new StorageMoveResponse();
			break;
		case "repair":
			response = new RepairResponse();
			break;
		case "assemble":
			response = new AssembleResponse();
			break;
		case "empty":
			response = new EmptyResponse();
			break;
		case "check":
			response = new CheckResponse();
			break;
		case "loot":
			response = new LootResponse();
			break;
		case "throw":
			response = new ThrowResponse();
			break;
		case "task":
			response = new TaskResponse();
			break;
		case "cycle_base_animation":
			response = new CycleBaseAnimationResponse();
			break;
		case "save_base_animations":
			response = new SaveBaseAnimationsResponse();
			break;
		case "makeover":
			response = new MakeoverResponse();
			break;
		case "switch_artisan_shop_tab":
			response = new SwitchArtisanShopTabResponse();
			break;
		case "buy_artisan_stock":
			response = new BuyArtisanStockResponse();
			break;
		case "enhance_item":
			response = new EnhanceItemResponse();
			break;
		case "block_artisan_task":
			response = new BlockArtisanTaskResponse();
			break;
		case "unblock_artisan_task":
			response = new UnblockArtisanTaskResponse();
			break;
		case "skip_artisan_task":
			response = new SkipArtisanTaskResponse();
			break;
		case "show_smithing_skill_window":
			response = new ShowSmithingSkillWindowResponse();
			break;
		case "show_construction_skill_window":
			response = new ShowConstructionSkillWindowResponse();
			break;
		case "pick up":
			response = new PickUpResponse();
			break;
			
		case "buy_house":
			response = new BuyHouseResponse();
			break;
			
		case "sell_house":
			response = new SellHouseResponse();
			break;
			
		case "get_house_info":
			response = new GetHouseInfoResponse();
			break;
			
		case "build":
			response = new BuildResponse();
			break;
			
		case "board":
			response = new BoardResponse();
			break;
			
		case "show_ship_accessory_materials":
			response = new ShowShipAccessoryMaterialsResponse();
			break;
			
		case "storage":
			response = new OpenShipStorageResponse();
			break;
			
		case "cast net":
			response = new CastNetResponse();
			break;
			
		default:
			response = new UnknownResponse();
			break;
		}
		
		response.setAction(action);
		return response;
	}
}
