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
		Game[] possibleGames = Game.values();
		ArrayList<Game> chosenGames = Board.generateSpaces(game.players.size(),1,Game.values());

		game.channel.sendMessage("It's **Minigames For All**! All players remaining receive " +
				"a copy of a randomly chosen minigame!").queue();
		
		
		for(int i = 0; i < game.players.size(); i++)
		{
			Player nextPlayer = game.players.get(i);
			Game chosenGame = chosenGames.get(i);
			
			if(nextPlayer.status == PlayerStatus.ALIVE)
			{
				game.players.get(player).games.add(chosenGame);
				game.players.get(player).games.sort(null);
				game.channel.sendMessage(nextPlayer.getSafeMention() +
						" receives a copy of **" + chosenGame.getName() + "**!").queue();
			}
		}
		
	}
}
