package com.ray.router.gradle

import java.util.jar.JarEntry
import java.util.jar.JarFile

class RouterMappingCollector {

    String PACKAGE_NAME

    {
        PACKAGE_NAME = "com" + File.separator + "ray" + File.separator + "rayrouter" + File.separator + "mapping" + File.separator
    }

    Set<String> routerMappingClassNames = new HashSet<>()

    static final String CLASS_NAME_PREFIX = "RouterMapping_"
    static final String CLASS_FILE_SUFFIX = ".class"

    void collect(File classFile) {
        if (classFile == null || !classFile.exists())
            return
        if (classFile.isDirectory()) {
            classFile.listFiles().each {
                collect(it)
            }
        } else {
            println(classFile.absolutePath)
            if (classFile.absolutePath.contains(PACKAGE_NAME)
                    && classFile.name.startsWith(CLASS_NAME_PREFIX)
                    && classFile.name.endsWith(CLASS_FILE_SUFFIX)) {
                String className = classFile.name.replace(CLASS_FILE_SUFFIX, "")
                routerMappingClassNames.add(className)
            }
        }
    }


    void collectJar(File jarFile) {
        if (jarFile == null || !jarFile.exists())
            return
        Enumeration<JarEntry> entryEnumeration = new JarFile(jarFile).entries()
        while (entryEnumeration.hasMoreElements()) {
            JarEntry entry = entryEnumeration.nextElement()
            String entryName = entry.getName()
            if (entryName.contains(PACKAGE_NAME)
                    && entryName.contains(CLASS_NAME_PREFIX)
                    && entryName.contains(CLASS_FILE_SUFFIX)) {
                String className = entryName.replace(PACKAGE_NAME, "")
                        .replace(CLASS_FILE_SUFFIX, "")
                routerMappingClassNames.add(className)
            }
        }
    }

}