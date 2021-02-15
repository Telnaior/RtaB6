package tel.discord.rtab.commands.mod;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class LeaveGuildCommand extends Command
{
	public LeaveGuildCommand()
	{
		this.name = "leaveguild";
		this.help = "gets the bot out of a server I don't want it in";
        this.guildOnly = false;
        this.ownerCommand = true;
		this.hidden = true;
	}
	@Override
	protected void execute(CommandEvent event)
	{
		event.getJDA().getGuildById(event.getArgs()).leave().queue();
	}
}
