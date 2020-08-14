package main.requests;

import lombok.Getter;

@Getter
public class TogglePrayerRequest extends MultiRequest {
	private int prayerId;
}
