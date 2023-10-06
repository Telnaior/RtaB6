package tel.discord.rtab.events;

import tel.discord.rtab.GameController;
import tel.discord.rtab.RtaBMath;

public class BoostCharger implements EventSpace
{
	@Override
	public String getName()
	{
		return "Boost Charger";
	}

	@Override
	public void execute(GameController game, int player)
	{
		int boostAmount = (int)(RtaBMath.random() * 6 + 5);
		//Occasionally get big
		if(RtaBMath.random() < 0.2)
			boostAmount += (int)(RtaBMath.random() * 10 + 1);
		game.players.get(player).boostCharge += boostAmount;
		game.channel.sendMessage("It's a **Boost Charger**, "
				+ String.format("you'll gain %d%% boost every turn until your next loss!",boostAmount)).queue();
	}
}
