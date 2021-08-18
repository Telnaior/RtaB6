package tel.discord.rtab.events;

import tel.discord.rtab.GameController;

public class InstantAnnuity implements EventSpace
{

	@Override
	public String getName()
	{
		return "Instant Annuity";
	}

	@Override
	public void execute(GameController game, int player)
	{
		int annuityAmount = (int)Math.pow((Math.random()*20)+15,4); //Similar to mystery money but with a bigger minimum
		annuityAmount = game.applyBaseMultiplier(annuityAmount);
		game.channel.sendMessage("It's an **Instant Annuity**!").queue();
		try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
		game.channel.sendMessage(String.format("You'll be receiving **$%,d** per turn for the rest of the season!", annuityAmount)).queue();
		int boostedAmount = game.players.get(player).addAnnuity(annuityAmount, -1);
		if(boostedAmount != annuityAmount)
			game.channel.sendMessage(String.format("...which gets boosted to **$%,d**!",boostedAmount)).queue();
	}

}
