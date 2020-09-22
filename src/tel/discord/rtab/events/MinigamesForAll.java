package tel.discord.rtab.events;

import java.util.ArrayList;
import java.util.Comparator;

import tel.discord.rtab.GameController;
import tel.discord.rtab.Player;
import tel.discord.rtab.PlayerStatus;
import tel.discord.rtab.board.Board;
import tel.discord.rtab.board.Game;

public class MinigamesForAll implements EventSpace {
	@Override
	public void execute(GameController game, int player) {
		Game[] possibleGames = Game.values();
		
		/* 1% chance that Minigames for All becomes All Minigames for One
		 * 
		 * TODO: If this triggers in a very large game, it's possible Discord's
		 * character limit will break it--find that point and see what can/should be
		 * done to prevent it.
		 */
		if (Math.random() < 0.01) {
			ArrayList<String> gameNames = new ArrayList<>(possibleGames.length);
			
			game.channel.sendMessage("Hoo hoo hoo, lucky you! :four_leaf_clover: It's " + 
					"**Every Minigame in the Rotation**!").queue();
			
			for (int i = 0; i < possibleGames.length; i++) {
				Game thisGame = possibleGames[i];
				
				if (thisGame.getWeight(1) > 0) {
					game.players.get(player).games.add(thisGame);
					gameNames.add(thisGame.getName());
				}
			}
			gameNames.trimToSize();
			gameNames.sort(Comparator.comparing(String::toString));
			game.players.get(i).games.sort(null);
			
			String message = "That's ";
			for (int i = 0; i < gameNames.size(); i++) {
				message += "**" + gameNames.get(i) + "**";
				if (i + 2 == gameNames.size())
					message += " and ";
				else message += ", ";
			}
			message += "and you could play each and every one of them!";
			game.channel.sendMessage(message).queue();
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
