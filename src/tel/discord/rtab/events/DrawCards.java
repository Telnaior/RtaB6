package tel.discord.rtab.events;

import tel.discord.rtab.GameController;

public class DrawCards implements EventSpace
{
	int cardsToDraw;
	public DrawCards(int cardsToDraw)
	{
		this.cardsToDraw = cardsToDraw;
	}

	@Override
	public void execute(GameController game, int player)
	{
		game.advanceTurn(false);
		if(game.repeatTurn > 0)
		{
			game.channel.sendMessage(String.format("It's another **Draw %d**, and that stacks up to make %d turns for %s!",
					cardsToDraw, game.repeatTurn+cardsToDraw,game.players.get(game.currentTurn).getName())).queue();
		}
		else
		{
			game.channel.sendMessage(String.format("It's a **Draw %1$d**, %2$s needs to take %1$d turns in a row!",
					cardsToDraw, game.players.get(game.currentTurn).getName())).queue();
		}
		game.firstPick = true;
		game.repeatTurn += cardsToDraw;
	}

}
