package com.squareup.javapoet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RawMembersTest {

    @Test
    void emitsRawMembersAfterFieldsAndBeforeMethods() {
        TypeSpec typeSpec = TypeSpec.classBuilder("Example")
                .addField(String.class, "name")
                .addMethod(MethodSpec.methodBuilder("later").build())
                .addRawMembers("void raw() {}")
                .build();

        String source = typeSpec.toString();
        assertTrue(source.indexOf("String name;") < source.indexOf("void raw()"));
        assertTrue(source.indexOf("void raw()") < source.indexOf("void later()"));
        assertEquals(source, typeSpec.toBuilder().build().toString());
    }

    @Test
    void emitsRawMethodsAfterGeneratedMethods() {
        TypeSpec typeSpec = TypeSpec.classBuilder("Example")
                .addField(String.class, "name")
                .addRawMembers("String rawField;")
                .addMethod(MethodSpec.methodBuilder("generated").build())
                .addRawMethods("void extended() {}")
                .build();

        String source = typeSpec.toString();
        assertTrue(source.indexOf("String rawField;") < source.indexOf("void generated()"));
        assertTrue(source.indexOf("void generated()") < source.indexOf("void extended()"));
        assertEquals(source, typeSpec.toBuilder().build().toString());
    }

    @Test
    void retainsRawMembersInOtherwiseEmptyEnum() {
        String source = TypeSpec.enumBuilder("Example")
                .addEnumConstant("VALUE")
                .addRawMembers("void raw() {}")
                .build().toString();

        assertTrue(source.contains("VALUE;\n"));
        assertTrue(source.contains("void raw() {}"));
    }
}
