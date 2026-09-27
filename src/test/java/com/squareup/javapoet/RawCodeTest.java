package com.squareup.javapoet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RawCodeTest {

    @Test
    public void rawCodePlacementAndCompilation() throws IOException {
        TypeSpec type = TypeSpec.classBuilder("Example")
                .addJavadoc("Example documentation.\n")
                .addAnnotation(Deprecated.class)
                .addField(java.util.Date.class, "date")
                .addType(TypeSpec.classBuilder("Nested").build())
                .addRawMembers("public String x() {\n    return \"$L $S $T\";\n}")
                .addRawMembers("public List<String> values() { return Collections.emptyList(); }\n")
                .build();
        JavaFile file = JavaFile.builder("example", type)
                .skipJavaLangImports(true)
                .indent("    ")
                .addRawImports("import java.util.*;")
                .addRawImports("import java.util.concurrent.*;\n")
                .build();
        String source = file.toString();
        assertTrue(source.contains("import java.util.Date;\nimport java.util.*;\n"
                + "import java.util.concurrent.*;\n"));
        assertTrue(source.indexOf("import java.util.*;") < source.indexOf("/**"));
        assertTrue(source.indexOf("class Nested") < source.indexOf("public String x()"));
        assertTrue(source.contains("    public String x() {\n        return \"$L $S $T\";\n    }\n"));
        assertTrue(source.endsWith("    public List<String> values() { return Collections.emptyList(); }\n\n}\n"));
        assertEquals(source, file.toBuilder().build().toString());
        assertEquals(type.toString(), type.toBuilder().build().toString());
        assertCompiles(file);
    }

    @Test
    public void rawImportsWithoutGeneratedImports() throws IOException {
        JavaFile file = JavaFile.builder("", TypeSpec.classBuilder("Example")
                .addRawMembers("List<String> values;").build())
                .addRawImports("import java.util.*;").build();
        assertTrue(file.toString().startsWith("import java.util.*;\n\n"));
        assertCompiles(file);
    }

    @Test
    public void emptyAdditionsPreserveOutput() {
        TypeSpec type = TypeSpec.classBuilder("Example").build();
        assertEquals(type.toString(), type.toBuilder().addRawMembers("").build().toString());
        JavaFile file = JavaFile.builder("example", type).build();
        assertEquals(file.toString(), file.toBuilder().addRawImports("").build().toString());
    }

    @Test
    public void enumRawMembersAndConstantBodies() throws IOException {
        TypeSpec type = TypeSpec.enumBuilder("Example")
                .addEnumConstant("VALUE", TypeSpec.anonymousClassBuilder("")
                        .addRawMembers("public String x() { return \"$VALUE\"; }").build())
                .addRawMembers("public abstract String x();")
                .build();
        assertTrue(type.toString().contains("};\n"));
        assertCompiles(JavaFile.builder("example", type).build());
    }

    @Test
    public void anonymousClassRawMembers() throws IOException {
        TypeSpec anonymous = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(Runnable.class)
                .addRawMembers("public void run() {}")
                .build();
        TypeSpec type = TypeSpec.classBuilder("Example")
                .addField(FieldSpec.builder(Runnable.class, "task", Modifier.FINAL)
                        .initializer("$L", anonymous).build()).build();
        assertCompiles(JavaFile.builder("example", type).build());
    }

    private static void assertCompiles(final JavaFile file) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path output = Files.createTempDirectory("javapoet-raw-code-");
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(null, null, null)) {
            assertTrue(compiler.getTask(null, manager, null,
                    Arrays.asList("-proc:none", "-d", output.toString()), null,
                    Collections.singletonList(file.toJavaFileObject())).call());
        } finally {
            try (java.util.stream.Stream<Path> paths = Files.walk(output)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toArray(Path[]::new)) {
                    Files.delete(path);
                }
            }
        }
    }
}
