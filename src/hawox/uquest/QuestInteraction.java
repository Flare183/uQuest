package hawox.uquest;

import hawox.uquest.questclasses.CurrentQuest;

import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nijiko.coelho.iConomy.system.Account;

final public class QuestInteraction {
	private final UQuest plugin;

	
	public QuestInteraction(UQuest plugin){
		this.plugin = plugin;
	}
	
	//hoping that making only one random number gen keeps numbers more random than they were =/
	Random numberGen = new Random();
	
	/*
	 * General give quest method
	 * 
	 * -Remember to add a check to see if that quest is available
	 * 
	 * returns true if player got a new quest
	 */
	public boolean giveQuest(int questNumber, Player player, boolean showText){
		Quester quester = getQuester(player);
		//check if the player already has an active quest or not (-1 is no active quest)
		if( this.getCurrentQuest(player,false) == null ){
			quester.giveQuest(questNumber, plugin.getTheQuests().get(questNumber));
			//get their quest info again to output stuffs
			this.getCurrentQuest(player,this.isScaleQuestLevels()).printInfo(this.plugin, player);
			return true;
		} else {
			//player dosn't have a quest
			if(showText == true){
				player.sendMessage(ChatColor.RED + "You already have an active quest!");
			}
			return false;
		}
	}
	
	/*
	 * Gives a player a random quest from the quest pool
	 * 
	 * returns true if player got a new quest
	 */
	public boolean giveQuestRandom(Player player, boolean showText){
		return giveQuest(numberGen.nextInt(getQuestTotal()),player,showText);
		/*
		Quester quester = getQuester(player);
		//check if the player already has an active quest or not (-1 is no active quest)
		if( this.getCurrentQuest(player,this.isScaleQuestLevels()) == null ){
			//player can get a quest! Assign them a quest ID
			quester.setQuestID(numberGen.nextInt(getQuestTotal()));
			//get their quest info again to output stuffs
			this.getCurrentQuest(player,this.isScaleQuestLevels()).printInfo(this.plugin, player);
			return true;
		} else {
			//player dosn't have a quest
			if(showText == true){
				player.sendMessage(ChatColor.RED + "You already have an active quest!");
			}
			return false;
		}
		*/
	}
	
	/*
	 * Player tries to turn in a quest
	 * 
	 * returns true if their quest was turned in
	 */
	public boolean questTurnInAttempt(Player player, boolean showText){
		CurrentQuest currentQuest = getCurrentQuest(player,this.isScaleQuestLevels());
			//player has a quest so check if it's done
			if(currentQuest.doneCheck(this.plugin, player)){
				if(showText == true){
					questTurnInForceDone(player,showText);
				}else{
					questTurnInForceDone(player);
				}
				return true;
			} else {
				//the quest isn't done
				return false;
			}
		}
	
