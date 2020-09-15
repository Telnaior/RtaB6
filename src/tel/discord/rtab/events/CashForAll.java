package tel.discord.rtab.events;

import tel.discord.rtab.GameController;

public class CashForAll implements EventSpace
{

	@Override
	public void execute(GameController game, int player)
	{
		//Give each living player from $100,000 to $250,000 in a random $50,000 increment.
		int cashGiven = game.boardMultiplier * (100_000 + (50_000 * (int) (Math.random() * 4)));
		for(Player nextPlayer : game.players)
		{
			if(nextPlayer.status == PlayerStatus.ALIVE)
			{
				game.players.get(player).addMoney(cashGiven, MoneyMultipliersToUse.BOOSTER_ONLY);
			}
		}
		game.channel.sendMessage(String.format("It's **Cash For All**! All players remaining receive **$%,d**!",cashGiven)).queue();
	}
	
}
