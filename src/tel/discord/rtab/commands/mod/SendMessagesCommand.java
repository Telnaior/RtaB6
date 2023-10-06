package tel.discord.rtab.commands.mod;

import static tel.discord.rtab.RaceToABillionBot.waiter;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;

public class SendMessagesCommand extends Command
{
	boolean sendToAll = false;
	TextChannel channel;
	PrivateChannel myChannel;
	public SendMessagesCommand()
	{
        this.name = "sendmessages";
        this.aliases = new String[] {"echo"};
        this.help = "lets me send messages as the bot";
        this.guildOnly = false;
        this.ownerCommand = true;
		this.hidden = true;
	}
	@Override
	protected void execute(CommandEvent event)
	{
		//First, get the channel we're sending to from the argument
		String channelID = event.getArgs();
		if(channelID.equalsIgnoreCase("all"))
			sendToAll = true;
		else
		{
			sendToAll = false;
			channel = event.getJDA().getTextChannelById(channelID);
		}
		//We just opened the private channel by sending the command lol
		myChannel = event.getAuthor().openPrivateChannel().complete();
		try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		getMessage();
	}
	
	private void getMessage()
	{
		waiter.waitForEvent(MessageReceivedEvent.class,
				//Make sure it's from me in my private channel
				e -> (e.getChannel().getId().equals(myChannel.getId())),
				//Read their choice and handle things accordingly
				e -> 
				{
				if(e.getMessage().getContentRaw().equals("STOP"))
					return;
				else if(sendToAll)
				{
					for(GameController nextGameChannel : RaceToABillionBot.game)
						nextGameChannel.channel.sendMessage(e.getMessage().getContentRaw()).queue();
				}
				else
					channel.sendMessage(e.getMessage().getContentRaw()).queue();
				getMessage();
				});
	}
}