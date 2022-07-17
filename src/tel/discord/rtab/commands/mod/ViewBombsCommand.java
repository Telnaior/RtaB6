package tel.discord.rtab.commands.mod;

import tel.discord.rtab.GameController;
import tel.discord.rtab.Player;
import tel.discord.rtab.RaceToABillionBot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.Permission;

public class ViewBombsCommand extends Command {
	public ViewBombsCommand()
	{
		this.name = "viewbombs";
		this.help = "See who placed their bomb where (can only use when not in game)";
		this.hidden = true;
		this.userPermissions = new Permission[] {Permission.BAN_MEMBERS};
	}
	@Override
	protected void execute(CommandEvent event) {
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.equals(event.getChannel()))
			{
				if(game.findPlayerInGame(event.getAuthor().getId()) != -1)
					event.reply("You can't view bombs for a game you're in!");
				else
				{
					StringBuilder output = new StringBuilder();
					for(Player nextPlayer : game.players)
					{
						output.append(nextPlayer.printBombs()).append("\n");
					}
					event.replyInDm(output.toString());
				}
				//We found the right channel, so
				return;
			}
		}
	}

}
