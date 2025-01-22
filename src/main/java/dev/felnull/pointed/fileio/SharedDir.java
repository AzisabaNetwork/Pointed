package dev.felnull.pointed.fileio;

import dev.felnull.pointed.Pointed;
import lombok.Getter;

import java.io.File;

public class SharedDir {
    @Getter
    public static String dir;

    public SharedDir(){
        dir = "shared";
        File file = new File(dir);
        if(!file.isAbsolute()) {
            File parent = Pointed.getInstance().getDataFolder();
            parent = parent.getParentFile().getParentFile();
            dir = new File(parent, dir).getAbsolutePath();
        }
    }
}
