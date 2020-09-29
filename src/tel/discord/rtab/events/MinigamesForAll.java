package tel.discord.rtab.events;

import java.util.ArrayList;

import tel.discord.rtab.GameController;
import tel.discord.rtab.Player;
import tel.discord.rtab.PlayerStatus;
import tel.discord.rtab.board.Board;
import tel.discord.rtab.board.Game;

public class MinigamesForAll implements EventSpace
{
	@Override
	public void execute(GameController game, int player)
	{
		// 1% chance that Minigames for All becomes Multiple Copies of a Minigame for One
		if (Math.random() < 0.01) {
			Game chosenGame = Board.generateSpaces(1,1,Game.values()).get(0);
			game.channel.sendMessage("It's a minigame, **" + chosenGame + "**!").queue();
			game.channel.sendMessage("**" + game.players.size() + " copies** of it, as a matter of fact :wink:").queue();
			
			for (int i = 0; i < game.players.size(); i++)
				game.players.get(player).games.add(chosenGame);
			game.players.get(player).games.sort(null);
		} else {
			game.channel.sendMessage("It's **Minigames For All**! All players remaining receive " +
					"a copy of a randomly chosen minigame!").queue();
			
			ArrayList<Game> chosenGames = Board.generateSpaces(game.players.size(),1,Game.values());
		
			for(int i = 0; i < game.players.size(); i++) {
				Player nextPlayer = game.players.get(i);
				Game chosenGame = chosenGames.get(i);
			
				if(nextPlayer.status == PlayerStatus.ALIVE) {
					game.players.get(i).games.add(chosenGame);
					game.players.get(i).games.sort(null);
					game.channel.sendMessage(nextPlayer.getSafeMention() +
							" receives a copy of **" + chosenGame.getName() + "**!").queue();
				}
			}
		}
	}
}
