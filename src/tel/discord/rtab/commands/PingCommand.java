package tel.discord.rtab.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class PingCommand extends Command
{
    public PingCommand()
    {
        this.name = "ping";
        this.help = "checks the bot's ping";
        this.guildOnly = false;
    }
	
	@Override
	protected void execute(CommandEvent event)
	{
		event.getJDA().getRestPing().queue(
				(time) ->
				event.reply(String.format("Ping: %d ms", time))
				);
	}

}
