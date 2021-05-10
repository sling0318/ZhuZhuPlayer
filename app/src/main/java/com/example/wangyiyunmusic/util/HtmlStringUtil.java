package com.example.wangyiyunmusic.util;

import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

import java.io.File;

public class HtmlStringUtil {

    public static Spanned SongSingerName(String song, String singer){
        if (TextUtils.isEmpty(song) && TextUtils.isEmpty(singer))
            return Html.fromHtml("<font color = \"#212121\">快去听听音乐吧</font>",
                    Html.FROM_HTML_OPTION_USE_CSS_COLORS);
        if (TextUtils.isEmpty(singer) || singer.equals("<unknown>")) singer = "Unknown";

        String SongInformation = "<font color = \"#212121\">"+song+"</font>"+
                "<font color = \"#757575\"><small> - "+singer+"</small></font>";
        return Html.fromHtml(SongInformation,Html.FROM_HTML_OPTION_USE_CSS_COLORS);
    }
    public static String SheetTips(int count){
        return "已有歌单("+count+"个)";
    }
    /*public static String MusicTime(long duration){
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
        return sdf.format(new Date(duration));
    }
    public static String getSystemTime(){
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-DD HH:mm:ss");
        return sdf.format(new Date());
    }
    public static String getTimeDifference(long time1,long time2){
        return "执行了"+(time2-time1)+"";
    }*/

    public static boolean FileExists(String targetFileAbsPath){
        try {
            File f= new File(targetFileAbsPath);
            if(!f.exists()) return false;
        }catch (Exception e){
            return false;
        }
        return true;
    }
}
