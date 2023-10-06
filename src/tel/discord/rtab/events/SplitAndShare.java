package tel.discord.rtab.events;

import tel.discord.rtab.GameController;

public class SplitAndShare implements EventSpace
{
	@Override
	public String getName()
	{
		return "Split & Share";
	}

	@Override
	public void execute(GameController game, int player)
	{
		if(!game.players.get(player).splitAndShare)
		{
			game.channel.sendMessage("It's a **Split & Share**, "
					+ "if you lose now you'll share approximately "
					+ String.format("$%,d",game.applyBankPercentMultiplier(game.players.get(player).money/50))
					+ " from your bank with each living player!").queue();
			game.players.get(player).splitAndShare = true;
		}
		else
		{
			game.channel.sendMessage("It's a **Split & Share**, but you already have one...").queue();
			try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			game.channel.sendMessage("Well then, how about we activate it~?").queue();
			game.players.get(player).blowUp(0,false);
		}
	}

}
