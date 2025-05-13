package net.me.mappings;

import net.me.Main;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TinyMappings {

    // namedClassName -> intermediaryClassName
    private final Map<String, String> namedToIntermediaryClass = new HashMap<>();
    // intermediaryClassName -> namedClassName
    private final Map<String, String> intermediaryToNamedClass = new HashMap<>();
    // officialClassName -> intermediaryClassName
    private final Map<String, String> officialToIntermediaryClass = new HashMap<>();
    // officialClassName -> namedClassName
    private final Map<String, String> officialToNamedClass = new HashMap<>();

    // ownerNamedClassName -> (namedFieldName -> intermediaryFieldName)
    private final Map<String, Map<String, String>> namedClassFieldsToIntermediary = new HashMap<>();
    // ownerIntermediaryClassName -> (intermediaryFieldName -> namedFieldName)
    private final Map<String, Map<String, String>> intermediaryClassFieldsToNamed = new HashMap<>();


    // ownerNamedClassName -> (MethodSignature(named) -> intermediaryMethodName)
    private final Map<String, Map<MethodSignature, String>> namedClassMethodsToIntermediary = new HashMap<>();
    // ownerIntermediaryClassName -> (MethodSignature(intermediary) -> namedMethodName)
    private final Map<String, Map<MethodSignature, String>> intermediaryClassMethodsToNamed = new HashMap<>();

    // officialOwnerClassName -> officialMemberName -> officialDescriptor
    private final Map<String, Map<String, String>> officialFieldDescriptors = new HashMap<>();
    private final Map<String, Map<String, String>> officialMethodDescriptors = new HashMap<>(); // clé: officialName + officialDescriptor


    // Regex pour parser les descripteurs de méthode JVM et les types de champ
    private static final Pattern JVM_METHOD_DESCRIPTOR_PATTERN = Pattern.compile("\\(([^)]*)\\)(.*)");
    // Ce pattern est plus simple et vise à capturer les types de classe ou les types primitifs/tableaux.
    private static final Pattern JVM_TYPE_CAPTURE_PATTERN = Pattern.compile("(\\[*L[^;]+;|\\[*[BCDFIJSZ])");


    public TinyMappings(Path tinyFilePath) throws IOException {
        parse(tinyFilePath);
    }

    private void parse(Path tinyFilePath) throws IOException {
        Main.LOGGER.info("Parsing mappings from: {}", tinyFilePath);
        try (BufferedReader reader = Files.newBufferedReader(tinyFilePath)) {
            String line = reader.readLine();
            if (line == null || !line.startsWith("v1\tofficial\tintermediary\tnamed")) {
                throw new IOException("Invalid tiny v1 file header: " + line);
            }

            String currentOfficialClassRaw = null;
            String currentIntermediaryClass = null;
            String currentNamedClass = null;

            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.startsWith("#") || line.trim().isEmpty()) continue;

                String[] parts = line.split("\t");
                if (parts.length < 1) continue;
                String type = parts[0];

                try {
                    if ("CLASS".equals(type)) {
                        if (parts.length < 4) throw new IOException("Malformed CLASS line");
                        currentOfficialClassRaw = parts[1];
                        currentIntermediaryClass = parts[2].replace('/', '.');
                        currentNamedClass = parts[3].replace('/', '.');

                        namedToIntermediaryClass.put(currentNamedClass, currentIntermediaryClass);
                        intermediaryToNamedClass.put(currentIntermediaryClass, currentNamedClass);
                        officialToIntermediaryClass.put(currentOfficialClassRaw.replace('/','.'), currentIntermediaryClass);
                        officialToNamedClass.put(currentOfficialClassRaw.replace('/','.'), currentNamedClass);

                    } else if ("FIELD".equals(type)) {
                        if (parts.length < 6) throw new IOException("Malformed FIELD line");
                        if (currentNamedClass == null || currentIntermediaryClass == null || currentOfficialClassRaw == null) {
                            Main.LOGGER.warn("FIELD entry found before CLASS context at line {}", lineNum);
                            continue;
                        }
                        // FIELD official_owner_class official_desc official_name intermediary_name named_name
                        String officialOwnerRaw = parts[1];
                        String officialDescriptor = parts[2];
                        String officialFieldName = parts[3];
                        String intermediaryFieldName = parts[4];
                        String namedFieldName = parts[5];

                        if (officialOwnerRaw.equals(currentOfficialClassRaw)) {
                            namedClassFieldsToIntermediary.computeIfAbsent(currentNamedClass, k -> new HashMap<>())
                                    .put(namedFieldName, intermediaryFieldName);
                            intermediaryClassFieldsToNamed.computeIfAbsent(currentIntermediaryClass, k -> new HashMap<>())
                                    .put(intermediaryFieldName, namedFieldName);
                            officialFieldDescriptors.computeIfAbsent(currentOfficialClassRaw.replace('/','.'), k -> new HashMap<>())
                                    .put(officialFieldName, officialDescriptor);
                        }

                    } else if ("METHOD".equals(type)) {
                        if (parts.length < 6) throw new IOException("Malformed METHOD line");
                        if (currentNamedClass == null || currentIntermediaryClass == null || currentOfficialClassRaw == null) {
                            Main.LOGGER.warn("METHOD entry found before CLASS context at line {}", lineNum);
                            continue;
                        }
                        // METHOD official_owner_class official_desc official_name intermediary_name named_name
                        String officialOwnerRaw = parts[1];
                        String officialDescriptor = parts[2];
                        String officialMethodName = parts[3];
                        String intermediaryMethodName = parts[4];
                        String namedMethodName = parts[5];

                        if (officialOwnerRaw.equals(currentOfficialClassRaw)) {
                            String namedDescriptor = convertOfficialDescriptorToTarget(officialDescriptor, officialToNamedClass);
                            String intermediaryDescriptor = convertOfficialDescriptorToTarget(officialDescriptor, officialToIntermediaryClass);

                            MethodSignature namedSig = new MethodSignature(namedMethodName, namedDescriptor);
                            MethodSignature intermediarySig = new MethodSignature(intermediaryMethodName, intermediaryDescriptor);

                            namedClassMethodsToIntermediary.computeIfAbsent(currentNamedClass, k -> new HashMap<>())
                                    .put(namedSig, intermediaryMethodName);
                            intermediaryClassMethodsToNamed.computeIfAbsent(currentIntermediaryClass, k -> new HashMap<>())
                                    .put(intermediarySig, namedMethodName);
                            officialMethodDescriptors.computeIfAbsent(currentOfficialClassRaw.replace('/','.'), k->new HashMap<>())
                                    .put(officialMethodName + officialDescriptor, officialDescriptor); // Clé unique
                        }
                    }
                } catch (Exception e) {
                    Main.LOGGER.error("Error parsing tiny mapping line {}: {}", lineNum, line, e);
                }
            }
        }
        Main.LOGGER.info("Finished parsing mappings. {} classes, {} named->inter field maps, {} named->inter method maps.",
                namedToIntermediaryClass.size(), namedClassFieldsToIntermediary.size(), namedClassMethodsToIntermediary.size());
    }


    public String convertOfficialDescriptorToNamed(String officialDescriptor) {
        return convertOfficialDescriptorToTarget(officialDescriptor, officialToNamedClass);
    }

    public String convertOfficialDescriptorToIntermediary(String officialDescriptor) {
        return convertOfficialDescriptorToTarget(officialDescriptor, officialToIntermediaryClass);
    }

    /**
     * Converts a JVM descriptor (field or method) from official class names to target class names.
     * @param officialDescriptor The descriptor using official (potentially obfuscated) class names.
     * @param officialToTargetClassMap A map from official class name (e.g., "a/b/C") to target class name (e.g., "net/minecraft/util/MyClass").
     *                                 Class names in the map should use '.' as separator.
     * @return The descriptor with target class names.
     */
    private String convertOfficialDescriptorToTarget(String officialDescriptor, Map<String, String> officialToTargetClassMap) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = JVM_TYPE_CAPTURE_PATTERN.matcher(officialDescriptor);
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(officialDescriptor, lastEnd, matcher.start()); // Append non-type parts (like '(', ')', ';')

            String jvmType = matcher.group(1);
            String prefix = "";
            String baseType = jvmType;

            // Handle array prefixes
            int arrayDepth = 0;
            while (baseType.startsWith("[")) {
                arrayDepth++;
                baseType = baseType.substring(1);
            }
            prefix = String.join("", Collections.nCopies(arrayDepth, "["));

            if (baseType.startsWith("L") && baseType.endsWith(";")) {
                String officialClassNameWithSlashes = baseType.substring(1, baseType.length() - 1);
                String officialClassNameWithDots = officialClassNameWithSlashes.replace('/', '.');
                String targetClassNameWithDots = officialToTargetClassMap.get(officialClassNameWithDots);

                if (targetClassNameWithDots != null) {
                    result.append(prefix).append("L").append(targetClassNameWithDots.replace('.', '/')).append(";");
                } else {
                    // Class not in mappings (e.g., java.lang.String), use original
                    result.append(prefix).append("L").append(officialClassNameWithSlashes).append(";");
                }
            } else {
                // Primitive type, already correct
                result.append(prefix).append(baseType);
            }
            lastEnd = matcher.end();
        }
        result.append(officialDescriptor.substring(lastEnd)); // Append any trailing characters (e.g. method return type if not an L-type)

        return result.toString();
    }


    // --- Getters pour MappingsResolver ---

    public String getIntermediaryClassName(String namedClassName) {
        return namedToIntermediaryClass.get(namedClassName);
    }

    public String getNamedClassName(String intermediaryClassName) {
        return intermediaryToNamedClass.get(intermediaryClassName);
    }

    public String getIntermediaryFieldName(String namedOwnerClassName, String namedFieldName) {
        Map<String, String> fields = namedClassFieldsToIntermediary.get(namedOwnerClassName);
        return (fields != null) ? fields.get(namedFieldName) : null;
    }

    public String getIntermediaryMethodName(String namedOwnerClassName, String namedMethodName, String namedMethodDescriptor) {
        Map<MethodSignature, String> methods = namedClassMethodsToIntermediary.get(namedOwnerClassName);
        if (methods == null) return null;
        return methods.get(new MethodSignature(namedMethodName, namedMethodDescriptor));
    }

    // Utilisé par TypeScriptDefinitionGenerator ou des outils similaires
    public List<String> getAllNamedClasses() {
        return new ArrayList<>(namedToIntermediaryClass.keySet());
    }

    public Map<String, String> getFieldsForNamedClass(String namedClassName) {
        return namedClassFieldsToIntermediary.getOrDefault(namedClassName, Collections.emptyMap());
    }

    public Map<MethodSignature, String> getMethodsForNamedClass(String namedClassName) {
        return namedClassMethodsToIntermediary.getOrDefault(namedClassName, Collections.emptyMap());
    }

    public String getOfficialClassName(String namedClassName) {
        // Inverse de officialToNamedClass
        for (Map.Entry<String, String> entry : officialToNamedClass.entrySet()) {
            if (Objects.equals(entry.getValue(), namedClassName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public String getOfficialFieldName(String namedOwnerClassName, String namedFieldName) {
        String officialOwner = getOfficialClassName(namedOwnerClassName);
        if (officialOwner == null) return null;
        String intermediaryField = getIntermediaryFieldName(namedOwnerClassName, namedFieldName);
        if (intermediaryField == null) return null;

        // Nécessite une map inverse ou une logique plus complexe pour official -> intermediary -> named
        // Pour l'instant, on ne peut pas le déduire proprement sans plus de stockage.
        // Cette méthode est un placeholder et doit être revue si vous avez besoin de mapper named -> official pour les champs.
        // Le plus simple serait de stocker official->intermediary->named pour les champs aussi.
        return namedFieldName; // Placeholder incorrect
    }

    public String getOfficialFieldDescriptor(String officialOwnerClassName, String officialFieldName) {
        return officialFieldDescriptors.getOrDefault(officialOwnerClassName, Collections.emptyMap()).get(officialFieldName);
    }


    public String getNamedDescriptor(Class<?> returnType, Class<?>... parameterTypes) {
        StringBuilder desc = new StringBuilder("(");
        for (Class<?> paramType : parameterTypes) {
            desc.append(classToDescriptorString(paramType, true));
        }
        desc.append(")");
        desc.append(classToDescriptorString(returnType, true));
        return desc.toString();
    }

    public String getIntermediaryDescriptor(Class<?> returnType, Class<?>... parameterTypes) {
        StringBuilder desc = new StringBuilder("(");
        for (Class<?> paramType : parameterTypes) {
            desc.append(classToDescriptorString(paramType, false));
        }
        desc.append(")");
        desc.append(classToDescriptorString(returnType, false));
        return desc.toString();
    }

    private String classToDescriptorString(Class<?> clazz, boolean useNamed) {
        if (clazz.isPrimitive()) {
            if (clazz == void.class) return "V";
            if (clazz == int.class) return "I";
            if (clazz == long.class) return "J";
            if (clazz == float.class) return "F";
            if (clazz == double.class) return "D";
            if (clazz == boolean.class) return "Z";
            if (clazz == byte.class) return "B";
            if (clazz == short.class) return "S";
            if (clazz == char.class) return "C";
        } else if (clazz.isArray()) {
            return "[" + classToDescriptorString(clazz.getComponentType(), useNamed);
        } else {
            String className = clazz.getName(); // ex: net.me.scripting.chain.ChainedAccessor
            String targetClassName;
            if (useNamed) {
                // Si c'est une classe non-Minecraft (ne commence pas par "net.minecraft"), on garde son nom.
                // Sinon, on s'attend à ce qu'elle soit dans nos mappings "named".
                targetClassName = className.startsWith("net.minecraft.") ? className : className;
            } else { // useIntermediary
                targetClassName = getIntermediaryClassName(className);
                if (targetClassName == null) targetClassName = className; // Fallback pour les classes non-Minecraft
            }
            return "L" + targetClassName.replace('.', '/') + ";";
        }
        throw new IllegalArgumentException("Impossible de convertir en descripteur : " + clazz);
    }

    // Record pour une identification unique des méthodes
    public record MethodSignature(String name, String descriptor) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodSignature that = (MethodSignature) o;
            return name.equals(that.name) && descriptor.equals(that.descriptor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, descriptor);
        }
    }
}