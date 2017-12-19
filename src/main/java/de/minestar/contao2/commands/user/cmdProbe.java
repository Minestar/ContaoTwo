/*
 * Copyright (C) 2012 MineStar.de 
 * 
 * This file is part of Contao2.
 * 
 * Contao2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * Contao2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Contao2.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.minestar.contao2.commands.user;

import java.util.*;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import de.minestar.contao2.core.Core;
import de.minestar.contao2.manager.DatabaseManager;
import de.minestar.contao2.manager.PlayerManager;
import de.minestar.contao2.units.ContaoGroup;
import de.minestar.minestarlibrary.commands.AbstractCommand;
import de.minestar.minestarlibrary.utils.ChatUtils;
import de.minestar.minestarlibrary.utils.PlayerUtils;

public class cmdProbe extends AbstractCommand {

    private HashMap<String, Long> timeMap = new HashMap<>();

    private PlayerManager playerManager;
    private DatabaseManager databaseManager;

    public cmdProbe(String syntax, String arguments, String node, PlayerManager playerManager, DatabaseManager dbHandler) {
        super(Core.LOG_NAME, syntax, arguments, node);
        this.description = "Probeuser hinzufügen";
        this.playerManager = playerManager;
        this.databaseManager = dbHandler;
    }

    @Override
    public void execute(String[] args, Player player) {
        addProbe(args, player);
    }

    @Override
    public void execute(String[] args, ConsoleCommandSender console) {
        addProbe(args, console);
    }

    private void addProbe(String[] args, CommandSender sender) {
        // SET CONTAO GROUP
        String inputPlayerName = args[0];

        Player player = PlayerUtils.getOnlinePlayer(inputPlayerName);
        if(player == null) {
            ChatUtils.writeError(sender, pluginName, "Spieler '" + inputPlayerName + "' ist nicht online. Offline Beförderungen aktuell nicht möglich.");
            return;
        }

        UUID uuid = player.getUniqueId();

        int forumID = checkUser(sender, inputPlayerName, uuid);
        if(forumID == -1) {
            ChatUtils.writeError(sender, pluginName, "User nicht im Forum gefunden. Prüfe ob er seinen UserNick korrekt eingetragen hat.");
            return;
        }


        if(!setToProbe(sender, forumID, uuid)) {
            ChatUtils.writeError(sender, pluginName, "User konnte nicht zum ProbeUser befördert werden.");
            return;
        }

        // PRINT INFO
        ChatUtils.writeSuccess(sender, pluginName, "Spieler '" + inputPlayerName + "' ist nun Probeuser!");
        if (player.isOnline()) {
            PlayerUtils.sendSuccess(player, "Du bist nun Probeuser!");
            PlayerUtils.sendSuccess(player, "Herzlich Willkommen auf Minestar.de");
            PlayerUtils.sendSuccess(player, "Viel Spass im Probegebiet :)");
        }
        // UPDATE GROUPMANAGER-GROUP
        this.playerManager.updateGroupManagerGroup(player.getName(), ContaoGroup.PROBE);
    }

    private int checkUser(CommandSender sender, String mcName, UUID uuid) {

        // USER IS IN X
        if (playerManager.isInGroup(mcName, ContaoGroup.X)) {
            ChatUtils.writeError(sender, pluginName, "Fehler: User ist in Gruppe X.");
            ChatUtils.writeInfo(sender, pluginName, "Er wurde bereits abgelehnt!");
            return -1;
        }

        HashMap<Integer, String> forumIDs = databaseManager.getForumIDs(mcName);

        if(forumIDs == null || forumIDs.isEmpty()) {
            ChatUtils.writeError(sender, pluginName, "Fehler: MinecraftNick nicht gefunden.");
            return -1;
        }
        else if(forumIDs.size() > 1) {
            ChatUtils.writeError(sender, pluginName, "Fehler: Mehrere ForenAccounts mit gleichem MinecraftNick gefunden.");
            for(Integer i : forumIDs.keySet()) {
                ChatUtils.writeInfo(sender, pluginName, Integer.toString(i));
            }
            return -1;
        }

        int forumID = forumIDs.keySet().iterator().next();

        ContaoGroup contaoGroup = databaseManager.getContaoGroup(forumID);

        if(!ContaoGroup.DEFAULT.equals(contaoGroup) && !ContaoGroup.FREE.equals(contaoGroup)) {
            ChatUtils.writeError(sender, pluginName, "Spieler '" + mcName + "' ist weder Free noch Default! Aktuelle Gruppe: " + contaoGroup);
            return -1;
        }

        if (!databaseManager.isForumAccountActive(forumID)) {
            ChatUtils.writeError(sender, pluginName, "Fehler: Bestätigungsemail wurde noch nicht bestätigt.");
            ChatUtils.writeInfo(sender, pluginName, "Bitte dem User bescheid sagen das er bestätigen muss.");
            return -1;
        }


        List<Integer> forumIDsForUUID = databaseManager.getForumIds(uuid);
        if(forumIDsForUUID == null) {
            ChatUtils.writeError(sender, pluginName, "Fehler: Konnte nicht prüfen ob UUID bereits eingetragen ist.");
            return -1;
        }

        if(forumIDsForUUID.size() > 2 || !forumIDs.containsKey(forumID)) {
            ChatUtils.writeError(sender, pluginName, "Fehler: UUID ist bereits für folgende ForenAccounts hinterlegt.");
            for(Integer i : forumIDsForUUID) {
                ChatUtils.writeInfo(sender, pluginName, Integer.toString(i));
            }
            return -1;
        }

        return forumID;
    }


    public boolean setToProbe(CommandSender sender, int forumID, UUID uuid) {

        if (databaseManager.isMCUUIDInUser(uuid)){
            ContaoGroup oldGroup = databaseManager.getContaoGroup(forumID);

            if (oldGroup != null && !ContaoGroup.DEFAULT.equals(oldGroup)) {
                databaseManager.removeGroup(forumID, oldGroup);
            }
        }


        if(!databaseManager.addGroup(ContaoGroup.PROBE, forumID)) {
            ChatUtils.writeError(sender, pluginName, "Fehler: UserGruppe konnte nicht hinzugefügt werden.");
            return false;
        }

        if (!databaseManager.updateuserOnlineGroupID(forumID, ContaoGroup.PROBE)) {
            ChatUtils.writeError(sender, pluginName, "Fehler: UserGruppe konnte nicht gesetzt werden.");
            return false;
        }


        if(!databaseManager.setProbeValues(forumID, sender.getName(), uuid)) {
            ChatUtils.writeError(sender, pluginName, "Fehler: ProbeDaten konnte nicht gesetzt werden.");
            return false;
        }

        return true;
    }
}
