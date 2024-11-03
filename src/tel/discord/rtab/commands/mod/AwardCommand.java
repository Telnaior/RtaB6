package tel.discord.rtab.commands.mod;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import tel.discord.rtab.Achievement;

public class AwardCommand extends Command
{
	public AwardCommand()
	{
		this.name = "award";
		this.help = "manually award an achievement to a player";
		this.hidden = true;
		this.userPermissions = new Permission[] {Permission.MANAGE_SERVER};
	}
	@Override
	protected void execute(CommandEvent event)
	{
		Member awardee = event.getMessage().getMentions().getMembers().get(0);
		Achievement achievement = Achievement.valueOf(event.getArgs().split(" ")[1]);
		achievement.award(awardee.getId(), awardee.getEffectiveName(), event.getTextChannel());
	}

}
