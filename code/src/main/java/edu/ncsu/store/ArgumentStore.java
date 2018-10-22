package edu.ncsu.store;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import edu.ncsu.config.Properties;
import edu.ncsu.executors.ArgumentGenerator;
import edu.ncsu.executors.models.ClassMethods;
import edu.ncsu.executors.models.Function;
import edu.ncsu.executors.models.Primitive;
import edu.ncsu.utils.Utils;
import edu.ncsu.visitors.adapters.ConstantAdapter;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

public class ArgumentStore {

    private static final Logger LOGGER = Logger.getLogger(ArgumentStore.class.getName());

    private String dataset;

    private static volatile Map<String, ArgumentStore> storeMap = new HashMap<>();

    private ArgumentStore(String dataset) {
        this.dataset = dataset;
    }

    public static ArgumentStore loadArgumentStore(String dataset) {
        if (!storeMap.containsKey(dataset)) {
            storeMap.put(dataset, new ArgumentStore(dataset));
        }
        return storeMap.get(dataset);
    }

    /**
     * Get path where primitive arguments are stored.
     * @return - File path
     */
    private String getPrimitiveArgStorePath() {
        return Utils.pathJoin(Properties.META_STORE, dataset, "primitive_arguments.json");
    }

    /**
     * Get path of folder containing the fuzzed arguments
     * @return - Folder path
     */
    private String getArgumentsFolder() {
        return Utils.pathJoin(Properties.META_STORE, dataset, "arguments");
    }


    /***
     * Get path of the index file of the fuzzed arguments
     * @return - Index Path
     */
    private String getArgumentsIndexPath() {
        return Utils.pathJoin(getArgumentsFolder(), "index.json");
    }


    // PRIMITIVE ARGUMENTS
    // ****************************************

