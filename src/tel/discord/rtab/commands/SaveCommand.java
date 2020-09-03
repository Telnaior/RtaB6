package tel.discord.rtab.commands;

import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class SaveCommand extends Command
{
	public SaveCommand()
	{
		this.name = "forcesave";
		this.help = "immediately saves the game and ends it where it stands";
		this.hidden = true;
		this.requiredRole = "Mod";
	}
	@Override
	protected void execute(CommandEvent event)
	{
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.equals(event.getChannel()))
			{
				game.runFinalEndGameTasks();
				return;
			}
		}
	}
}