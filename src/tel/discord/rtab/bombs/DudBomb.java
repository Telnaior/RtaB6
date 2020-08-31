package tel.discord.rtab.bombs;

import tel.discord.rtab.GameController;

public class DudBomb implements Bomb
{
	public void explode(GameController game, int victim, int penalty)
	{
		game.channel.sendMessage("It goes _\\*fizzle*_.").queue();
		//That's it, no explosion here! Then again, no triforce either.
	}
}
