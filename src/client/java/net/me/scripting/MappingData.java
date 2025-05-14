package net.me.scripting;


import net.fabricmc.loader.impl.lib.mappingio.tree.MappingTree;
import net.fabricmc.loader.impl.lib.mappingio.tree.MappingTreeView;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MappingData {
    // Yarn FQCN -> Official FQCN
    private final Map<String, String> classMappings = new HashMap<>();
    // Yarn Class FQCN -> { Yarn Method Signature (name+desc) -> Official Method Name }
    private final Map<String, Map<String, String>> methodMappings = new HashMap<>();
    // Yarn Class FQCN -> { Yarn Field Name -> Official Field Name }
    private final Map<String, Map<String, String>> fieldMappings = new HashMap<>();

    private final MappingTree mappingTree; // Keep the tree for descriptor mapping
    private final int namedNsId;
    private final int officialNsId;

    public MappingData(MappingTree tree, String namedNamespace, String officialNamespace) {
        this.mappingTree = tree;
        this.namedNsId = tree.getNamespaceId(namedNamespace);
        this.officialNsId = tree.getNamespaceId(officialNamespace);

        for (MappingTreeView.ClassMappingView classView : tree.getClasses()) {
            String yarnClassName = classView.getName(namedNsId);
            String officialClassName = classView.getName(officialNsId);

            if (yarnClassName != null && officialClassName != null) {
                classMappings.put(yarnClassName.replace('/', '.'), officialClassName.replace('/', '.'));

                Map<String, String> methods = new HashMap<>();
                for (MappingTreeView.MethodMappingView methodView : classView.getMethods()) {
                    String yarnMethodName = methodView.getName(namedNsId);
                    String yarnMethodDesc = methodView.getDesc(namedNsId);
                    String officialMethodName = methodView.getName(officialNsId);
                    if (yarnMethodName != null && yarnMethodDesc != null && officialMethodName != null) {
                        methods.put(yarnMethodName + yarnMethodDesc, officialMethodName);
                    }
                }
                methodMappings.put(yarnClassName.replace('/', '.'), methods);

                Map<String, String> fields = new HashMap<>();
                for (MappingTreeView.FieldMappingView fieldView : classView.getFields()) {
                    String yarnFieldName = fieldView.getName(namedNsId);
                    String officialFieldName = fieldView.getName(officialNsId);
                    if (yarnFieldName != null && officialFieldName != null) {
                        fields.put(yarnFieldName, officialFieldName);
                    }
                }
                fieldMappings.put(yarnClassName.replace('/', '.'), fields);
            }
        }
    }

    public Optional<String> getOfficialClassName(String yarnClassName) {
        return Optional.ofNullable(classMappings.get(yarnClassName));
    }

    public Optional<String> getOfficialMethodName(String yarnClassName, String yarnMethodName, String yarnMethodDesc) {
        // Map the descriptor from Yarn to Official first for lookup
        String officialYarnDesc = mappingTree.mapDesc(yarnMethodDesc, namedNsId, officialNsId);
        String yarnFullSignature = yarnMethodName + yarnMethodDesc; // Key is Yarn name + Yarn desc

        return Optional.ofNullable(methodMappings.get(yarnClassName))
                .flatMap(methods -> Optional.ofNullable(methods.get(yarnFullSignature)));
    }

    public Optional<String> getOfficialFieldName(String yarnClassName, String yarnFieldName) {
        return Optional.ofNullable(fieldMappings.get(yarnClassName))
                .flatMap(fields -> Optional.ofNullable(fields.get(yarnFieldName)));
    }

    public String mapDescriptorToOfficial(String yarnDesc) {
        return mappingTree.mapDesc(yarnDesc, namedNsId, officialNsId);
    }
     public String mapDescriptorToNamed(String officialDesc) {
        return mappingTree.mapDesc(officialDesc, officialNsId, namedNsId);
    }

    public MappingTree getMappingTree() {
        return mappingTree;
    }

    public int getNamedNsId() {
        return namedNsId;
    }

    public int getOfficialNsId() {
        return officialNsId;
    }
}
