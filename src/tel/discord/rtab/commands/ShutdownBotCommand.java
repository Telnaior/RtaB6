package tel.discord.rtab.commands;

import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.examples.command.ShutdownCommand;

public class ShutdownBotCommand extends ShutdownCommand
{
	public ShutdownBotCommand()
	{
		this.hidden = true;
	}
	@Override
	protected void execute(CommandEvent event)
	{
		for(GameController game : RaceToABillionBot.game)
		{
			game.timer.purge();
			game.timer.shutdownNow();
		}
		super.execute(event);
	}
}