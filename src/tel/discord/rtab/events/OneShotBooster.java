package tel.discord.rtab.events;

import tel.discord.rtab.GameController;
import tel.discord.rtab.Player;

public class OneShotBooster implements EventSpace
{
	int multiplier;
	public OneShotBooster(int multiplier)
	{
		this.multiplier = multiplier;
	}
	
	@Override
	public String getName()
	{
		return switch(multiplier)
		{
		case 10 -> "Times Ten";
		case 4 -> "Quad Damage";
		default -> "Oneshot Booster";
		};
	}

	@Override
	public void execute(GameController game, int player)
	{
		if(game.players.get(player).oneshotBooster == 1)
		{
			game.channel.sendMessage(String.format("It's **%s**. The next booster-affected cash you gain or lose will be multiplied by %d!"
					, getName(), multiplier)).queue();
			game.players.get(player).oneshotBooster = multiplier;
		}
		else
		{ //OneShot for All? ...AllShot?? idk I don't think the world machine's gonna like this
			game.channel.sendMessage(String.format("It's **%s**, but you already have it... then it'll be for everyone else!"
					, getName())).queue();
			for(Player next : game.players)
				next.oneshotBooster = multiplier;
		}
	}

}
