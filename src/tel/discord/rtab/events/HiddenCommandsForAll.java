package tel.discord.rtab.events;

import tel.discord.rtab.GameController;
import tel.discord.rtab.Player;
import tel.discord.rtab.PlayerStatus;

public class HiddenCommandsForAll implements EventSpace
{
	@Override
	public String getName()
	{
		return "Hidden Commands for All";
	}

	@Override
	public void execute(GameController game, int player)
	{
		game.channel.sendMessage("It's ||**Commands for All**||!").queue();
		for(Player nextPlayer : game.players)
			if(nextPlayer.status == PlayerStatus.ALIVE)
				nextPlayer.awardHiddenCommand();
	}

}
