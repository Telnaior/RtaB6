package tel.discord.rtab.events;

import tel.discord.rtab.GameController;

public class DoubleDeal implements EventSpace
{
	@Override
	public String getName()
	{
		return "Double Deal";
	}

	@Override
	public void execute(GameController game, int player)
	{
		game.channel.sendMessage("It's a **Double Deal**, all cash left on the board is doubled in value!").queue();
		game.boardMultiplier *= 2;
	}

}
