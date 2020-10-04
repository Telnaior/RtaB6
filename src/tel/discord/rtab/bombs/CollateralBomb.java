package tel.discord.rtab.bombs;

import tel.discord.rtab.GameController;

public class CollateralBomb implements Bomb
{
	public void explode(GameController game, int victim, int penalty)
	{
		int detonationChance = 5;
		for(int i=0; i<game.boardSize; i++)
		{
			if(!game.pickedSpaces[i] && Math.random()*detonationChance < 1)
			{
				game.pickedSpaces[i] = true;
				game.spacesLeft --;
				detonationChance ++; //So the next one becomes less likely, and to count spaces destroyed
			}
		}
		//If detonation chance is still 5, no spaces were destroyed so let's revert to default bomb code
		if(detonationChance == 5)
			Bomb.super.explode(game, victim, penalty);
		else
		{
			game.channel.sendMessage("It goes **KABLAM**! "
					+ String.format("$%,d lost as penalty, and %d space"+(detonationChance!=6?"s":"")+" destroyed."
							,Math.abs(penalty),detonationChance-5)).queue();
			StringBuilder extraResult = game.players.get(victim).blowUp(penalty,false);
			if(extraResult != null)
				game.channel.sendMessage(extraResult).queue();
		}
	}
}
