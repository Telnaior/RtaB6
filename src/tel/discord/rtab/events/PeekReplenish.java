package tel.discord.rtab.events;

import tel.discord.rtab.GameController;

public class PeekReplenish implements EventSpace
{
	@Override
	public String getName()
	{
		return "Extra Peek";
	}

	@Override
	public void execute(GameController game, int player)
	{
		game.channel.sendMessage("It's an **Extra Peek**! Use it wisely!").queue();
		game.players.get(player).peeks ++;
	}

}
