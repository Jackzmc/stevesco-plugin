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
import me.jackz.jackzco3.lib.InventoryStore;
import me.jackz.jackzco3.lib.LocationStore;
import me.jackz.jackzco3.lib.Util;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.inventivetalent.glow.GlowAPI;

import java.util.*;

public class InteractEvent implements Listener {


	private final Main plugin;
	private final Util util;
	private jPhoneMain jphone;
	InteractEvent(Main plugin,jPhoneMain jphone) {
		this.plugin = plugin;
		this.jphone = jphone;
		this.util = new Util();
	}

	private Random random = new Random();
	private double randomnum() {
		return random.nextBoolean() ? random.nextDouble() : -random.nextDouble();
	}
	@EventHandler
	public void PhoneClick(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		//below is the stupid way to stop double activation
		if(e.getAction() == Action.PHYSICAL) return;
		ItemStack item = p.getInventory().getItemInMainHand();
		ItemMeta meta = item.getItemMeta();
		if(e.getHand().equals(EquipmentSlot.HAND)) {
			//spacing so i dont get confused. rightclick
			if(item.getType().equals(Material.TRIPWIRE_HOOK) && meta != null && meta.getDisplayName() != null && meta.getDisplayName().contains(jphone.phoneName)) {
				e.setCancelled(true);
				//cancel event, then set the item in hand to itself, fixing ghosting
				p.getInventory().setItemInMainHand(p.getInventory().getItemInMainHand());
				NBTItem nbti = ItemNBTAPI.getNBTItem(item);
				if (e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.PISTON_BASE) {
					if (new LocationStore(plugin).getBoolean(e.getClickedBlock().getLocation())) {
						if (nbti.getInteger("battery") == 100) {
							p.sendMessage("§7Your phone is already at §e100%");
							return;
						}
						//charge phone
						p.getWorld().spawnParticle(Particle.SPELL_INSTANT, util.getCenterLocation(e.getClickedBlock().getLocation()).add(0, 3, 0), 40, 0.5, 3, 0.5);
						p.sendMessage("§7Charging...");
						for (int i = 0; i < 59; i++) {
							plugin.getServer().getScheduler().runTaskLater(plugin, () -> p.getWorld().spawnParticle(Particle.SPELL_INSTANT,
									util.getCenterLocation(e.getClickedBlock().getLocation()).add(0, 3, 0),
									10, 0.5, 3, 0.5),
									i
							);
						}
						plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
							if (p.getInventory().getItemInMainHand().getType() != Material.TRIPWIRE_HOOK) { //TODO?: add to the above loop?
								p.sendMessage("§7Charging aborted - you must hold your phone.");
								return;
							}
							p.playSound(e.getClickedBlock().getLocation(), Sound.BLOCK_NOTE_CHIME, 1, 1);
							nbti.setInteger("battery", 100);
							p.sendMessage("§aYour phone has been charged!");
							p.getInventory().setItemInMainHand(nbti.getItem());
						}, 60L);
						return;
					}

				}

				if (!nbti.getBoolean("state")) {
					if (p.isSneaking()) {
						if (nbti.getInteger("battery") < 5) {
							p.sendMessage("§7Battery is too low to start");
						} else {
							nbti.setBoolean("state", true);
							p.sendMessage("§7Turned on phone");
						}
						p.getInventory().setItemInMainHand(nbti.getItem());
						return;
					}
					p.sendMessage("§cPhone is turned off, shift+rightclick to turn it on");
					return;
				}
				if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
					if (p.isSneaking()) {
						p.openInventory(jphone.getAppSwitcher(p));
						p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 0.2F, 5);
					} else {
						Integer battery = nbti.getInteger("battery");
						p.sendMessage(" ");
						if (battery == -1) {
							p.sendMessage("§ajPhoneOS Version §e" + plugin.getJackzCo().getString("versions.jphone") + "§6 | §aBattery §cDead");
						} else {
							p.sendMessage("§ajPhoneOS Version §e" + plugin.getJackzCo().getString("versions.jphone") + "§6 | §a Battery §e" + battery + "%");
						}
						p.sendMessage("§7Check your data by /jackzco jcloud info");
						if (!(nbti.hasKey("owner"))) {
							//hover: "Go to App Switcher->Settings->Owner to claim"
							TextComponent msg = new TextComponent("§cThis device is not claimed. ");
							TextComponent msg_hover = new TextComponent("§c[Hover to learn how to]");
							//message.setClickEvent( new ClickEvent( ClickEvent.Action.OPEN_URL, "http://spigotmc.org" ) );
							msg_hover.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("To claim this device go to \n§eapp switcher §rthen §esettings §rthen\n§eowner§r to claim.").create()));
							msg.addExtra(msg_hover);
							p.spigot().sendMessage(msg);
							//Key "owner" not set
						} else {
							//is claimed
							TextComponent msg = new TextComponent("§9This device is claimed. §7Hover for details");
							msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§9Device claimed by\n§7" + nbti.getString("owner")).create()));

							p.spigot().sendMessage(msg);
						}
						p.sendMessage(" ");
					}
				} else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
					InventoryStore store = new InventoryStore(plugin,"keychain_" + p.getName(),9*3);
					if (p.isSneaking()) {
						if(!jphone.isInTowerRange(p.getLocation())) {
							p.sendMessage("§cCannot access your keychain, please get in range of a tower.");
							return;
						}
						Inventory inv = store.loadInv();
						p.openInventory(inv);
					} else {
						//https://gist.github.com/Caeden117/92223ecd39b61bd3310aee64e0dfd0d0
						HashMap<String,Double> towers = jphone.getTowers(p.getLocation());
						if(towers.isEmpty()) {
							p.sendMessage("§cCould not locate any nearby towers");
							return;
						}
						for (String tower : towers.keySet()) {
							Double distances = towers.get(tower);
							p.sendMessage("§7Tower §e" + tower + "§7:§e " + getQualityTerm(distances) + " §7(" + Math.round(distances) + " blocks)");
						}
					}
				}
			}else if(p.getInventory().getItemInMainHand().getType() == Material.PISTON_BASE && e.getAction() == Action.RIGHT_CLICK_AIR) {
				if (meta != null && meta.getDisplayName() != null && meta.getDisplayName().equals("§fjCharger")) {
					e.setCancelled(true);
					p.sendMessage("§7Please right click on a gold block to setup the §ejCharger");
					return;
				}
			}else if(util.checkItem(item,Material.BONE,"§3jWrench")) {
				e.setCancelled(true);
				if(p.isSneaking()) {
					ItemMeta phoneMeta = item.getItemMeta();
					item.setType(Material.TRIPWIRE_HOOK);
					phoneMeta.setDisplayName(jphone.phoneName); //check if 2X
					item.setItemMeta(phoneMeta);
					p.getInventory().setItemInMainHand(item);
					return;
				}
				List<Material> allowedBlocks = new ArrayList<>(Arrays.asList(
						Material.PISTON_BASE,
						Material.PISTON_STICKY_BASE,
						Material.IRON_DOOR,
						Material.DISPENSER,
						Material.CHEST,
						Material.WOOD_DOOR,
						Material.DROPPER
				));
				if(e.getClickedBlock() != null && e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK) {
					Block b = e.getClickedBlock();
					if(!allowedBlocks.contains(b.getType())) {
						p.sendMessage("§cInvalid block");
						//p.sendMessage("§cValid blocks: §e" + String.join(",",allowedBlocks::toString));
						return;
					}
					try {
						/*BlockFace[] directions = {BlockFace.NORTH,BlockFace.EAST,BlockFace.SOUTH,BlockFace.WEST,BlockFace.UP,BlockFace.DOWN};
						Directional dir = (Directional) b;
						BlockFace direction = dir.getFacing();
						dir.setFacingDirection(BlockFace.EAST_NORTH_EAST);*/
						if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {
							/*int index = java.util.Arrays.binarySearch(directions,direction);
							if(index+1 == directions.length) {
								dir.setFacingDirection(directions[0]);
							}else{
								dir.setFacingDirection(directions[++index]);
							}*/
							int data = b.getData();
							b.setData((byte) ((data == 5) ? 0 : ++data));
						}else if(e.getAction() == Action.LEFT_CLICK_BLOCK) {
							int data = b.getData();
							b.setData((byte) ((data == 0) ? 5 : --data));
						}

					}catch(IllegalArgumentException ex) {
						plugin.getLogger().warning("Wrench failure: " + ex.toString());
						p.sendMessage("§7Uh oh! Something went wrong. §c" + ex.toString());
					}

				}else{
					p.sendMessage("§cPlease left/right click a block");
				}
			}else if(p.getInventory().getItemInMainHand().getType() == Material.NETHER_STAR) {
				if (p.isSneaking()) {
					if(!plugin.checkRegion(p.getLocation(),"horsechaos")) {
						p.sendMessage("§cYou must be in the horsechaos region");
						return;
					}
					if(e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_AIR) {
						for (int i = 0; i < 50; i++) {
							double rnd = Math.random();
							Material mt;
							if (rnd < .25) {
								mt = Material.WOOL;
							} else if (rnd >= .25 && rnd < .5) {
								mt = Material.STAINED_CLAY;
							} else if (rnd >= .5 && rnd < .75) {
								mt = Material.CONCRETE;
							} else {
								mt = Material.CONCRETE_POWDER;
							}
							@SuppressWarnings("deprecation")
							FallingBlock fb = p.getWorld().spawnFallingBlock(p.getEyeLocation(), mt, (byte) random.nextInt(16));
							fb.setHurtEntities(false);
							fb.setDropItem(false);
							GlowAPI.setGlowing(fb, GlowAPI.Color.WHITE, p);
							Vector v = p.getEyeLocation().getDirection();
							v = v.add(new Vector(random.nextInt((1 - -1) + 1) + -1,0,random.nextInt((1 - -1) + 1) + -1));
							fb.setVelocity(v.multiply(2));

						}
						return;
					}
					Block targetbk = p.getTargetBlock(null, 100);
					Location targetloc;
					if (targetbk == null) {
						p.sendMessage("§cCan't find block, must be within 100 blocks");
						return;
					}else if(targetbk.getType().equals(Material.GLASS)) {
						p.sendMessage("§cGlass is blacklisted, try some other block.");
						return;
					}
					targetloc = targetbk.getLocation();
					if(!plugin.checkRegion(targetloc,"horsechaos")) {
						p.sendMessage("§cYou must be in the horsechaos region");
						return;
					}
					p.getWorld().createExplosion(targetloc, 0);

					for (int i = 0; i < 50; i++) {
						double rnd = Math.random();
						Material mt;
						if (rnd < .25) {
							mt = Material.WOOL;
						} else if (rnd >= .25 && rnd < .5) {
							mt = Material.STAINED_CLAY;
						} else if (rnd >= .5 && rnd < .75) {
							mt = Material.CONCRETE;
						} else {
							mt = Material.CONCRETE_POWDER;
						}
						@SuppressWarnings("deprecation")
						FallingBlock fb = p.getWorld().spawnFallingBlock(targetloc, mt, (byte) random.nextInt(16));
						fb.setHurtEntities(false);
						fb.setDropItem(false);
						GlowAPI.setGlowing(fb, GlowAPI.Color.WHITE, p);
						Vector v = new Vector(randomnum(), randomnum(), randomnum());
						fb.setVelocity(v.multiply(2));
					}
				} else {
					if(!plugin.checkRegion(p.getLocation(),"horsechaos")) {
						p.sendMessage("§cYou must be in the horsechaos region");
						return;
					}
					final Horse entity = (Horse) p.getWorld().spawnEntity(p.getEyeLocation(), EntityType.HORSE);
					entity.setStyle(Horse.Style.NONE);
					e.setCancelled(true);
					entity.setVelocity(p.getEyeLocation().getDirection().multiply((e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) ? 5 : 2));
					Bukkit.getScheduler().runTaskLater(plugin, () -> {
						entity.getWorld().createExplosion(entity.getLocation(), 0);
						entity.remove();
						for (int i = 0; i < 100; i++) {
							double rnd = Math.random();
							Material mt;
							if (rnd < .25) {
								mt = Material.WOOL;
							} else if (rnd >= .25 && rnd < .5) {
								mt = Material.STAINED_CLAY;
							} else if (rnd >= .5 && rnd < .75) {
								mt = Material.CONCRETE;
							} else {
								mt = Material.CONCRETE_POWDER;
							}
							@SuppressWarnings("deprecation")
							FallingBlock fb = entity.getWorld().spawnFallingBlock(entity.getLocation(), mt, (byte) random.nextInt(16));
							fb.setHurtEntities(false);
							fb.setDropItem(false);
							GlowAPI.setGlowing(fb, GlowAPI.Color.WHITE, p);
							Vector v = new Vector(randomnum(), randomnum(), randomnum());
							fb.setVelocity(v.multiply(2));
						}
					}, 15);
				}
			}else if(item.getType().equals(Material.TORCH) && meta != null && meta.getDisplayName() != null && meta.getDisplayName().equals("§fjLight") ) {
				e.setCancelled(true);
				if(e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
					ItemStack phone = p.getInventory().getItemInMainHand();
					ItemMeta phoneMeta = phone.getItemMeta();
					phone.setType(Material.TRIPWIRE_HOOK);
					phoneMeta.setDisplayName(jphone.phoneName); //check if 2X
					phone.setItemMeta(phoneMeta);
					p.getInventory().setItemInMainHand(phone);
				}

			}
			//below is the stupid way to stop offhand placement. I don't know if two setcancels will fuck it up but i hope not
		}
		if(e.getHand().equals(EquipmentSlot.OFF_HAND)) {
			if(item.getType() == Material.TRIPWIRE_HOOK && meta != null && meta.getDisplayName() != null && meta.getDisplayName().contains(jphone.phoneName)) {
				e.setCancelled(true);
			}
		}

	}
	String getQualityTerm(Double distance) {
		if (distance < 25)  return "§3NASA Quality";
		if (distance < 100) return "§2Excellent";
		if (distance < 250) return "§aGreat";
		if (distance < 400) return "§eOK";
		if (distance < 550) return "§cPoor";
		if (distance < 600) return "§cHorrible";
		return "§4Unreachable";
	}

}
