package tel.discord.rtab.events;

import tel.discord.rtab.GameController;

import java.security.SecureRandom;

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
		SecureRandom r = new SecureRandom();
		int boostAmount = r.nextInt(6) + 5;
		//Occasionally get big
		if(Math.random() < 0.2)
			boostAmount += r.nextInt(10) + 1;
		game.players.get(player).boostCharge += boostAmount;
		game.channel.sendMessage("It's a **Boost Charger**, "
				+ String.format("you'll gain %d%% boost every turn until your next loss!",boostAmount)).queue();
	}
}
