package pt.ua.imodec.util;

import java.io.File;
import java.io.IOException;

public class MiscUtils {

    /**
     *
     * Same as file.createNewFile, however creates parents directories if they don't exist
     *
     * @param file File to be created
     * @return Status flag
     * @throws IOException from file.createNewFile
     */
    public static boolean createNewFile(File file) throws IOException {
        File parentFile = file.getParentFile();

        if (!parentFile.exists())
            return createNewFile(parentFile);

        return file.createNewFile();
    }

}
