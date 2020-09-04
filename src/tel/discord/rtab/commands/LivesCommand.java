package tel.discord.rtab.commands;

import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;
import tel.discord.rtab.LifePenaltyType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.jagrosh.jdautilities.command.CommandEvent;


public class LivesCommand extends ParsingCommand {
	public LivesCommand()
    {
        this.name = "lives";
		this.aliases = new String[]{"refill", "hp"};
        this.help = "see how many lives you have left, and how long until they refill";
        this.guildOnly = true;
    }
	
	@Override
	protected void execute(CommandEvent event)
	{
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.equals(event.getChannel()))
			{
				if(game.lifePenalty == LifePenaltyType.NONE)
				{
					event.reply("You have unlimited lives in this channel.");
					return;
				}
				try
				{
					List<String> list = Files.readAllLines(Paths.get("scores","scores"+event.getChannel().getId()+".csv"));
					//If no name given, check it for themselves
					int index;
					if(event.getArgs() == "")
						index = findUserInList(list,event.getAuthor().getId(),false);
					//If it's a mention, search by the id of the mention
					else if(event.getArgs().startsWith("<@!"))
					{
						String mentionID = parseMention(event.getArgs());
						index = findUserInList(list,mentionID,false);
					}
					//Otherwise check it for the player named
					else
					{
						index = findUserInList(list,event.getArgs(),true);
					}
					//Then pass off to the actual controller if they're an actual user
					if(index < 0 || index >= list.size())
						event.reply("User not found.");
					else
						event.reply(checkLives(game, index));
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				//We found the right channel, so
				return;
			}
		}
	}
	
	public String checkLives(GameController game, int index) //TODO - fix this to work with different life penalties
	{
		StringBuilder output = new StringBuilder();
		try
		{
			List<String> list = Files.readAllLines(Paths.get("scores"+game.channel.getId()+".csv"));
			String[] record = list.get(index).split("#");
			output.append(record[1] + ": ");
			int newbieProtection = Integer.parseInt(record[5]);
			int lives = Integer.parseInt(record[6]);
			if(newbieProtection > 0)
			{
				output.append(newbieProtection);
				output.append(" game");
				if(newbieProtection != 1)
					output.append("s");
				output.append(" of newbie protection left.");
			}
			else
			{
				//Calculate how much their lives have refilled
				Instant lifeRefill = Instant.parse(record[7]);
				while(lifeRefill.isBefore(Instant.now()))
				{
					if(lives < game.maxLives)
						lives = game.maxLives;
					else
						lives++;
					lifeRefill = lifeRefill.plusSeconds(72000);
				}
				//Just display a negative life count as 0 lol
				output.append(Math.max(lives,0));
				if(lives == 1)
					output.append(" life left.");
				else
					output.append(" lives left.");
				//If they're out of lives, tell them how much their next game's entry fee would be
				if(lives <= 0 && game.lifePenalty != LifePenaltyType.HARDCAP)
				{
					int money = Integer.parseInt(record[2]);
					int entryFee;
					switch(game.lifePenalty)
					{
					case FLAT:
						entryFee = 1_000_000;
						break;
					case SCALED:
						entryFee = GameController.calculateEntryFee(money, 0);
						break;
					case INCREASING:
						entryFee = GameController.calculateEntryFee(money, lives);
						break;
					default: //We shouldn't be here
						entryFee = 1_000_000_000;
						break;
					}
					output.append(String.format(" Playing now will cost $%,d.",entryFee));
				}
				//If they're below the base maximum, tell them how long until they get a refill
				if(lives < game.maxLives)
				{
					output.append(" Lives refill in ");
					//Check hours, then minutes, then seconds
					OffsetDateTime lifeRefillTime = lifeRefill.minusSeconds(Instant.now().getEpochSecond())
							.atOffset(ZoneOffset.UTC);
					int hours = lifeRefillTime.getHour();
					if(hours>0)
					{
						output.append(hours + " hours, ");
					}
					int minutes = lifeRefillTime.getMinute();
					if(hours>0 || minutes>0)
					{
						output.append(minutes + " minutes, ");
					}
					int seconds = lifeRefillTime.getSecond();
					if(hours>0 || minutes>0 || seconds>0)
					{
						output.append(seconds + " seconds");
					}
					output.append(".");
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return output.toString();
	}
}
