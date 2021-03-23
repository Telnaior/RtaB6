package tel.discord.rtab.events;

import tel.discord.rtab.GameController;

public class Jackpot implements EventSpace
{
	@Override
	public String getName()
	{
		return "Jackpot";
	}

	@Override
	public void execute(GameController game, int player)
	{
		int jackpotAmount = game.spacesLeft+1;
		game.channel.sendMessage(String.format("You found the $%,d **JACKPOT**, "
				+ "win the round to claim it!", game.applyBaseMultiplier(1_000_000*jackpotAmount))).queue();
		game.players.get(player).jackpot += jackpotAmount;
	}

}
