package pt.ua.imodec.util;

import java.io.File;
import java.io.IOException;
import java.io.WriteAbortedException;

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
            if (!parentFile.mkdirs())
                throw new IOException(String.format("Unexpected error: Could not mkdir of dir '%s'.", parentFile));

        return file.createNewFile();
    }

}
