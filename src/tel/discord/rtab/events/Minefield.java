package tel.discord.rtab.events;

import tel.discord.rtab.GameController;

import static tel.discord.rtab.RaceToABillionBot.rng;


public class Minefield implements EventSpace
{
	@Override
	public String getName()
	{
		return "Minefield";
	}

	@Override
	public void execute(GameController game, int player)
	{
				game.channel.sendMessage("Oh no, it's a **Minefield**! Adding up to " + game.players.size() + " more bombs...").queue();
		for(int i=0; i<game.players.size(); i++)
			game.gameboard.addBomb(rng.nextInt(game.boardSize));
		game.starman = false;
	}

}
