package com.ray.router.processor;

import com.google.auto.service.AutoService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ray.router.annotations.Autowired;
import com.ray.router.annotations.Destination;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@AutoService(Processor.class)
public class DestinationProcessor extends AbstractProcessor {

    private final static String TAG = "DestinationProcessor";
    private Messager mMessager;
    private Elements elementTool;// 操作Element的工具类（类，函数，属性，其实都是Element）
    private Types mTypeUtil;

    /**
     * 编译器找到支持的注解之后会调用此方法
     *
     * @param set
     * @param roundEnvironment
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (roundEnvironment.processingOver()) {
            //避免多次调用
            return false;
        }
        mMessager = processingEnv.getMessager();
        elementTool = processingEnv.getElementUtils();
        mTypeUtil = processingEnv.getTypeUtils();
        generateAutowiredClass(roundEnvironment);

        Set<? extends Element> allDestinationElements =
                roundEnvironment.getElementsAnnotatedWith(Destination.class);
        if (allDestinationElements.size() < 1) {
            return false;
        }
        System.out.println(TAG + " >>>> process start...");
        System.out.println(TAG + " >>>> all Destination Elements count = " + allDestinationElements.size());

        String rootDir = processingEnv.getOptions().get("root-project-dir");


        StringBuilder stringBuilder = new StringBuilder();
        String mappingClassName = "RouterMapping_" + System.currentTimeMillis();
        stringBuilder.append("package com.ray.rayrouter.mapping;").append("\n\n");
        stringBuilder.append("import java.util.HashMap;").append("\n");
        stringBuilder.append("import java.util.Map;").append("\n\n");
        stringBuilder.append("public class ")
                .append(mappingClassName)
                .append(" {")
                .append("\n\n");
        stringBuilder.append("    public static Map<String, String> get() {").append("\n");
        stringBuilder.append("        Map<String, String> mapping = new HashMap<>();").append("\n");

        final JsonArray destinationJsonArray = new JsonArray();

        for (Element destinationElement : allDestinationElements) {
            TypeElement typeElement = (TypeElement) destinationElement;
            Destination annotation = typeElement.getAnnotation(Destination.class);
            if (null == annotation) {
                continue;
            }
            String url = annotation.url();
            String description = annotation.description();
            String realPath = typeElement.getQualifiedName().toString();
            stringBuilder.append("        mapping.put(\"")
                    .append(url).append("\",\"")
                    .append(realPath)
                    .append("\");")
                    .append("\n");
            System.out.println(TAG + " >>>> url = " + url);
            System.out.println(TAG + " >>>> description = " + description);
            System.out.println(TAG + " >>>> realPath = " + realPath);

            JsonObject item = new JsonObject();
            item.addProperty("url", url);
            item.addProperty("realPath", realPath);
            item.addProperty("description", description);
            destinationJsonArray.add(item);
        }

        stringBuilder.append("        return mapping;").append("\n");
        stringBuilder.append("    }").append("\n\n");
        stringBuilder.append("}");

        String mappingFullName = "com.ray.rayrouter.mapping." + mappingClassName;
        try {
            JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(mappingFullName);
            Writer writer = sourceFile.openWriter();
            writer.write(stringBuilder.toString());
            writer.flush();
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException("Error while creating router mapping file", e);
        }


        try {
            File rootFile = new File(rootDir);
            if (!rootFile.exists()) {
                throw new RuntimeException("root-project-dir doesn't exists");
            }
            File docDir = new File(rootDir, "router_mapping");
            if (!docDir.exists()) {
                docDir.mkdir();
            }
            File docFile = new File(docDir, "router_mapping_" + System.currentTimeMillis() + ".json");
            BufferedWriter outWriter = new BufferedWriter(new FileWriter(docFile));
            outWriter.write(destinationJsonArray.toString());
            outWriter.flush();
            outWriter.close();
        } catch (Exception e) {
            throw new RuntimeException("Error while create router mapping doc file", e);
        }

        System.out.println(TAG + " >>>> process end.");
        System.out.println(stringBuilder.toString());
        return false;
    }

    private void generateAutowiredClass(RoundEnvironment roundEnvironment) {
        Set<? extends Element> allAutowiredElements =
                roundEnvironment.getElementsAnnotatedWith(Autowired.class);
        if (allAutowiredElements.size() < 1)
            return;
        //类 - 被注解的成员变量
        Map<Element, List<Element>> classFieldMap = new HashMap<>();
        for (Element autowiredElement : allAutowiredElements) {
            TypeElement enclosingElement = (TypeElement) autowiredElement.getEnclosingElement();
            System.out.println("autowired class : " + enclosingElement);
            List<Element> autowiredList;
            if (classFieldMap.containsKey(enclosingElement)) {
                autowiredList = classFieldMap.get(enclosingElement);
                if (autowiredList == null) {
                    autowiredList = new ArrayList<>();
                } else {
                    classFieldMap.put(enclosingElement, autowiredList);
                }
            } else {
                autowiredList = new ArrayList<>();
                classFieldMap.put(enclosingElement, autowiredList);
            }
            autowiredList.add(autowiredElement);
        }
        for (Element typeElement : classFieldMap.keySet()) {
            writeAutowiredJavaFile(typeElement, classFieldMap.get(typeElement));
        }
    }

    private void writeAutowiredJavaFile(Element typeElement, List<Element> autowiredList) {
        ClassName targetClassName = ClassName.get((TypeElement) typeElement);
        MethodSpec.Builder injectMethodBuilder = MethodSpec.methodBuilder("inject")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class)
                .addParameter(Object.class, "target")
                .addStatement("$T realTarget = ($T) target", targetClassName, targetClassName)
                .addStatement("Intent realTargetIntent = realTarget.getIntent()");
        for (Element autowired : autowiredList) {
            VariableElement autowiredElement = (VariableElement) autowired;
            Autowired autowiredAnnotation = autowiredElement.getAnnotation(Autowired.class);
            TypeKind kind = autowiredElement.asType().getKind();
            Name autowiredElementSimpleName = autowiredElement.getSimpleName();
            if (kind.isPrimitive()) {
                switch (kind.ordinal()) {
                    case 0: //boolean
                        injectMethodBuilder.addStatement("realTarget.$N = realTargetIntent.getBooleanExtra($S, false)", autowiredElementSimpleName, autowiredElementSimpleName);
                        break;
                    case 3: //int
                        injectMethodBuilder.addStatement("realTarget.$N = realTargetIntent.getIntExtra($S, -1)", autowiredElementSimpleName, autowiredElementSimpleName);
                        break;
                    default:
                        mMessager.printMessage(Diagnostic.Kind.WARNING, "暂不支持的类型 ：" + kind + "\n");
                }
            } else {
                String typeName = autowiredElement.asType().toString();
                if ("java.lang.String".equals(typeName)) {
                    injectMethodBuilder.addStatement("realTarget.$N = realTargetIntent.getStringExtra($S)", autowiredElementSimpleName, autowiredElementSimpleName);
                } else {
                    // 判断是否为 Parcelable 类型
                    String PARCELABLE = "android.os.Parcelable";
                    TypeMirror parcelableType = elementTool.getTypeElement(PARCELABLE).asType();
                    if (mTypeUtil.isSubtype(autowiredElement.asType(), parcelableType)) {
                        //Parcelable
                        injectMethodBuilder.addStatement("realTarget.$N = realTargetIntent.getParcelableExtra($S)", autowiredElementSimpleName, autowiredElementSimpleName);
                    } else {
                        mMessager.printMessage(Diagnostic.Kind.WARNING, "暂不支持的类型 ：" + kind + "\n");
                    }
                }
            }

        }

        MethodSpec injectMethod = injectMethodBuilder.build();
        String javaClassName = typeElement.getSimpleName() + "$" + "AutoWired";
        TypeSpec helloWorld = TypeSpec.classBuilder(javaClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("This file is generated by RayRouter, do not modify!!!")
                .addMethod(injectMethod)
                .build();

        JavaFile javaFile = JavaFile.builder("com.ray.rayrouter.autowired", helloWorld)
                .build();

        try {
            javaFile.writeTo(System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return 当前注解处理器支持的注解类型
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> sat = new HashSet<>();
        sat.add(Destination.class.getCanonicalName());
        sat.add(Autowired.class.getCanonicalName());
        return sat;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_8;
    }
}
