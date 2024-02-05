package tel.discord.rtab.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import tel.discord.rtab.MinigameTournament;
import tel.discord.rtab.RaceToABillionBot;
import tel.discord.rtab.SuperBotChallenge;

public class ReadyCommand extends Command
{
	public ReadyCommand()
	{
		this.name = "ready";
		this.help = "Indicate your readiness to play a round in the Minigame Tournament or Super Bot Challenge";
		this.guildOnly = false;
	}
	@Override
	protected void execute(CommandEvent event)
	{
		//Just find the right channel and pass the player id on to the appropriate method
		for(MinigameTournament tournament : RaceToABillionBot.tournament)
		{
			if(tournament.channel.getId().equals(event.getChannel().getId()))
			{
				switch(tournament.status)
				{
				case LOADING:
					event.reply("Tournament still loading. Try again in a few seconds.");
					break;
				case PLAYING:
					event.reply("Someone else is already playing.");
					break;
				case SHUTDOWN:
					break;
				case OPEN:
					tournament.runHuman(event.getMember());
					break;
				}
			}
		}
		for(SuperBotChallenge challenge : RaceToABillionBot.challenge)
		{
			if(challenge.channel.getId().equals(event.getChannel().getId()))
			{
				if(challenge.loadingHumanGame)
					challenge.readyUp(event.getAuthor().getId());
				else
					challenge.searchForHumanGame(event.getAuthor().getId());
			}
		}
	}
}