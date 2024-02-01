package tel.discord.rtab.bombs;

import tel.discord.rtab.GameController;
import tel.discord.rtab.board.Game;

public class LoserWheelBomb implements Bomb
{
	public void explode(GameController game, int victim, int penalty)
	{
		//If they're in newbie protection, protect them from the loser wheel
		if(game.players.get(victim).newbieProtection > 0)
			Bomb.super.explode(game, victim, penalty);
		else
		{
			game.channel.sendMessage("It goes **BOOM**...").queue();
			try { Thread.sleep(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			game.channel.sendMessage("with a penalty to be determined later.").queue();
			game.awardGame(victim, Game.LOSER_WHEEL);
			game.players.get(victim).blowUp(0,false);
		}
	}
}
