package tel.discord.rtab.bombs;

import tel.discord.rtab.GameController;
import tel.discord.rtab.MoneyMultipliersToUse;
import tel.discord.rtab.Player;
import tel.discord.rtab.PlayerStatus;

public class ReverseBomb implements Bomb
{
	public void explode(GameController game, int victim, int penalty)
	{
		game.channel.sendMessage("It goes **BOOM**...").queue();
		try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }
		game.channel.sendMessage(String.format("But it's a REVERSE bomb. $%,d awarded to living players!",Math.abs(penalty))).queue();
		game.players.get(victim).blowUp(0,false);
		for(Player nextPlayer : game.players)
			if(nextPlayer.status == PlayerStatus.ALIVE)
				nextPlayer.addMoney(-penalty,MoneyMultipliersToUse.BOOSTER_ONLY);
	}
}
