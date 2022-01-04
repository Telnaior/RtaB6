package tel.discord.rtab.events;

import tel.discord.rtab.GameController;
import tel.discord.rtab.board.SpaceType;

public class Lockdown implements EventSpace
{
	@Override
	public String getName()
	{
		return "Triple Deal Lockdown";
	}

	@Override
	public void execute(GameController game, int player)
	{
		game.channel.sendMessage("It's the **Triple Deal Lockdown**, "
				+ "all the boost, games, and events on the board have been converted to cash... "
				+ "and all cash has been tripled!").queue();
		game.boardMultiplier *= 3;
		for(int i=0; i<game.boardSize; i++)
		{
			//Bombs and blammos aren't affected
			if(game.gameboard.getType(i) != SpaceType.BLAMMO && !game.gameboard.getType(i).isBomb())
				game.gameboard.changeType(i,SpaceType.CASH);
		}
	}

}
