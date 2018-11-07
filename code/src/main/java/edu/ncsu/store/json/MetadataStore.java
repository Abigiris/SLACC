package edu.ncsu.store.json;

import com.google.gson.JsonObject;
import edu.ncsu.config.Settings;
import edu.ncsu.executors.models.ClassMethods;
import edu.ncsu.store.IMetadataStore;
import edu.ncsu.utils.Utils;

import java.io.File;
import java.util.logging.Logger;

public class MetadataStore implements IMetadataStore {

    private static final Logger LOGGER = Logger.getLogger(MetadataStore.class.getName());

    @Override
    public void saveClassFunctionsMetadata(JsonObject metadata, ClassMethods classMethods) {
        LOGGER.info("Writing metadata ... ");
        String writeFolder = Utils.pathJoin(Settings.META_STORE, classMethods.getDataset(), "functions",
                classMethods.getPackageName().replaceAll("\\.", File.separator));
        Utils.mkdir(writeFolder);
        String writeFile = Utils.pathJoin(writeFolder, String.format("%s.json", classMethods.getClassName()));
        JSONDriver.saveJsonObject(metadata, writeFile, true);
    }
}
