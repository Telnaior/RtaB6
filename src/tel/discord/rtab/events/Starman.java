package tel.discord.rtab.events;

import tel.discord.rtab.Achievement;
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
		int bombsDestroyed = game.detonateBombs(true);
		if(bombsDestroyed > game.players.size())
			Achievement.STAR_MINEFIELD.check(game.players.get(player));
		game.starman = true;
	}

}
