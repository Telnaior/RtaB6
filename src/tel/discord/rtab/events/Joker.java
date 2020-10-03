package tel.discord.rtab.events;

import tel.discord.rtab.GameController;

public class Joker implements EventSpace
{
	@Override
	public String getName()
	{
		return "Joker";
	}

	@Override
	public void execute(GameController game, int player)
	{
		//This check shouldn't be needed right now, but in case we change things later
		if(game.players.get(player).jokers >= 0)
		{
			game.channel.sendMessage("Congratulations, you found a **Joker**, protecting you from a single bomb!").queue();
			game.players.get(player).jokers ++;
		}
		else
		{
			game.channel.sendMessage("You found a **Joker**, but you don't need it.").queue();
		}
	}

}
