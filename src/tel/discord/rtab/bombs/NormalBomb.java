package tel.discord.rtab.bombs;

import tel.discord.rtab.GameController;

public class NormalBomb implements Bomb
{
	@Override
	public void explode(GameController game, int victim, int penalty)
	{
		//A normal bomb uses the default code. This class could be completely blank, it just seemed sensible to spell it out.
		Bomb.super.explode(game, victim, penalty);
	}
}
