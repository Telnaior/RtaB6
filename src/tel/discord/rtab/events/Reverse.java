package tel.discord.rtab.events;

import tel.discord.rtab.GameController;

public class Reverse implements EventSpace
{
	@Override
	public String getName()
	{
		return "Reverse";
	}

	@Override
	public void execute(GameController game, int player)
	{
		if(game.playersAlive > 2)
		{
			game.channel.sendMessage("It's a **Reverse**!").queue();
			game.reverse = !game.reverse;
		}
		//If 2p, treat them as skips instead
		else
		{
			game.channel.sendMessage("It's a **Skip Turn**!").queue();
			game.repeatTurn += game.repeatTurn == 0 ? 1 : -1; //If they're repeating turns remove 1 from the repeat count, otherwise make them repeat
		}
	}

}
