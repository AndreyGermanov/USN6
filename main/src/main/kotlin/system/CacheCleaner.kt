package system

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * Class for cronjob, which used to clean cache folder (remove all subfolders, which are not needed)
 */
class cleanCacheDirs: TimerTask() {
    /**
     * Task runner
     */
    override fun run() {
        val dirs = File(ConfigManager.webConfig["cache_path"].toString()).listFiles()
        if (dirs===null || dirs.isEmpty()) return
        for (dir in dirs) {
            if (dir.isDirectory) {
                if (!Files.exists(Paths.get(dir.path+"/lockfile")) &&
                        !Files.exists(Paths.get(dir.path+"/lockfile.tmp"))) {
                    val files = dir.listFiles()
                    for (file in files) {
                        file.delete()
                    }
                    dir.delete()
                }
            }
        }
    }
}