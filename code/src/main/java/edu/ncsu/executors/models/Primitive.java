package edu.ncsu.executors.models;

import java.util.*;

public enum Primitive {

    SHORT("short", "short", "Short", "java.lang.Short"),
    INTEGER("integer", "int", "Integer", "java.lang.Integer"),
    LONG("long", "long", "Long", "java.lang.Long"),
    CHARACTER("character", "char", "Character", "java.lang.Character"),
    FLOAT("float", "float", "Float", "java.lang.Float"),
    DOUBLE("double", "double", "Double", "java.lang.Double"),
    BOOLEAN("boolean", "boolean", "Boolean", "java.lang.Boolean"),
    BYTE("byte", "byte", "Byte", "java.lang.Byte"),
    STRING("string", "String", "java.lang.String");

    /**
     * Mapping types to Primitive Enum.
     */
    private final static Map<String, Primitive> typeToPrimitiveMap = new HashMap<>();

    /**
     * Mapping names to Primitive Enum.
     */
    private final static Map<String, Primitive> nameToPrimitiveMap = new HashMap<>();

    /**
     * Name of enum
     */
    private String name;

    /**
     * List of types corresponding to the primitive.
     */
    private List<String> types;

    /**
     * @return - Name of the Primitive.
     */
    public String getName() {
        return name;
    }

    /**
     * @return - Types for Primitive
     */
    public List<String> getTypes() {
        return types;
    }

    /**
     * @param type - Type as string
     * @return - Return Primitive Enum for the type
     */
    public static Primitive getPrimitive(String type) {
        return typeToPrimitiveMap.get(type);
    }

    /**
     * @param name - Name of primitive
     * @return - Return Primitive Enum for the name
     */
    public static Primitive getPrimitiveByName(String name) {
        return nameToPrimitiveMap.get(name);
    }

    /**
     * Create an instance of Primitive Enum
     * @param name - Name of the Primitive Enum
     * @param types - variable args of all the types
     */
    Primitive(String name, String... types) {
        this.name = name;
        this.types = new ArrayList<>(Arrays.asList(types));
    }

    static {
        for (Primitive dataType: Primitive.values()) {
            nameToPrimitiveMap.put(dataType.name, dataType);
            for (String className: dataType.getTypes())
                typeToPrimitiveMap.put(className, dataType);
        }
    }

    public static Object convertToArgument(Primitive primitive, String argString) {
        switch (primitive) {
            case SHORT:
                return Short.parseShort(argString);
            case INTEGER:
                return Integer.parseInt(argString);
            case LONG:
                return Long.parseLong(argString);
            case CHARACTER:
                return argString.charAt(0);
            case FLOAT:
                return Float.parseFloat(argString);
            case DOUBLE:
                return Double.parseDouble(argString);
            case BOOLEAN:
                return Boolean.parseBoolean(argString);
            case BYTE:
                return Byte.parseByte(argString);
            case STRING:
                return argString;
            default:
                throw new RuntimeException(String.format(
                        "Currently we do not support the class %s", primitive.getName()));
        }
    }
}
