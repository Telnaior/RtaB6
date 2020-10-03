package tel.discord.rtab.events;

import tel.discord.rtab.GameController;

public class SuperJoker implements EventSpace
{
	@Override
	public String getName()
	{
		return "Midas Touch";
	}

	@Override
	public void execute(GameController game, int player)
	{
		game.channel.sendMessage("You found the **MIDAS TOUCH**! "
				+ "Every space you pick for the rest of the round (even bombs) will be converted to cash, "
				+ "but you won't receive a win bonus at the end.").queue();
		game.players.get(player).jokers = -1;
	}

}
