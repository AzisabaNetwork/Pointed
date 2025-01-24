package dev.felnull.pointed.task;

import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.util.RankingSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class ClockMachine {

    public void rankingUpdaterTaskStarter(){
        //0時と12時にランキング更新
        scheduleDailyTask(23, 0, this::rankingUpdaterDefaultTask);
        scheduleDailyTask(11, 0, this::rankingUpdaterDefaultTask);
    }

    private void rankingUpdaterDefaultTask(){
        Bukkit.getServer().broadcast(Component.text("戦果ランキングが更新されました").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        RankingSystem.getRankingList();
        RankingSystem.broadcastRanking();
    }

    //1日の指定された時間、分に送られてきたタスクを実行
    public void scheduleDailyTask(int hour, int minute, Runnable task) {
        long currentMillis = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long targetMillis = calendar.getTimeInMillis();
        if (targetMillis < currentMillis) {
            // 翌日のスケジュールに変更
            targetMillis += TimeUnit.DAYS.toMillis(1);
        }

        long initialDelay = targetMillis - currentMillis;
        long oneDayInTicks = 20L * 60 * 60 * 24; // 1日の長さ (20 ticks/秒)

        Bukkit.getScheduler().runTaskTimer(Pointed.getInstance(), task, initialDelay / 50 + 2, oneDayInTicks);
    }
}
