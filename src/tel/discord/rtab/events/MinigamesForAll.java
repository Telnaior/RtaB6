package tel.discord.rtab.events;

import tel.discord.rtab.GameController;
import tel.discord.rtab.Player;
import tel.discord.rtab.PlayerStatus;
import tel.discord.rtab.board.Game;

public class MinigamesForAll implements EventSpace
{
	@Override
	public void execute(GameController game, int player)
	{
		Game[] possibleGames = Game.values();
		Game chosenGame;
		
		// Randomly select a minigame...
		int[] cumulativeWeights = new int[possibleGames.length];
		int totalWeight = 0;
		for(int i=0; i<possibleGames.length; i++)
		{
			totalWeight += possibleGames[i].getWeight(0); // minigames don't care about player count
			cumulativeWeights[i] = totalWeight;
		}
		double random = Math.random() * totalWeight;
		int search=0;
		while(cumulativeWeights[search] < random)
			search++;
		chosenGame = possibleGames[search];
		
		// ...and award it to everyone still in the game!
		for(Player nextPlayer : game.players)
		{
			if(nextPlayer.status == PlayerStatus.ALIVE)
				{
					game.players.get(player).games.add(chosenGame);
					game.players.get(player).games.sort(null);
				}
			}
		game.channel.sendMessage("It's **Minigames For All**! All players remaining receive " +
				"a copy of **" + chosenGame.getName() + "**!").queue();
	}
}
