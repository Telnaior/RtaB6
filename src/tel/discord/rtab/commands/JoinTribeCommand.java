package tel.discord.rtab.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.json.JSONObject;
import org.json.JSONTokener;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.entities.UserSnowflake;

public class JoinTribeCommand extends Command
{
	public JoinTribeCommand()
	{
		this.name = "jointribe";
		this.help = "be assigned to a tribe (if in a server that allows this)";
	}
	@Override
	protected void execute(CommandEvent event)
	{
		String serverID = event.getGuild().getId();
		JSONObject tribeConfig;
		try
		{
			tribeConfig = new JSONObject(new JSONTokener(Files.newInputStream(Paths.get("guilds","tribes"+serverID+".json"))));
		}
		catch (IOException e)
		{
			return; //No tribes in this server, and I don't feel like responding
		}
		if(!tribeConfig.optBoolean("canJoin",false))
		{
			event.reply("You can't be automatically assigned to a tribe in this server.");
			return;
		}
		int tribes = tribeConfig.optInt("tribes");
		if(tribes < 1)
		{
			event.reply("Invalid tribe configuration");
			return;
		}
		//At this point we've established that we're allowed to do this
		int divisor = tribeConfig.optInt("divisor",1);
		long userID = event.getAuthor().getIdLong();
		int assignedTribe = (int)((userID/divisor) % tribes);
		event.reply(String.format("Welcome to the **%s** tribe!",
				tribeConfig.optJSONObject("names").optString(String.valueOf(assignedTribe))));
		String roleID = tribeConfig.optJSONObject("roles").optString(String.valueOf(assignedTribe));
		event.getGuild().addRoleToMember(UserSnowflake.fromId(userID),event.getGuild().getRoleById(roleID)).queue();
	}
}