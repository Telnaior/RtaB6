package tel.discord.rtab.events;

import static tel.discord.rtab.RaceToABillionBot.waiter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import tel.discord.rtab.GameController;
import tel.discord.rtab.MoneyMultipliersToUse;
import tel.discord.rtab.Player;
import tel.discord.rtab.PlayerStatus;
import tel.discord.rtab.RtaBMath;

public class RevivalChance implements EventSpace
{
	EventStatus status = EventStatus.PREPARING;
	private enum EventStatus
	{
		PREPARING, WAITING, RESOLVING, BOMB_PLACE, FINISHED
	}
	
	private enum RevivalPrize
	{
		PEEK_REFUND("a new peek")
		{
			void awardPrize(GameController game, Player target, RevivalChance event)
			{
				game.channel.sendMessage(String.format("%s receives another peek!", target.getName())).queue();
				target.peeks ++;
			}
		},
		HIDDEN_COMMAND("a hidden command")
		{
			void awardPrize(GameController game, Player target, RevivalChance event)
			{
				game.channel.sendMessage(String.format("%s receives a hidden command!", target.getName())).queue();
				target.awardHiddenCommand();
			}
		},
		TAX_REFUND("a bomb refund")
		{
			void awardPrize(GameController game, Player target, RevivalChance event)
			{
				game.channel.sendMessage(String.format("%s receives a **$%,d** bomb refund!", target.getName(), 
						game.applyBaseMultiplier(250_000))).queue();
				target.addMoney(game.applyBaseMultiplier(250_000), MoneyMultipliersToUse.BOOSTER_ONLY);
			}
		},
		OMG_ITS_A_BOMB("a new bomb")
		{
			void awardPrize(GameController game, Player target, RevivalChance event)
			{
				game.channel.sendMessage(String.format("%s gets to place a new bomb!", target.getName())).queue();
				//We can't do this in a static way, so let's just get back out
				event.status = EventStatus.BOMB_PLACE;
				event.newBomb(target);
			}
		},
		NOTHING("no bonus")
		{
			void awardPrize(GameController game, Player target, RevivalChance event)
			{
				game.channel.sendMessage(String.format("%s receives no bonus.",	target.getName())).queue();
			}
		};
		
		final String prize;
		RevivalPrize(String prize) { this.prize = prize; }
		String getPrize() {	return prize; }
		abstract void awardPrize(GameController game, Player target, RevivalChance event);
	}
	
	ArrayList<Integer> candidates;
	ArrayList<Integer> waitingOn;
	
	GameController game;
	int player;
	private Player getCurrentPlayer()
	{
		return game.players.get(player);
	}
	
	@Override
	public String getName()
	{
		return "Revival Chance";
	}

	//Note for the future: if this space can ever be randomly generated, we have to include handling for the failsafe
	@Override
	public void execute(GameController game, int player)
	{
		//Initialise stuff
		this.game = game;
		this.player = player;
		game.channel.sendMessage("You've found the **Revival Chance**!").queue();
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		//Check if there's anyone to revive in the first place
		if(game.playersAlive == game.players.size())
		{
			game.channel.sendMessage("But no one even needs to be revived...").queue();
			try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			failedRevival();
		}
		else
		{
			//Get candidates
			getCandidates();
		}
		//Wait for them to be done
		while(status != EventStatus.FINISHED)
		{
			try { Thread.sleep(2000); } catch (InterruptedException e) { status = EventStatus.FINISHED; Thread.currentThread().interrupt(); }
		}
		//Once it's finished, the execute method ends and control passes back to the game controller
	}
	
	private void getCandidates()
	{
		candidates = new ArrayList<>(game.players.size());
		waitingOn = new ArrayList<>(game.players.size());
		//Loop through the current list of players and see who might need a revival
		for(int i=0; i<game.players.size(); i++)
		{
			if(game.players.get(i).status != PlayerStatus.ALIVE)
			{
				if(game.players.get(i).isBot)
				{
					candidates.add(i);
				}
				else
				{
					waitingOn.add(i);
					//Set up flow trap and wait for input
					status = EventStatus.WAITING;
					final int iInner = i;
					waiter.waitForEvent(MessageReceivedEvent.class,
							//Right player and channel
							e ->
							{
		                            if(e.getChannel().getId().equals(game.channel.getId()) && e.getAuthor().equals(game.players.get(iInner).user))
		        					{
		        						String firstLetter = e.getMessage().getContentStripped().toUpperCase().substring(0,1);
		        						return(firstLetter.startsWith("Y") || firstLetter.startsWith("N"));
		        					}
		                            return false;
							},
							//Parse it and call the method that does stuff
							e ->
							{
								waitingOn.remove(Integer.valueOf(iInner));
								if(e.getMessage().getContentStripped().toUpperCase().startsWith("Y"))
									candidates.add(iInner);
								checkReady();
							},
							45,TimeUnit.SECONDS, () ->
							{
								waitingOn.remove(Integer.valueOf(iInner));
								checkReady();
							});
				}
			}
		}
		//Everyone's been set up, now let's see what's actually here
		if(status == EventStatus.WAITING) //waiting on at least one player
		{
			StringBuilder messageString = new StringBuilder();
			for(int i : waitingOn)
			{
				messageString.append(game.players.get(i).getSafeMention());
				messageString.append(", ");
			}
			messageString.append("would you like a chance to be revived? (Y/N, 45 seconds)");
			game.channel.sendMessage(messageString.toString()).queue();
		}
		else
		{
			status = EventStatus.WAITING;
			checkReady();
		}
	}
	
