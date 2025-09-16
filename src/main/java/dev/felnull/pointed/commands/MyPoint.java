package dev.felnull.pointed.commands;

import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.database.dataio.SubjectPointsDao;
import dev.felnull.pointed.database.dataio.SubjectRepository;
import dev.felnull.pointed.fileio.ConfigList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;

public class MyPoint implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(sender instanceof ConsoleCommandSender){
            sender.sendMessage("このコマンドはプレイヤー専用です");
            return true;
        }

        Player player = (Player) sender;
        List<String> viewPointList = Pointed.getInstance().getConfig().getStringList(ConfigList.VIEWPOINT.configName);
        for(String viewPoint : viewPointList){
            try {
                int[] playerPointData = SubjectPointsDao.loadOneByName(SubjectRepository.ensurePlayer(((Player) sender).getUniqueId(), sender.getName()), viewPoint);
                sender.sendMessage(viewPoint);
                sender.sendMessage(player.getName() + "の保有ポイント: " + playerPointData[0] + "累計獲得ポイント: " + playerPointData[1]);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }


        return true;
    }
}
