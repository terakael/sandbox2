package main.requests;

public abstract class MultiRequest extends Request {
	// a multi-request is a special subset of request that the user can send multiple of in the same tick
	// for example EquipRequest - you can equip multiple items within the same tick, otherwise its slow af to equip items
}
