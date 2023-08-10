package tel.discord.rtab.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class RegularCommand extends Command {
	public RegularCommand()
	{
		this.name = "regular";
		this.aliases = new String[] {"regulars"};
		this.help = "sends a ping in the host server to everyone opted-in to game invites";
		this.cooldown = 900;
		this.cooldownScope = CooldownScope.GUILD;
		this.guildOnly = true;
	}
	@Override
	protected void execute(CommandEvent event) {
		if(!event.getGuild().getId().equals("466545561743654922"))
			return;

		StringBuilder output = new StringBuilder();
		output.append(event.getJDA().getRoleById("504510238829969408").getAsMention());
		
		String mention = event.getArgs();
		if(mention.length() > 0)
		{
			output.append(" ");
			output.append(mention);
		}

		event.reply(output.toString());
	}
}