	/*
	 * Force complete a players current quest
	 */
	public void questTurnInForceDone(Player player, boolean showText){
		Quester quester = plugin.getQuestInteraction().getQuester(player);

		CurrentQuest currentQuest = getCurrentQuest(player,this.isScaleQuestLevels());
		
		currentQuest.finish(plugin, player, showText);
		
		//Tell the server for every x quests someone completes
		if( (quester.getQuestsCompleted() % plugin.getQuestAnnounceInterval() ) == 0){
			plugin.getServer().broadcastMessage(ChatColor.YELLOW + player.getName() + " has completed " + ChatColor.DARK_PURPLE + quester.getQuestsCompleted() + ChatColor.YELLOW + " quests! [Quest Level " + ChatColor.AQUA + getQuestLevel(player) + ChatColor.YELLOW + "]");
		}
		
		//for every 10 quests give them a random 10 blocks!
		if( ( quester.getQuestsCompleted() % plugin.getQuestRewardInterval() ) == 0){
			Random numberGen = new Random();
			int itemNumberatInterValReward = numberGen.nextInt( plugin.getQuestRewards().length );
			try{
				String rewards[] = plugin.getQuestRewards()[itemNumberatInterValReward].split(",");
				player.getInventory().addItem(new ItemStack(Integer.parseInt(rewards[0]), Integer.parseInt(rewards[2])));
				plugin.getServer().broadcastMessage(ChatColor.YELLOW + player.getName() + " got a reward of " + ChatColor.DARK_PURPLE + rewards[2] + " " + rewards[1] + ChatColor.YELLOW + "!");
			}catch(NumberFormatException nfe){
					plugin.log.log(Level.SEVERE, "[" + plugin.getPdfFile().getName() + "] Invalid quest reward item ID! Giving them dirt by default!");
					plugin.getServer().broadcastMessage(ChatColor.RED + "There was an invalid item ID in the quest rewards config! so you get 10 dirt!");
					player.getInventory().addItem(new ItemStack(Material.DIRT, 10));
					plugin.getServer().broadcastMessage(ChatColor.YELLOW + player.getName() + " got a reward of " + ChatColor.DARK_PURPLE + "10 Dirt" + ChatColor.YELLOW + "!");
				}
		}
		
		//Tell the server for every x quests that the difficulty increased!
		if( (quester.getQuestsCompleted() % plugin.getQuestLevelInterval() ) == 0){
			//plugin.getServer().broadcastMessage(ChatColor.YELLOW + player.getName() + " has completed " + ChatColor.DARK_PURPLE + quester.getQuestsCompleted() + ChatColor.YELLOW + " quests!");
			plugin.getServer().broadcastMessage(ChatColor.GOLD + player.getName() + ChatColor.RED + " is now on quest level " + ChatColor.DARK_RED + getQuestLevel(player));
		}
		
		
	}
	
	/*
	 * Drops a players current quest
	 */
	public void questDrop(Player player){
		Quester quester = getQuester(player);
		//set them to having no active quest
		quester.setQuestID(-1);
		quester.clearTracker();
		//save them to file
		plugin.saveQuesterToFile(quester);
		
		if(plugin.isUseSQLite() == true){
			plugin.getDB().put(player.getName(), quester);
		}
		
	}
	
	/*
	 * Returns the players current quest with no level scaling
	 * -Returns void if they have no quest
	 */
	public CurrentQuest getCurrentQuest(Player player, boolean scaleToQuesterLevel){
		Quester quester = getQuester(player);
		//get the players current quest as well if they have one
		CurrentQuest currentQuest = null;
		if(quester.getQuestID() != -1){
			if(scaleToQuesterLevel == true){
				currentQuest = new CurrentQuest(plugin, plugin.getTheQuests().get(quester.getQuestID()), 0);
			}else{
				currentQuest = new CurrentQuest(plugin, plugin.getTheQuests().get(quester.getQuestID()), this.getQuestLevel(player));
			}
		}
		return currentQuest;
	}
	
	/*
	 * Shows the questers info
	 */
	@SuppressWarnings("static-access")
	public void showQuestersInfo(Player player){
		Quester quester = getQuester(player);
		//Tell the player their active quest name
		player.sendMessage(ChatColor.GOLD + "Stats for: " + player.getName());
		String tempQuestNameStorage = "You have no active quests!";
		
		if( quester.getQuestID() != -1 ){
			//get the players active quest name
			tempQuestNameStorage = this.getCurrentQuest(player,this.isScaleQuestLevels()).getName();
		}
		player.sendMessage("Quest Level: " + getQuestLevel(player));
		player.sendMessage("Active quest: " + tempQuestNameStorage);
		//tell the player their # of completed quests
		player.sendMessage("Quests completed: " + quester.getQuestsCompleted());
		//tell the player the amount of money they have earned from quests
		if(plugin.isUseiConomy() == true){
			player.sendMessage("Total " + plugin.getiConomy().getBank().getCurrency() + " received: " + quester.getMoneyEarnedFromQuests());
		}
	}
	
