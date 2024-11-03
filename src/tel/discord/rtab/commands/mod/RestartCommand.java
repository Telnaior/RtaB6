package tel.discord.rtab.commands.mod;

import tel.discord.rtab.RaceToABillionBot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class RestartCommand extends Command
{
	public RestartCommand()
	{
        this.name = "restart";
        this.help = "reboots the bot (and server)";
        this.guildOnly = false;
        this.ownerCommand = true;
		this.hidden = true;
	}
	@Override
	protected void execute(CommandEvent event)
	{
		RaceToABillionBot.shutdown(true);
	}
}