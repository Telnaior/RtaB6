package tel.discord.rtab.events;

import tel.discord.rtab.GameController;

public class StreakBonus implements EventSpace
{
	@Override
	public String getName()
	{
		return "Streak Bonus";
	}

	@Override
	public void execute(GameController game, int player)
	{
		//Start with 1.0 and add another 0.0-1.0 randomly
		int streakAwarded = 10 + (int) (Math.random() * 11);
		int newStreak = game.players.get(player).winstreak + streakAwarded;
		game.players.get(player).addWinstreak(streakAwarded);
		game.channel.sendMessage(String.format("It's a **+%1$d.%2$d Streak Bonus**, raising you to x%3$d.%4$d!",
				streakAwarded/10, streakAwarded%10, newStreak/10, newStreak%10)).queue();
	}

}
