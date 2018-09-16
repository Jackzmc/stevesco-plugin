/*
 * Copyright (C) 2018 Jackson Bixby
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.jackz.jackzco3.jPhone;

import de.tr7zw.itemnbtapi.ItemNBTAPI;
import de.tr7zw.itemnbtapi.NBTItem;
import me.jackz.jackzco3.Main;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.inventivetalent.glow.GlowAPI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ChatListener implements Listener {
	private Main plugin;
	private jPhoneMain jphone;
	ChatListener(Main plugin,jPhoneMain jphone) {
		this.plugin = plugin;
		this.jphone = jphone;
	}
	@EventHandler
	public void jPhoneChat(AsyncPlayerChatEvent e) {
		Player p = e.getPlayer();
		ItemStack itm = p.getInventory().getItemInMainHand();
		if(itm != null) {
			NBTItem nbt = ItemNBTAPI.getNBTItem(itm);
			if(!nbt.hasNBTData()) {
				return;
			}
			if(nbt.getBoolean("terminal") && !nbt.getBoolean("state")) { //check if terminal mode on, and its off
				p.sendMessage("§7Cannot connect to phone: §cPhone is offline");
				return;
			}
			if(nbt.getBoolean("terminal")) {
				e.setCancelled(true);
				p.sendMessage(" ");
				p.sendMessage("§a>" + e.getMessage());
				String[] args = e.getMessage().split(" ");
				switch(args[0].toLowerCase()) {
					case "version":
						p.sendMessage("§7The current version of terminal is §e" + plugin.getJackzCo().getString("versions.terminal"));
						break;
					case "light":
					case "jlight":
						ItemStack CurrentPhone = nbt.getItem();
						ItemMeta PhoneMeta = CurrentPhone.getItemMeta();
						PhoneMeta.setDisplayName("§fjLight");
						CurrentPhone.setItemMeta(PhoneMeta);
						CurrentPhone.setType(Material.TORCH);
						p.getInventory().setItemInMainHand(CurrentPhone);
						break;
					case "commands":
						List<String> cmds = new ArrayList<>(Arrays.asList( //make clickable names
								"§ehelp §7get jphone help",
								"§eversion §7check the version of terminal",
								"§elight §7turn on your flashlight",
								"§eclaim §7claim the device as yours",
								"§eglow §7highlight player, entities, monsters",
								"§edangers §7highlights dangers (legacy",
								"§estate §7turn on/off phone",
								"§elookup §7lookup a player by UUID",
								"§eexit §7exit terminal mode",
								"§ejcloud §7manage your jCloud account"
						));
						p.sendMessage("§3Current Commands:\n" + String.join("\n",cmds));
						break;
					case "help":
						p.sendMessage("§7Hi, terminal is currently in alpha and missing features.");
						p.sendMessage("§7Current Version is: §e" + plugin.getJackzCo().getString("versions.terminal"));
						p.sendMessage("§7Type §ecommands §7to view commands");
						break;
					case "claim":
					case "own":
						if (nbt.hasKey("owner")) {
							p.sendMessage("§cThis device is claimed by: §e" + nbt.getString("owner")  + (nbt.getString("owner").equals(p.getUniqueId().toString()) ? " §7(You)":""));
						} else {
							nbt.setString("owner", p.getUniqueId().toString());
							p.sendMessage("§7Claimed device as §e" + p.getUniqueId().toString());
							p.getInventory().setItemInMainHand(nbt.getItem());
						}
						break;
					case "charge":
						nbt.setInteger("battery", 100);
						p.getInventory().setItemInMainHand(nbt.getItem());
						p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING,1,1);
						p.sendMessage("§aYour phone has been charged using the power of BlockChain(TM)");
						break;
					case "trash":
						Inventory trash = Bukkit.createInventory(null, 9 * 3, "jPhone Portable Trash");
						p.openInventory(trash);
						break;
					case "dangers": {
						if (plugin.getServer().getPluginManager().getPlugin("GlowAPI") == null) {
							p.sendMessage("§cThis feature is disabled, missing plugin §eGlowAPI");
							return;
						}
						List<Entity> entities = new ArrayList<>();
						for (Entity ent : p.getNearbyEntities(50, 50, 50)) {
							if (ent instanceof Monster) {
								entities.add(ent);
								GlowAPI.setGlowing(ent, GlowAPI.Color.DARK_RED, p);
							} else if (ent instanceof Player && p.getWorld().getPVP()) {
								//if ent is player and PVP is enabled for that world
								entities.add(ent);
								GlowAPI.setGlowing(ent, GlowAPI.Color.RED, p);
							}

						}
						Bukkit.getScheduler().runTaskLater(plugin, () -> {
							for (Entity ent : entities) {
								if (GlowAPI.isGlowing(ent, p)) {
									GlowAPI.setGlowing(ent, false, p);
								}
							}
						}, (30 * 20L));
						p.sendMessage("§cFound " + entities.size() + " dangers");
						break;
					} case "glow":
					case "highlight": {
						if (plugin.getServer().getPluginManager().getPlugin("GlowAPI") == null) {
							p.sendMessage("§cThis feature is disabled, missing plugin §eGlowAPI");
							return;
						}
						if (args.length > 1) {
							if (args[1].equalsIgnoreCase("players")) {
								int count = 0;
								for (Player player : Bukkit.getOnlinePlayers()) {
									if (!(GlowAPI.isGlowing(player, p))) {
										GlowAPI.setGlowing(player, GlowAPI.Color.WHITE, p);
										count += 1;
									}
									Bukkit.getScheduler().runTaskLater(plugin, () -> {
										if (GlowAPI.isGlowing(player, p)) GlowAPI.setGlowing(player, false, p);
									}, (30 * 20L));
								}
								p.sendMessage("§7Made §e" + count + "§7 players glow for §e30§7 seconds");
								break;
							} else if (args[1].equalsIgnoreCase("entities")) {
								int count = 0;
								for (Entity ent : p.getNearbyEntities(50, 50, 50)) {
									if (!(ent instanceof Player)) {
										if (!(GlowAPI.isGlowing(ent, p))) {

											GlowAPI.setGlowing(ent, GlowAPI.Color.WHITE, p);
											count += 1;
										}
										Bukkit.getScheduler().runTaskLater(plugin, () -> {
											if (GlowAPI.isGlowing(ent, p)) GlowAPI.setGlowing(ent, false, p);
										}, (30 * 20L));
									}
								}
								p.sendMessage("§7Made §e" + count + "§7 entities glow for §e30§7 seconds");
								break;
							}  else if (args[1].equalsIgnoreCase("dangers")) {
								int count = 0;
								for (Entity ent : p.getNearbyEntities(50, 50, 50)) {
									if (ent instanceof Monster) {
										if (!(GlowAPI.isGlowing(ent, p))) {
											GlowAPI.setGlowing(ent, GlowAPI.Color.WHITE, p);
											count += 1;
										}
										Bukkit.getScheduler().runTaskLater(plugin, () -> {
											if (GlowAPI.isGlowing(ent, p)) GlowAPI.setGlowing(ent, false, p);
										}, (30 * 20L));
									}
								}
								p.sendMessage("§7Made §e" + count + "§7 entities glow for §e30§7 seconds");
								break;
							} else if (args[1].equalsIgnoreCase("all")) {
								int count = 0;
								for (Entity ent : p.getNearbyEntities(50, 50, 50)) {
									if (!(GlowAPI.isGlowing(ent, p))) {
										GlowAPI.setGlowing(ent, GlowAPI.Color.WHITE, p);
										count += 1;
									}
									Bukkit.getScheduler().runTaskLater(plugin, () -> {
										if (GlowAPI.isGlowing(ent, p)) GlowAPI.setGlowing(ent, false, p);
									}, (30 * 20L));
								}
								p.sendMessage("§7Made §e" + count + "§7 entities/players glow for §e30§7 seconds");
								break;
							}
						} else {
							TextComponent msg = new TextComponent("§cPlease choose an option: §e");
							TextComponent msg_2 = new TextComponent("[Players]");
							TextComponent msg_3 = new TextComponent(" [Entities]");
							TextComponent msg_4 = new TextComponent(" [All]"); //i hope i can simplify all of this
							msg_2.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"glow players"));
							msg_3.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "glow entities"));
							msg_4.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "glow all"));

							msg.addExtra(msg_2);
							msg.addExtra(msg_3);
							msg.addExtra(msg_4);
							p.spigot().sendMessage(msg);
							//Key "owner" not set
						}
						break;
					}case "state":
						if (nbt.getBoolean("state")) {
							nbt.setBoolean("state", false);
							p.sendMessage("§7Phone has been switched off.");
						} else {
							nbt.setBoolean("state", true);
							p.sendMessage("§7Phone has been turned on.");
						}
						p.getInventory().setItemInMainHand(nbt.getItem());
						break;
					case "lookup":
						p.sendMessage("§7Looking up player from UUID...");
						try {
							UUID uuid = UUID.fromString(args[1]);
							p.sendMessage("§7Player: §e" + Bukkit.getOfflinePlayer(uuid).getName());
						}catch(IllegalArgumentException ex) {
							p.sendMessage("§cPlayer was not found, or invalid UUID");
						}
						break;
					case "jcloud":
						switch(args[1].toLowerCase()) {
							default:
								p.sendMessage("§7Failed to fetch data from jCloud API: §c501 Not Implemented");
								break;
						}
						break;
					case "exit":
						nbt.setBoolean("terminal",false);
						p.sendMessage("§7Exited §eterminal mode");
						p.getInventory().setItemInMainHand(nbt.getItem());
						break;
					default:
						p.sendMessage("§cUnknown command was specified. §7Type §ehelp for help");

				}

			}
		}
	}
}
