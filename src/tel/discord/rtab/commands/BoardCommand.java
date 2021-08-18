package tel.discord.rtab.commands;

import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;
import tel.discord.rtab.GameStatus;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class BoardCommand extends Command
{
	public BoardCommand()
	{
		this.name = "board";
		this.aliases = new String[]{"table","status", "totals", "total"};
		this.help = "displays the current board (in-game)";
	}
	
	@Override
	protected void execute(CommandEvent event) {
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.equals(event.getChannel()))
			{
				if(game.gameStatus == GameStatus.SIGNUPS_OPEN)
				{
					//No board to display if the game isn't running!
					event.reply("No game currently running.");
				}
				else
				{	
					game.displayBoardAndStatus(game.gameStatus != GameStatus.END_GAME, false);
				}
				//We found the right channel, so
				return;
			}
		}
	}

}