	/*
	 * Gets the current amount of loaded quests
	 */
	public int getQuestTotal(){
		return plugin.theQuests.size();
	}
	
	/*
	 * Returns the quester that's asoiated with the player
	 */
	public Quester getQuester(Player player) {
		//flatfile
		if(plugin.isUseSQLite() == true){
			return plugin.getDB().get(player.getName());
		}else{
			for (Quester quester : plugin.theQuesterList) {
				if (quester.theQuestersName.equalsIgnoreCase(player.getName())) {
					return quester;
				}
			}
			// if we get to this point the player is not found so add him to the
			// list and return him!
			plugin.placePlayerIntoList(player);
			// Because we're using recursion, lets tell the console so we know if we
			// hit an infinite loop
			System.out.println("[Hawox uQuest] getQuester : Quester not found, placing them into list and retrying.");
			return getQuester(player); // should return true at this point!
		}
	}
	
	/*
	 * Gets the level of the quester
	 */
	public int getQuestLevel(Player player) {
		//update the questerlevel first
		Quester quester = getQuester(player);
		quester.setQuestLevel(quester.getQuestsCompleted()/plugin.getQuestLevelInterval());
		
		if(plugin.isUseSQLite() == true){
			plugin.getDB().put(player.getName(), quester);
		}
		return quester.getQuestLevel();
	}
	
	//save them to file
	public void saveQuester(Player player){
		Quester quester = this.getQuester(player);
		plugin.saveQuesterToFile(quester);
	
		if(plugin.isUseSQLite() == true){
			plugin.getDB().put(player.getName(), quester);
		}
	}
	
	/*
	 * Gets the hashset of players on the drop quest reset timer
	 */
	public HashSet<String> getPlayersOnDropQuestTimer(){
		return plugin.getCanNotDrop();
	}
	
	/*
	 * Takes a player off the drop quest reset timer hashmap
	 */
	public boolean removePlayerFromDropQuestList(String name){
		HashSet<String> list = this.getPlayersOnDropQuestTimer();
		if(list.contains(name)){
			list.remove(name);
			plugin.setCanNotDrop(list);
			return true;
		}else{
			return false; //player was not on the list so they could not be removed
		}
	}
	
	/*
	 * Adds a player to the drop quest reset timer hashmap
	 */
	public boolean addPlayerToDropQuestList(String name){
		HashSet<String> list = this.getPlayersOnDropQuestTimer();
		if(list.contains(name)){
			return false;
		}else{
			//Not on list so added
			list.add(name);
			plugin.setCanNotDrop(list);
			return true;
		}
	}
	
	/*
	 * Checks if the player is on the drop quest reset timer hashmap
	 */
	public boolean isPlayerOnDropQuestList(String name){
		if(this.getPlayersOnDropQuestTimer().contains(name)){
			return true;
		}else{
			return false;
		}
	}
	
	/*
	 * Adds the player to a timer that removes them from the drop list
	 */
	public boolean removePlayerFromDropQuestListWithTimer(String name, int howLongInMinutes){
		if(isPlayerOnDropQuestList(name) && !(plugin.canNotDropRemoveTimer.contains(name)) ){
			plugin.canNotDropRemoveTimer.add(name);
			ScheduledThreadPoolExecutor removePlayerFromDropQuestListWithTimer_Timer = new ScheduledThreadPoolExecutor(1);
			removePlayerFromDropQuestListWithTimer_Timer.schedule(new Runnable() {
				public void run() {
					if(plugin.canNotDropRemoveTimer.size() > 0){
						removePlayerFromDropQuestList(plugin.canNotDropRemoveTimer.get(0));
						plugin.canNotDropRemoveTimer.remove(0);
					}
				}
				}, howLongInMinutes, TimeUnit.MINUTES);
		}
		return false;
	}
	
