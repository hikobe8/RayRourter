package com.ray.router.processor;

import com.google.auto.service.AutoService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ray.router.annotations.Autowired;
import com.ray.router.annotations.Destination;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

@AutoService(Processor.class)
public class RouterAnnotationsProcessor extends AbstractProcessor {

    private final static String TAG = "DestinationProcessor";
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
        elementTool = processingEnv.getElementUtils();
        mTypeUtil = processingEnv.getTypeUtils();
        generateAutowiredClasses(roundEnvironment);
        generateRouterMappingFilesAndWiki(roundEnvironment);
        return false;
    }

    /**
     * 生成Router映射文件和文档
     *
     * @param roundEnvironment
     */
    private void generateRouterMappingFilesAndWiki(RoundEnvironment roundEnvironment) {
        Set<? extends Element> allDestinationElements =
                roundEnvironment.getElementsAnnotatedWith(Destination.class);
        if (allDestinationElements.size() > 0) {
            String rootDir = processingEnv.getOptions().get("root-project-dir");
            String mappingClassName = "RouterMapping_" + System.currentTimeMillis();
            MethodSpec.Builder builder = MethodSpec.methodBuilder("get")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(ParameterizedTypeName.get(Map.class, String.class, String.class))
                    .addStatement("Map<String, String> mapping = new $T<>()", HashMap.class);

            //文档json数组
            final JsonArray destinationJsonArray = new JsonArray();
            for (Element destinationElement : allDestinationElements) {
                Destination annotation = destinationElement.getAnnotation(Destination.class);
                if (null == annotation) {
                    continue;
                }
                String url = annotation.url();
                String description = annotation.description();
                String realPath = ((TypeElement) destinationElement).getQualifiedName().toString();
                builder.addStatement("mapping.put($S, $S)", url, realPath);

                //生成文档json对象
                JsonObject item = new JsonObject();
                item.addProperty("url", url);
                item.addProperty("realPath", realPath);
                item.addProperty("description", description);
                destinationJsonArray.add(item);
            }
            builder.addStatement("return mapping");
            MethodSpec get = builder.build();

            TypeSpec mapping = TypeSpec.classBuilder(mappingClassName)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addJavadoc("This file is generated by RayRouter, do not modify!!!")
                    .addMethod(get)
                    .build();

            writeJavaPoet2JavaFile(mapping, "com.ray.rayrouter.mapping");
            //生成wiki json
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
        }
    }

    private void generateAutowiredClasses(RoundEnvironment roundEnvironment) {
        Set<? extends Element> allAutowiredElements =
                roundEnvironment.getElementsAnnotatedWith(Autowired.class);
        if (allAutowiredElements.size() < 1)
            return;
        //类 - 被注解的成员变量
        Map<TypeElement, List<Element>> classFieldMap = new HashMap<>();
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
        for (TypeElement typeElement : classFieldMap.keySet()) {
            writeAutowiredJavaFile(typeElement, classFieldMap.get(typeElement));
        }
    }

    private void writeAutowiredJavaFile(TypeElement typeElement, List<Element> autowiredList) {
        ClassName targetClassName = ClassName.get(typeElement);
        ClassName androidIntentClassName = ClassName.get("android.content", "Intent");
        MethodSpec.Builder getMethodBuilder = MethodSpec.methodBuilder("inject")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(void.class)
                .addParameter(Object.class, "target")
                .addStatement("$T realTarget = ($T) target", targetClassName, targetClassName)
                .addStatement("$T realTargetIntent = realTarget.getIntent()", androidIntentClassName);
        for (Element autowired : autowiredList) {
            VariableElement autowiredElement = (VariableElement) autowired;
            Autowired autowiredAnnotation = autowiredElement.getAnnotation(Autowired.class);
            CharSequence autowiredElementSimpleName = autowiredAnnotation.name().trim().isEmpty() ? autowiredElement.getSimpleName() : autowiredAnnotation.name();
            boolean required = autowiredAnnotation.required();
            if (required) {
                //boom!!! add fatal code!
                getMethodBuilder.beginControlFlow("if (!realTargetIntent.getExtras().containsKey($S))", autowiredElementSimpleName);
                getMethodBuilder.addStatement("throw new $T($S)", RuntimeException.class, String.format("Intent extra: %s is required!", autowiredElementSimpleName));
                getMethodBuilder.endControlFlow();
            }
            TypeKind kind = autowiredElement.asType().getKind();
            if (kind.isPrimitive()) {
                switch (kind.ordinal()) {
                    case 0: //boolean
                        getMethodBuilder.addStatement("realTarget.$N = realTargetIntent.getBooleanExtra($S, false)", autowiredElementSimpleName, autowiredElementSimpleName);
                        break;
                    case 3: //int
                        getMethodBuilder.addStatement("realTarget.$N = realTargetIntent.getIntExtra($S, -1)", autowiredElementSimpleName, autowiredElementSimpleName);
                        break;
                    case 4: //long
                        getMethodBuilder.addStatement("realTarget.$N = realTargetIntent.getLongExtra($S, -1L)", autowiredElementSimpleName, autowiredElementSimpleName);
                        break;
                    case 6: //float
                        getMethodBuilder.addStatement("realTarget.$N = realTargetIntent.getFloatExtra($S, -1f)", autowiredElementSimpleName, autowiredElementSimpleName);
                        break;
                    case 7: //double
                        getMethodBuilder.addStatement("realTarget.$N = realTargetIntent.getDoubleExtra($S, -1)", autowiredElementSimpleName, autowiredElementSimpleName);
                        break;
                    default:
                        throw new RuntimeException("暂不支持的类型 ：" + kind + ", 请使用Bundle");
                }
            } else {
                String typeName = autowiredElement.asType().toString();
                if ("java.lang.String".equals(typeName)) {
                    getMethodBuilder.addStatement("realTarget.$N = realTargetIntent.getStringExtra($S)", autowiredElementSimpleName, autowiredElementSimpleName);
                } else if ("android.os.Bundle".equals(typeName)) {
                    getMethodBuilder.addStatement("realTarget.$N = realTargetIntent.getBundleExtra($S)", autowiredElementSimpleName, autowiredElementSimpleName);
                } else {
                    // 判断是否为 Parcelable 类型
                    String PARCELABLE = "android.os.Parcelable";
                    TypeMirror parcelableType = elementTool.getTypeElement(PARCELABLE).asType();
                    if (mTypeUtil.isSubtype(autowiredElement.asType(), parcelableType)) {
                        //Parcelable
                        getMethodBuilder.addStatement("realTarget.$N = realTargetIntent.getParcelableExtra($S)", autowiredElementSimpleName, autowiredElementSimpleName);
                    } else {
                        throw new RuntimeException("暂不支持的类型 ：" + kind + ", 请使用Bundle");
                    }
                }
            }

        }

        MethodSpec injectMethod = getMethodBuilder.build();
        String javaClassName = typeElement.getSimpleName() + "_Autowired";
        TypeSpec autowiredInjector = TypeSpec.classBuilder(javaClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("This file is generated by RayRouter, do not modify!!!")
                .addMethod(injectMethod)
                .build();

        writeJavaPoet2JavaFile(autowiredInjector, targetClassName.packageName());
    }

    private void writeJavaPoet2JavaFile(TypeSpec targetClazz, String packageName) {
        JavaFile javaFile = JavaFile.builder(packageName, targetClazz)
                .build();
        Writer writer = null;
        try {
            JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(packageName + "." + targetClazz.name);
            javaFile.writeTo(System.out);
            writer = sourceFile.openWriter();
            writer.write(javaFile.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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