    /**
     * Load Primitive Arguments
     * @return - Map of Primitive and Set of the arguments.
     */
    public synchronized Map<Primitive, Set<Object>> loadPrimitiveArguments() {
        LOGGER.info("Loading primitive arguments ... ");
        Map<Primitive, Set<Object>> primitiveArguments = new HashMap<>();
        try (Reader reader = new FileReader(getPrimitiveArgStorePath())) {
            Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
            Map<String, Set<String>> gsonData = gson.fromJson(reader, new TypeToken<HashMap<String, Set<String>>>(){}.getType());
            for (String key: gsonData.keySet()) {
                Set<String> argStrings = gsonData.get(key);
                Set<Object> args = new HashSet<>();
                Primitive primitive = Primitive.getPrimitiveByName(key);
                for (String argString: argStrings)
                    args.add(Primitive.convertToArgument(primitive, argString));
                primitiveArguments.put(primitive, args);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return primitiveArguments;
    }

    /**
     * Save primitive arguments
     * @param primitiveArguments - Map of Primitive and Set of arguments.
     */
    public synchronized void savePrimitiveArguments(Map<Primitive, Set<Object>> primitiveArguments) {
        LOGGER.info("Saving primitive arguments ... ");
        Map<String, Set<Object>> gsonFriendlyArguments = new HashMap<>();
        for (Primitive primitive: primitiveArguments.keySet())
            gsonFriendlyArguments.put(primitive.getName(), primitiveArguments.get(primitive));
        StoreUtils.saveObject(gsonFriendlyArguments, getPrimitiveArgStorePath());
    }

    // FUZZED ARGUMENTS
    // ****************************************

    /**
     * Check if fuzzed key exists
     * @param key - Key to be checked
     * @return - True if key exists
     */
    public synchronized boolean fuzzedKeyExists(String key) {
        return loadFuzzedArgumentIndices().has(key);
    }

    /**
     * Save fuzzed arguments
     * @param key - Key to be saved to
     * @param arguments - Arguments to save
     */
    public synchronized void saveFuzzedArguments(String key, Object arguments) {
        Utils.mkdir(getArgumentsFolder());
        String indexKey = Utils.randomString();
        saveArgumentIndex(key, indexKey);
        String argsFile = Utils.pathJoin(getArgumentsFolder(), String.format("%s.json", indexKey));
        StoreUtils.saveObject(arguments, argsFile);
    }

    /**
     * Save Argument Index for a key and index
     * @param argKey - Argument key
     * @param indexKey - Index key
     */
    private void saveArgumentIndex(String argKey, String indexKey) {
        JsonObject jsonObject = loadFuzzedArgumentIndices();
        jsonObject.addProperty(argKey, indexKey);
        StoreUtils.saveJsonObject(jsonObject, getArgumentsIndexPath(), true);
    }

    /**
     * Load fuzzed argument indices
     * @return - Get a json object for codejam arguments.
     */
    private JsonObject loadFuzzedArgumentIndices() {
        return StoreUtils.getJsonObject(getArgumentsIndexPath());
    }

    /**
     * Load fuzzed arguments.
     * @param key - Argument key to load
     * @return - Array of fuzzed arguments.
     */
    public JsonArray loadFuzzedArguments(String key) {
        JsonObject index = loadFuzzedArgumentIndices();
        if (!index.has(key))
            return null;
        String indexKey = index.get(key).getAsString();
        String filePath = Utils.pathJoin(getArgumentsFolder(), String.format("%s.json", indexKey));
        return StoreUtils.getJsonArray(filePath);
    }

    /**
     * Delete fuzzed arguments
     */
    public void deleteFuzzedArguments() {
        StoreUtils.deleteStore(getArgumentsFolder());
    }

    // STATIC GENERAL METHODS
    // *****************************************

    /**
     * Extract and store primitive arguments for a dataset.
     * @param javaFiles - List of paths fo java files.
     * @param dataset - Name of the dataset.
     */
    public static void extractAndStorePrimitiveArguments(List<String> javaFiles, String dataset) {
        LOGGER.info(String.format("Number of java files: %d", javaFiles.size()));
        ConstantAdapter adapter;
        Map<Primitive, Set<Object>> constantsMap = new HashMap<>();
        Map<Primitive, Set<Object>> fileConstantsMap;
        for (String javaFile: javaFiles) {
            try {
                adapter = new ConstantAdapter(javaFile);
                fileConstantsMap = adapter.getConstantsMap();
                for(Primitive primitive: fileConstantsMap.keySet()) {
                    Set<Object> values = new HashSet<>();
                    if (constantsMap.containsKey(primitive)) {
                        values = constantsMap.get(primitive);
                    }
                    values.addAll(fileConstantsMap.get(primitive));
                    constantsMap.put(primitive, values);
                }
            } catch (Exception e) {
                LOGGER.severe(String.format("Failed to process : %s", javaFile));
                throw e;
            }
        }
        LOGGER.info("PRIOR TO SAVING !!!!");
        for (Primitive primitive: constantsMap.keySet()) {
            System.out.println(primitive + " : " + constantsMap.get(primitive).size());
        }
        ArgumentStore store = ArgumentStore.loadArgumentStore(dataset);
        store.savePrimitiveArguments(constantsMap);
        constantsMap = store.loadPrimitiveArguments();
        LOGGER.info("====================");
        LOGGER.info("POST SAVING !!!!");
        for (Primitive primitive: constantsMap.keySet()) {
            System.out.println(primitive + " : " + constantsMap.get(primitive).size());
        }
    }

    /**
     * Generate arguments and save for the java file.
     * @param dataset - Name of the dataset.
     * @param javaFile - Path of the java file.
     */
    public static void generateForJavaFile(String javaFile, String dataset) {
        ArgumentStore store = ArgumentStore.loadArgumentStore(dataset);
        ClassMethods classMethods = new ClassMethods(dataset, javaFile);
        for (Method method: classMethods.getMethods()) {
            Function function = new Function(dataset, method, classMethods.getMethodBodies().get(method.getName()));
            if (!function.isValidArgs())
                continue;
            String key = function.makeArgumentsKey();
            if (!store.fuzzedKeyExists(key)) {
                LOGGER.info(String.format("Storing Key: %s", key));
                List<Object> arguments = ArgumentGenerator.generateArgumentsForFunction(dataset, function);
                if (arguments != null)
                    store.saveFuzzedArguments(key, arguments);
            }
        }
    }

    /**
     * Store fuzzed arguments for list of java files and dataset
     * @param javaFiles - List of path of java files
     * @param dataset - Name of dataset
     */
    public static void storeFuzzedArguments(List<String> javaFiles, String dataset) {
        ArgumentStore.loadArgumentStore(dataset).deleteFuzzedArguments();
        LOGGER.info("Generating random args. Here we go ....");
        for (String javaFile: javaFiles) {
            LOGGER.info(String.format("Running for %s", javaFile));
            generateForJavaFile(javaFile, dataset);
        }
    }
}
