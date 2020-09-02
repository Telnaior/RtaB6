package tel.discord.rtab.commands;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class StartCommand extends Command
{
	public StartCommand()
	{
		this.name = "forcestart";
		this.help = "starts the game immediately";
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
				game.timer.shutdownNow();
				game.timer = new ScheduledThreadPoolExecutor(1);
				game.startTheGameAlready();
				//We found the right channel, so
				return;
			}
		}
	}
}
