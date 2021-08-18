package tel.discord.rtab.events;

import tel.discord.rtab.GameController;

public class InstantBillion implements EventSpace {

	@Override
	public String getName()
	{
		return "ONE BILLION DOLLARS";
	}

	@Override
	public void execute(GameController game, int player)
	{
		game.players.get(player).money = 1_000_000_000;
		game.channel.sendMessage("It's **ONE BILLION DOLLARS**!! CONGRATULATIONS!").queue();
		try { Thread.sleep(10_000); } catch (InterruptedException e) { e.printStackTrace(); }
	}

}
