package tel.discord.rtab.events;

import tel.discord.rtab.GameController;
import tel.discord.rtab.MoneyMultipliersToUse;
import tel.discord.rtab.Player;
import tel.discord.rtab.PlayerStatus;

import java.security.SecureRandom;

public class CashForAll implements EventSpace
{
	@Override
	public String getName()
	{
		return "Cash for All";
	}

	@Override
	public void execute(GameController game, int player)
	{
		SecureRandom r = new SecureRandom();
		//Give each living player a value based on what fraction of the original playercount is still in
		int cashGiven = game.applyBaseMultiplier(50_000 + r.nextInt(50_001)) * game.players.size() / game.playersAlive;
		for(Player nextPlayer : game.players)
		{
			if(nextPlayer.status == PlayerStatus.ALIVE)
			{
				nextPlayer.addMoney(cashGiven, MoneyMultipliersToUse.BOOSTER_ONLY);
			}
		}
		game.channel.sendMessage(String.format("It's **Cash For All**! All players remaining receive **$%,d**!",cashGiven)).queue();
	}
	
}
