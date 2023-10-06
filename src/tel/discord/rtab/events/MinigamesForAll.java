package tel.discord.rtab.events;

import tel.discord.rtab.GameController;
import tel.discord.rtab.Player;
import tel.discord.rtab.PlayerStatus;
import tel.discord.rtab.RtaBMath;
import tel.discord.rtab.board.Game;

public class MinigamesForAll implements EventSpace
{
	@Override
	public String getName()
	{
		return "Minigames for All";
	}
	
	@Override
	public void execute(GameController game, int player)
	{
		// Small chance in large games of the space mutating, capping at 10% in 16p
		// Or if starman, then make it dramatically more likely
		if (RtaBMath.random() * 100 < game.playersAlive - 6 || (game.starman && RtaBMath.random() * 10 < game.playersAlive - 4))
		{
			game.channel.sendMessage("It's **Minigame For... One?!**").queue();
			Game chosenGame = game.players.get(player).generateEventMinigame();
			
			for (int i = 0; i < game.playersAlive; i++)
			{
				game.players.get(player).games.add(chosenGame);
				game.channel.sendMessage(game.players.get(player).getSafeMention() 
						+ " receives a copy of **" + chosenGame.getName() + "**!").queue();
				try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			}
			game.players.get(player).games.sort(null);
			game.players.get(player).minigameLock = true;
			game.channel.sendMessage("Minigame Lock applied to "+game.players.get(player).getSafeMention()+".").queue();
		}
		else
		{
			game.channel.sendMessage("It's **Minigames For All**! All players remaining receive a minigame!").queue();
			for(int i = 0; i < game.players.size(); i++)
			{
				Player nextPlayer = game.players.get(i);
				if(nextPlayer.status == PlayerStatus.ALIVE)
				{
					Game chosenGame = nextPlayer.generateEventMinigame();
					game.players.get(i).games.add(chosenGame);
					game.players.get(i).games.sort(null);
					game.channel.sendMessage(nextPlayer.getSafeMention() +
							" receives a copy of **" + chosenGame.getName() + "**!").queue();
					try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
				}
			}
		}
	}
}
