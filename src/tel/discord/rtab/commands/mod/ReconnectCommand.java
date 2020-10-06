package tel.discord.rtab.commands.mod;

import tel.discord.rtab.RaceToABillionBot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class ReconnectCommand extends Command
{
	
	public ReconnectCommand()
	{
		this.name = "reconnect";
		this.help = "reconnects the bot to its game channels";
		this.ownerCommand = true;
		this.hidden = true;
	}
	
	@Override
	protected void execute(CommandEvent event)
	{	
		RaceToABillionBot.scanGuilds();
	}
	
}