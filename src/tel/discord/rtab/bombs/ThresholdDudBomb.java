package tel.discord.rtab.bombs;

import tel.discord.rtab.Achievement;
import tel.discord.rtab.GameController;

public class ThresholdDudBomb implements Bomb
{
	public void explode(GameController game, int victim, int penalty)
	{
		if(game.players.get(victim).threshold)
			game.channel.sendMessage("It goes _\\*fizzle*_.").queue();
		else
		{
			game.players.get(victim).threshold = true;
			game.channel.sendMessage("It goes _\\*fizzle*_... leaving behind a THRESHOLD SITUATION.").queue();
		}
		
		//This one counts for the last hope achievement too
		if(game.spacesLeft == 0)
			Achievement.LUCKY_WIN.check(game.players.get(victim));
	}
}
