package tel.discord.rtab.events;

import java.util.LinkedList;

import tel.discord.rtab.GameController;
import tel.discord.rtab.MoneyMultipliersToUse;
import tel.discord.rtab.board.BombType;

public class CursedBomb implements EventSpace
{
	@Override
	public String getName()
	{
		return "CURSED BOMB";
	}

	@Override
	public void execute(GameController game, int player)
	{
		if(game.players.get(player).cursed)
		{
			//If you're cursed, you go boom. (unless you have a joker)
			game.channel.sendMessage("It's a **CURSED BOMB**.").queue();
			game.awardBomb(player, BombType.CURSED);
		}
		else
		{
			game.channel.sendMessage("It's a **CURSED BOMB**, but you aren't cursed...").queue();
			try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			//Get a list of cursed players
			LinkedList<Integer> cursedPlayers = new LinkedList<>();
			for(int i=0; i<game.players.size(); i++)
				if(game.players.get(i).cursed)
					cursedPlayers.add(i);
			//Vary message based on how manny players are cursed)
			switch(cursedPlayers.size())
			{
				case 0 -> {
					game.channel.sendMessage("But no one else *is* cursed either.").queue();
					return; //no one's cursed for some reason, so abort
				}
				case 1 -> {
					game.channel.sendMessage("So you get to steal from the cursed player!").queue();
				}
				case 2 -> {
					game.channel.sendMessage("So you get to steal from everyone who is cursed!").queue();
				}
			}
			for(int next : cursedPlayers)
			{
				try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
				//Steal a bomb penalty from the cursed player (and use the cursed player's booster, NOT the triggering player)
				int originalTheftAmount = game.calculateBombPenalty(next) * -4;
				int theftAmount = game.players.get(next).calculateBoostedAmount(originalTheftAmount, MoneyMultipliersToUse.BOOSTER_ONLY);
				game.channel.sendMessage(String.format("**$%,d** stolen from %s.", originalTheftAmount, game.players.get(next).getSafeMention())).queue();
				game.players.get(next).addMoney(-1 * theftAmount, MoneyMultipliersToUse.NOTHING);
				game.players.get(player).addMoney(theftAmount, MoneyMultipliersToUse.NOTHING);
				if(theftAmount != originalTheftAmount)
					game.channel.sendMessage(String.format("...which gets boosted to **$%,d**!", theftAmount)).queue();
			}
		}
	}
}
