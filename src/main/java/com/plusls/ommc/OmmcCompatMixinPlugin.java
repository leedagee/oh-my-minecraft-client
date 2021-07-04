package com.plusls.ommc;

import com.plusls.ommc.compat.CustomDepPredicate;
import com.plusls.ommc.compat.Dependencies;
import com.plusls.ommc.compat.NeedObfuscate;
import com.plusls.ommc.util.YarnUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.util.version.VersionPredicateParser;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class OmmcCompatMixinPlugin extends OmmcMixinPlugin {

    private final List<String> obfuscatedMixinList = new ArrayList<>();
    static private Path tempDirectory;

    static {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            try {
                tempDirectory = createTempDirectory();
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalStateException("Cannot create temp directory.");
            }
        }
    }
    // private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ClassNode loadClassNode(String className) {
        ClassNode classNode;
        try {
            classNode = MixinService.getService().getBytecodeProvider().getClassNode(className);
        } catch (ClassNotFoundException | IOException e) {
            throw new IllegalStateException(String.format("load ClassNode: {} fail.", className));
        }
        return classNode;
    }

    public static Path createTempDirectory() throws IOException {
        final Path tmp = Files.createTempDirectory(String.format("%s-", ModInfo.MOD_ID));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                FileUtils.forceDelete(tmp.toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        return tmp;
    }

    private static boolean getRemap(AnnotationNode annotationNode, boolean defaultValue) {
        Boolean ret = Annotations.getValue(annotationNode, "remap");
        if (ret == null) {
            return defaultValue;
        }
        return ret;
    }

    private final static List<String> NAME_LIST = Arrays.asList("method", "target");

    private static void obfuscateAnnotation(AnnotationNode annotationNode, boolean defaultRemap) {
        boolean remap = getRemap(annotationNode, defaultRemap);
        for (int i = 0; i < annotationNode.values.size(); i += 2) {
            if (annotationNode.values.get(i + 1) instanceof AnnotationNode subAnnotationNode) {
                obfuscateAnnotation(subAnnotationNode, remap);
            } else if (!defaultRemap) {
                String name = (String) annotationNode.values.get(i);
                if (NAME_LIST.contains(name) && annotationNode.values.get(i + 1) instanceof String str) {
                    annotationNode.values.set(i + 1, YarnUtil.obfuscateString(str));
                }
            }
        }
    }

    private static void obfuscateAnnotation(ClassNode classNode, Path outputDirectory) throws IOException {
        String fullClassName = classNode.name;
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        boolean classRemap = getRemap(Annotations.getInvisible(classNode, Mixin.class), true);

        for (MethodNode method : classNode.methods) {
            if (method.visibleAnnotations != null) {
                for (AnnotationNode annotationNode : method.visibleAnnotations) {
                    obfuscateAnnotation(annotationNode, classRemap);
                }
            }
        }
        classNode.accept(classWriter);

        int packageNameIdx = fullClassName.lastIndexOf('/');
        String packageName, className;
        if (packageNameIdx == -1) {
            packageName = "";
            className = fullClassName;
        } else {
            packageName = fullClassName.substring(0, packageNameIdx);
            className = fullClassName.substring(packageNameIdx + 1);
        }
        classNode.invisibleAnnotations.remove(Annotations.getInvisible(classNode, NeedObfuscate.class));
        Files.createDirectories(Paths.get(outputDirectory.toString(), packageName));
        Files.write(Paths.get(outputDirectory.toString(), packageName, className + "Obfuscated.class"), classWriter.toByteArray());
    }

    @Override
    public void onLoad(String mixinPackage) {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {


            Object urlLoader = Thread.currentThread().getContextClassLoader();
            Class<?> knotClassLoader;
            try {
                knotClassLoader = Class.forName("net.fabricmc.loader.launch.knot.KnotClassLoader");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new IllegalStateException("Cannot load class: net.fabricmc.loader.launch.knot.KnotClassLoader");
            }

            try {
                Method method = knotClassLoader.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(urlLoader, tempDirectory.toUri().toURL());
            } catch (MalformedURLException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                e.printStackTrace();
                throw new IllegalStateException("Cannot add custom class path to KnotClassLoader");
            }
            // obfuscateClasses(mixinPackage);
        }
    }

//    private void obfuscateClasses(String mixinPackage) {
//        Optional<ModContainer> modContainerOptional = FabricLoader.getInstance().getModContainer(ModInfo.MOD_ID);
//        assert modContainerOptional.isPresent();
//        ModContainer modContainer = modContainerOptional.get();
//        LoaderModMetadata metadata = (LoaderModMetadata) modContainer.getMetadata();
//        if (metadata.getId().equals(ModInfo.MOD_ID)) {
//            Collection<String> mixinConfigs = metadata.getMixinConfigs(FabricLoader.getInstance().getEnvironmentType());
//            for (String mixinConfig : mixinConfigs) {
//                MixinConfig config;
//                try {
//                    InputStream inputStream = OmmcCompatMixinPlugin.class.getClassLoader().getResourceAsStream(mixinConfig);
//                    String jsonString = IOUtils.toString(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8);
//                    config = GSON.fromJson(jsonString, MixinConfig.class);
//                    inputStream.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    throw new IllegalStateException(String.format("Load MixinConfig:%s fail!", mixinConfig));
//                }
//                if (mixinPackage.equals(config.mixinPackage)) {
//                    if (config.mixinClasses != null) {
//                        for (String mixinClass : config.mixinClasses) {
//                            obfuscateClass(String.format("%s.%s", mixinPackage, mixinClass));
//                        }
//                    }
//                    if (config.mixinClassesClient != null && FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
//                        for (String mixinClass : config.mixinClassesClient) {
//                            obfuscateClass(String.format("%s.%s", mixinPackage, mixinClass));
//                        }
//                    } else if (config.mixinClassesServer != null && FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
//                        for (String mixinClass : config.mixinClassesServer) {
//                            obfuscateClass(String.format("%s.%s", mixinPackage, mixinClass));
//                        }
//                    }
//                    break;
//                }
//            }
//        }
//    }

    private void obfuscateClass(String classFullName) {
        ClassNode classNode = loadClassNode(classFullName);
        if (Annotations.getInvisible(classNode, NeedObfuscate.class) != null) {
            try {
                obfuscateAnnotation(classNode, tempDirectory);
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalStateException("obfuscateAnnotation fail!");
            }
            obfuscatedMixinList.add(String.format("%sObfuscated", classFullName.substring(classFullName.lastIndexOf('.') + 1)));
        }
    }

    private boolean checkDependency(String targerClassName, AnnotationNode dependency) {
        String modId = Annotations.getValue(dependency, "modId");
        Optional<ModContainer> modContainerOptional = FabricLoader.getInstance().getModContainer(modId);
        if (modContainerOptional.isPresent()) {
            ModContainer modContainer = modContainerOptional.get();
            List<String> versionList = Annotations.getValue(dependency, "version");
            try {
                for (String version : versionList) {
                    // not work in fabric-loader 0.12
                    if (!VersionPredicateParser.matches(modContainer.getMetadata().getVersion(), version)) {
                        return false;
                    }
                }
            } catch (VersionParsingException e) {
                e.printStackTrace();
                throw new IllegalStateException(String.format("VersionParsingException, modid=%s, version=%s", modId, versionList));
            }
            ClassNode targetClassNode = loadClassNode(targerClassName);
            List<Type> predicateList = Annotations.getValue(dependency, "predicate");
            if (predicateList != null) {
                for (Type predicateType : predicateList) {
                    try {
                        CustomDepPredicate predicate = Class.forName(predicateType.getClassName()).asSubclass(CustomDepPredicate.class).getDeclaredConstructor().newInstance();
                        if (!predicate.test(targetClassNode)) {
                            return false;
                        }

                    } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                        e.printStackTrace();
                        ModInfo.LOGGER.warn("fuckyou");
                        throw new IllegalStateException("get CustomDepPredicate fail!");
                    }

                }
            }
            return true;
        }
        return false;
    }

    public boolean checkDependencies(ClassNode mixinClassNode, String targetClassName) {
        AnnotationNode dependencies = Annotations.getInvisible(mixinClassNode, Dependencies.class);
        if (Annotations.getInvisible(mixinClassNode, Dependencies.class) != null) {
            List<AnnotationNode> dependencyArray = Annotations.getValue(dependencies, "dependencyList");
            for (AnnotationNode dependency : dependencyArray) {
                if (!checkDependency(targetClassName, dependency)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        ClassNode mixinClassNode = loadClassNode(mixinClassName);
        if (!checkDependencies(mixinClassNode, targetClassName)) {
            return false;
        }

        if (tempDirectory != null && Annotations.getInvisible(mixinClassNode, NeedObfuscate.class) != null) {
            obfuscateClass(mixinClassName);
            return false;
        }
        return true;
    }

    @Override
    public List<String> getMixins() {
        return obfuscatedMixinList;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }
}