	/**
	 * Item interaction
	 */
	public void removeItem(Player player, int id, int amountToConsume) {
		Inventory bag = player.getInventory();
		while (amountToConsume > 0) {
			int slot = bag.first(id);
			ItemStack item = bag.getItem(slot);
			if (item.getAmount() <= amountToConsume) {
				amountToConsume -= item.getAmount();
				bag.clear(slot);
			} else {
				// more in this stack than than we need
				item.setAmount(item.getAmount() - amountToConsume);
				amountToConsume = 0;
			}
		}
	}
	
	public int countItems(Player player, int itemID) {
		// get the players inventory, check every slot for said item, count the
		// number in the slot, add it to total -> Return
		int count = 0;
		ItemStack[] allItems = player.getInventory().getContents();
		for (int i = 0; i < allItems.length; i++) {
			if (allItems[i] != null) {
				if (allItems[i].getTypeId() == itemID) {
					count += allItems[i].getAmount();
				}
			}
		}
		return count;
	}
	
	
	
	/**
	 * Support for different money stuff it's best to juse use these!
	 **/
	@SuppressWarnings("static-access")
	public double getMoney(Player player) {
		if(plugin.isUseiConomy()){
			return (plugin.getiConomy().getBank().getAccount(player.getName()).getBalance());
		}else if(plugin.isUseBOSEconomy()){
			return plugin.getTheBOSEconomy().getPlayerMoney(player.getName());
		}
		return 0;
	}

	@SuppressWarnings("static-access")
	public void setMoney(Player player, double toWhat) {
		if(plugin.isUseiConomy()){
			Account account = plugin.getiConomy().getBank().getAccount(player.getName());
			account.setBalance(toWhat);
			account.save();
		}
		if(plugin.isUseBOSEconomy()){
			plugin.getTheBOSEconomy().setPlayerMoney(player.getName(), (int) toWhat, false);
		}
		
		/*
		if(plugin.isUseEssentialsEco()){
			String[] empty = null;
			plugin.getEssentials().getPlayer(args, pos)
		}*/
	}

	public void addMoney(Player player, int addWhat, boolean showText) {
		if(plugin.isUseiConomy()){
			double balance = getMoney(player);
			if(showText == true){
				player.sendMessage(ChatColor.AQUA
						+ "**You have been rewarded with "
						+ Integer.toString(addWhat) + " " + /*plugin.getiConomy().getBank().getCurrency()*/ plugin.getMoneyName() + "!");
			}
			setMoney(player, balance + addWhat);
			Quester quester = getQuester(player);
			quester.setMoneyEarnedFromQuests(quester.getMoneyEarnedFromQuests() + addWhat);
			if(showText == true){
				player.sendMessage(ChatColor.AQUA + "**Your new balance is: "
						+ getMoney(player) + " " + /*plugin.getiConomy().getBank().getCurrency()*/ plugin.getMoneyName());
			}
			if(plugin.isUseSQLite() == true){
				plugin.getDB().put(player.getName(), quester);
			}
		}
		if(plugin.isUseBOSEconomy()){
			double balance = getMoney(player);
			if(showText == true){
				player.sendMessage(ChatColor.AQUA
						+ "**You have been rewarded with "
						+ Integer.toString(addWhat) + " " + /*plugin.getTheBOSEconomy().getMoneyNamePlural()*/ plugin.getMoneyName() + "!");
			}
			setMoney(player, balance + addWhat);
			Quester quester = getQuester(player);
			quester.setMoneyEarnedFromQuests(quester.getMoneyEarnedFromQuests() + addWhat);
			if(showText == true){
				player.sendMessage(ChatColor.AQUA + "**Your new balance is: "
						+ getMoney(player) + " " + /*plugin.getTheBOSEconomy().getMoneyNamePlural()*/ plugin.getMoneyName());
			}
			if(plugin.isUseSQLite() == true){
				plugin.getDB().put(player.getName(), quester);
			}
		}
		
	}
	
	
	
	
	
	
	
	
	/**
	 * Config file variables
	 */


