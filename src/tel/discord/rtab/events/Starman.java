package tel.discord.rtab.events;

import tel.discord.rtab.GameController;

public class Starman implements EventSpace
{
	@Override
	public String getName()
	{
		return "Starman";
	}

	@Override
	public void execute(GameController game, int player)
	{
		game.channel.sendMessage("Hooray, it's a **Starman**, here to destroy all the bombs!").queue();
		game.detonateBombs(true);
		game.starman = true;
	}

}
