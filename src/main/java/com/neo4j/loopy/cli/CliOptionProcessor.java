package com.neo4j.loopy.cli;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Annotation processor that generates Picocli CLI options from @CliOption annotations.
 * Scans LoopyConfig class and generates a corresponding CLI options class.
 */
@SupportedAnnotationTypes("com.neo4j.loopy.cli.CliOption")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class CliOptionProcessor extends AbstractProcessor {
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }
        
        try {
            generateCliOptionsClass(roundEnv);
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR, 
                "Failed to generate CLI options class: " + e.getMessage()
            );
        }
        
        return true;
    }
    
    private void generateCliOptionsClass(RoundEnvironment roundEnv) throws IOException {
        List<FieldInfo> fields = collectAnnotatedFields(roundEnv);
        if (fields.isEmpty()) {
            return;
        }
        
        JavaFileObject builderFile = processingEnv.getFiler()
            .createSourceFile("com.neo4j.loopy.cli.GeneratedCliOptions");
            
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            writeCliOptionsClass(out, fields);
        }
    }
    
    private List<FieldInfo> collectAnnotatedFields(RoundEnvironment roundEnv) {
        List<FieldInfo> fields = new ArrayList<>();
        
        for (Element element : roundEnv.getElementsAnnotatedWith(CliOption.class)) {
            if (element.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) element;
                CliOption annotation = field.getAnnotation(CliOption.class);
                
                FieldInfo fieldInfo = new FieldInfo(
                    field.getSimpleName().toString(),
                    field.asType().toString(),
                    annotation
                );
                fields.add(fieldInfo);
            }
        }
        
        return fields;
    }
    
    private void writeCliOptionsClass(PrintWriter out, List<FieldInfo> fields) {
        out.println("package com.neo4j.loopy.cli;");
        out.println();
        out.println("import picocli.CommandLine.Option;");
        out.println("import java.util.List;");
        out.println();
        out.println("/**");
        out.println(" * Auto-generated CLI options class from @CliOption annotations.");
        out.println(" * This class is generated at compile-time by CliOptionProcessor.");
        out.println(" */");
        out.println("public class GeneratedCliOptions {");
        out.println();
        
        for (FieldInfo field : fields) {
            writeFieldOption(out, field);
            out.println();
        }
        
        // Generate getters
        out.println("    // Getters");
        for (FieldInfo field : fields) {
            writeGetter(out, field);
        }
        
        out.println("}");
    }
    
    private void writeFieldOption(PrintWriter out, FieldInfo field) {
        out.println("    @Option(");
        out.println("        names = {" + formatNames(field.annotation.names()) + "},");
        out.println("        description = \"" + field.annotation.description() + "\"");
        if (field.annotation.required()) {
            out.println("        , required = true");
        }
        if (field.annotation.hidden()) {
            out.println("        , hidden = true");
        }
        out.println("    )");
        
        String fieldType = mapJavaTypeToSimple(field.type);
        out.println("    private " + fieldType + " " + field.name + ";");
    }
    
    private void writeGetter(PrintWriter out, FieldInfo field) {
        String methodName = "get" + capitalize(field.name);
        String fieldType = mapJavaTypeToSimple(field.type);
        
        out.println("    public " + fieldType + " " + methodName + "() {");
        out.println("        return " + field.name + ";");
        out.println("    }");
        out.println();
    }
    
    private String formatNames(String[] names) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            sb.append("\"").append(names[i]).append("\"");
            if (i < names.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
    
    private String mapJavaTypeToSimple(String type) {
        if (type.equals("java.lang.String")) return "String";
        if (type.equals("int")) return "int";
        if (type.equals("double")) return "double";
        if (type.equals("boolean")) return "boolean";
        if (type.contains("List<String>")) return "List<String>";
        return type;
    }
    
    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    private static class FieldInfo {
        final String name;
        final String type;
        final CliOption annotation;
        
        FieldInfo(String name, String type, CliOption annotation) {
            this.name = name;
            this.type = type;
            this.annotation = annotation;
        }
    }
}