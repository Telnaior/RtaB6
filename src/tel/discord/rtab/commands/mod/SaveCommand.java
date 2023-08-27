package tel.discord.rtab.commands.mod;

import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.Permission;

public class SaveCommand extends Command
{
	public SaveCommand()
	{
		this.name = "forcesave";
		this.help = "immediately saves the game and ends it where it stands";
		this.hidden = true;
		this.userPermissions = new Permission[] {Permission.BAN_MEMBERS};
	}
	@Override
	protected void execute(CommandEvent event)
	{
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.getId().equals(event.getChannel().getId()))
			{
				game.runFinalEndGameTasks();
				return;
			}
		}
	}
}