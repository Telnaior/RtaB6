package tel.discord.rtab.bombs;

import tel.discord.rtab.GameController;

public class CursedBomb implements Bomb
{
	@Override
	public void explode(GameController game, int victim, int penalty)
	{
		//Basically a normal bomb, but with an extra 4x penalty (what is it with bombs and penalty multipliers all being x4?)
		Bomb.super.explode(game, victim, 4*penalty);
	}
}
