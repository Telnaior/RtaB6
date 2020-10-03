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
					+ "if you lose now you'll give 2% of your total to each living player, approximately "
					+ String.format("$%,d!",game.players.get(player).money/50)).queue();
			game.players.get(player).splitAndShare = true;
		}
		else
		{
			game.channel.sendMessage("It's a **Split & Share**, but you already have one...").queue();
			try { Thread.sleep(3000); } catch (InterruptedException e) { e.printStackTrace(); }
			game.channel.sendMessage("Well then, how about we activate it~?").queue();
			game.players.get(player).blowUp(0,false);
		}
	}

}
