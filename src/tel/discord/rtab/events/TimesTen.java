package tel.discord.rtab.events;

import tel.discord.rtab.GameController;
import tel.discord.rtab.Player;

public class TimesTen implements EventSpace
{
	@Override
	public String getName()
	{
		return "Times Ten";
	}

	@Override
	public void execute(GameController game, int player)
	{
		//This check shouldn't be needed right now, but in case we change things later
		if(game.players.get(player).oneshotBooster == 1)
		{
			game.channel.sendMessage("It's **Times Ten**. The next time you gain or lose boosted cash, it will be multiplied by ten!").queue();
			game.players.get(player).oneshotBooster = 10;
		}
		else
		{
			game.channel.sendMessage("It's **Times Ten**, but you already have it... then it'll be for everyone else!").queue();
			for(Player next : game.players)
				next.oneshotBooster = 10;
		}
	}

}