	public boolean isScaleQuestLevels() {
		return plugin.isScaleQuestLevels();
	}

	public void setScaleQuestLevels(boolean scaleQuestLevels) {
		plugin.setScaleQuestLevels(scaleQuestLevels);
	}

	public boolean isBroadcastSaving() {
		return plugin.isBroadcastSaving();
	}

	public void setBroadcastSaving(boolean broadcastSaving) {
		plugin.setBroadcastSaving(broadcastSaving);
	}

	public boolean isUseiConomy() {
		return plugin.isUseiConomy();
	}

	public void setUseiConomy(boolean useiConomy) {
		plugin.setUseiConomy(useiConomy);
	}

	public boolean isUsePermissions() {
		return plugin.isUsePermissions();
	}

	public void setUsePermissions(boolean usePermissions) {
		plugin.setUsePermissions(usePermissions);
	}

	public boolean isUseSQLite() {
		return plugin.isUseSQLite();
	}

	public void setUseSQLite(boolean useSQLite) {
		plugin.setUseSQLite(useSQLite);
	}

	public boolean isUseDefaultUQuest() {
		return plugin.isUseDefaultUQuest();
	}

	public void setUseDefaultUQuest(boolean useDefaultUQuest) {
		plugin.setUseDefaultUQuest(useDefaultUQuest);
	}

	public int getSaveQuestersInfoIntervalInMinutes() {
		return plugin.getSaveQuestersInfoIntervalInMinutes();
	}

	public void setSaveQuestersInfoIntervalInMinutes( int saveQuestersInfoIntervalInMinutes) {
		plugin.setSaveQuestersInfoIntervalInMinutes(saveQuestersInfoIntervalInMinutes);
	}

	public int getQuestAnnounceInterval() {
		return plugin.getQuestAnnounceInterval();
	}

	public void setQuestAnnounceInterval(int questAnnounceInterval) {
		plugin.setQuestAnnounceInterval(questAnnounceInterval);
	}

	public int getQuestRewardInterval() {
		return plugin.getQuestRewardInterval();
	}

	public void setQuestRewardInterval(int questRewardInterval) {
		plugin.setQuestRewardInterval(questRewardInterval);
	}

	public int getQuestLevelInterval() {
		return plugin.getQuestLevelInterval();
	}

	public void setQuestLevelInterval(int questLevelInterval) {
		plugin.setQuestLevelInterval(questLevelInterval);
	}
	public int getDropQuestInterval() {
		return plugin.getDropQuestInterval();
	}

	public void setDropQuestInterval(int dropQuestInterval) {
		plugin.setDropQuestInterval(dropQuestInterval);
	}

	public int getDropQuestCharge() {
		return plugin.getDropQuestCharge();
	}

	public void setDropQuestCharge(int dropQuestCharge) {
		plugin.setDropQuestCharge(dropQuestCharge);
	}

	public boolean isUseBOSEconomy() {
		return plugin.isUseBOSEconomy();
	}

	public void setUseBOSEconomy(boolean useBOSEconomy) {
		plugin.setUseBOSEconomy(useBOSEconomy);
	}
	
	
	
	
	/**
	 * Overloaded commands for the stuff that pukes out text
	 * 
	 * Give, done, info, money
	 */
	public boolean giveQuest(int questNumber, Player player){
		return giveQuest(questNumber, player, true);
	}
	
	public boolean giveQuestRandom(Player player){
		return giveQuestRandom(player, true);
	}
	
	public boolean questTurnInAttempt(Player player){
		return questTurnInAttempt(player, true);
	}
	
	public void questTurnInForceDone(Player player){
		questTurnInForceDone(player, true);
	}

	public void addMoney(Player player, int addWhat) {
		addMoney(player, addWhat, true);
	}
	
	
	
	
	
	
	
	
	
	
}