	private synchronized void checkReady()
	{
		//If we're not ready or we've already processed things, eat the call
		if(status != EventStatus.WAITING || !waitingOn.isEmpty())
			return;
		//Okay, time to play REVIVAL CHANCE!
		status = EventStatus.RESOLVING;
		if(!candidates.isEmpty())
		{
			try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			playRevivalChance();
		}
		else
		{
			game.channel.sendMessage("No one wants to be revived...").queue();
			try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			failedRevival();
		}
	}
	
	private void playRevivalChance()
	{
		Collections.shuffle(candidates);
		game.channel.sendMessage("Let's play **Revival Chance**!").queue();
		int delay = 1000 + (int)(RtaBMath.random()*500);
		int revivalTarget = -1;
		String targetName = "no one";
		RevivalPrize chosenPrize = RevivalPrize.NOTHING;
		Message message = game.channel.sendMessage("Now Reviving: "+targetName+" with no bonus?").completeAfter(1, TimeUnit.SECONDS);
		while(delay < 2500)
		{
			try { Thread.sleep(delay); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			revivalTarget = (int)(RtaBMath.random() * (candidates.size()+1)) - 1;
			chosenPrize = RevivalPrize.values()[(int)(RtaBMath.random() * RevivalPrize.values().length)];
			if(revivalTarget == -1)
				targetName = "no one";
			else
				targetName = game.players.get(candidates.get(revivalTarget)).getName();
			message.editMessage("Now Reviving: "+targetName+" with "+chosenPrize.getPrize()+"?").queue();
			delay += (int)(RtaBMath.random()*500);
		}
		try { Thread.sleep(2500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		message.editMessage("Now Reviving: **"+targetName+"** with **"+chosenPrize.getPrize()+"**"+(revivalTarget==-1?"...":"!")).queue();
		if(revivalTarget == -1)
			failedRevival(chosenPrize);
		else
			revive(game.players.get(candidates.get(revivalTarget)), chosenPrize);
	}
	
	void failedRevival()
	{
		failedRevival(RevivalPrize.values()[(int)(RtaBMath.random() * RevivalPrize.values().length)]);
	}
	
	void failedRevival(RevivalPrize chosenPrize)
	{
		status = EventStatus.RESOLVING;
		game.channel.sendMessage("We'll just have to give you the bonus instead!").queue();
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		chosenPrize.awardPrize(game, getCurrentPlayer(), this);
		if(status == EventStatus.RESOLVING)
			status = EventStatus.FINISHED;
	}
	
	void revive(Player target, RevivalPrize chosenPrize)
	{
		//This is a mess~
		if(target.lifeLost)
		{
			target.lifeLost = false;
			target.lives ++;
			if(target.lives > 0)
				target.totalLivesSpent --;
		}
		target.cursed = false;
		target.threshold = false;
		target.splitAndShare = false;
		target.status = PlayerStatus.ALIVE;
		game.playersAlive ++;
		game.channel.sendMessage("Welcome back, "+target.getSafeMention()+"!").queue();
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		chosenPrize.awardPrize(game, target, this);
		if(status == EventStatus.RESOLVING)
			status = EventStatus.FINISHED;
	}
	
	void newBomb(Player target)
	{
		if(target.isBot)
		{
			//Get safe spaces, starting with all unpicked spaces
			ArrayList<Integer> openSpaces = new ArrayList<>(game.boardSize);
			for(int i=0; i<game.boardSize; i++)
				if(!game.pickedSpaces[i])
					openSpaces.add(i);
			//Remove all known bombs
			ArrayList<Integer> safeSpaces = new ArrayList<>(game.boardSize);
			safeSpaces.addAll(openSpaces);
			for(Integer bomb : target.knownBombs)
				safeSpaces.remove(bomb);
			//Bomb one at random
			int bombPosition = 0;
			if(!safeSpaces.isEmpty())
				 bombPosition = safeSpaces.get((int)(RtaBMath.random()*safeSpaces.size()));
			game.gameboard.addBomb(bombPosition);
			target.myBombs.add(bombPosition);
			target.knownBombs.add(bombPosition);
			status = EventStatus.FINISHED;
		}
		else
		{
			target.user.openPrivateChannel().queue(
					(channel) -> channel.sendMessage("Please place your bomb within the next 90 seconds "
							+ "by sending a number 1-" + game.boardSize + " (or 0 to cancel)").queue());
			waiter.waitForEvent(MessageReceivedEvent.class,
					//Check if right player, and valid bomb pick
					e -> (e.getAuthor().equals(target.user)
							&& e.getChannel().getType() == ChannelType.PRIVATE && 
							(game.checkValidNumber(e.getMessage().getContentStripped()) || e.getMessage().getContentStripped().equals("0"))),
					//Parse it and update the bomb board
					e -> 
					{
						int bombLocation = Integer.parseInt(e.getMessage().getContentStripped())-1;
						if(bombLocation > -1)
						{
							game.gameboard.addBomb(bombLocation);
							target.myBombs.add(bombLocation);
							target.knownBombs.add(bombLocation);
							target.user.openPrivateChannel().queue(
									(channel) -> channel.sendMessage("Bomb placement confirmed.").queue());
						}
						else
						{
							target.user.openPrivateChannel().queue(
									(channel) -> channel.sendMessage("Bomb placement cancelled.").queue());
						}
						status = EventStatus.FINISHED;
					},
					//Or timeout the prompt after a minute
					90, TimeUnit.SECONDS,
					() ->
					{
						target.user.openPrivateChannel().queue(
								(channel) -> channel.sendMessage("Bomb placement cancelled.").queue());
						status = EventStatus.FINISHED;
					});
		}
	}
}
