package com.github.yulichang.processor;

import com.baomidou.mybatisplus.annotation.TableField;
import com.github.yulichang.annotation.Table;
import com.github.yulichang.apt.BaseColumn;
import com.github.yulichang.apt.Column;
import com.github.yulichang.processor.matedata.FieldInfo;
import com.github.yulichang.processor.matedata.TableInfo;
import com.github.yulichang.processor.utils.StringUtil;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author yulichang
 * @since 1.5.0
 */
public class EntityProcessor extends AbstractProcessor {

    private Elements elementUtils;
    private Types typeUtils;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        this.messager = processingEnv.getMessager();
    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            TypeElement table = annotations.stream().filter(i -> i.toString().equals(Table.class.getName())).findFirst().orElse(null);
            if (table != null) {
                Set<? extends Element> tables = roundEnv.getElementsAnnotatedWith(table);
                tables.stream().filter(f -> f instanceof TypeElement)
                        .map(f -> (TypeElement) f).map(this::createColumn)
                        .collect(Collectors.groupingBy(TableInfo::getClassPackage))
                        .forEach(this::createTables);
            }
        }
        return false;
    }


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> supportedAnnotationTypes = new HashSet<>();
        supportedAnnotationTypes.add(Table.class.getCanonicalName());
        return supportedAnnotationTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 生成Column类
     */
    private TableInfo createColumn(TypeElement element) {
        TableInfo tableInfo = new TableInfo(element.getAnnotation(Table.class), element.toString(), element.getSimpleName().toString());
        tableInfo.setClassPackage(elementUtils.getPackageOf(element).getQualifiedName().toString());
        tableInfo.setClassComment(elementUtils.getDocComment(element));

        Set<FieldInfo> fieldInfos = new HashSet<>();

        TypeElement currElement = element;
        do {
            fieldInfos.addAll(currElement.getEnclosedElements().stream().filter(e ->
                            // 过滤static字段
                            e.getKind() == ElementKind.FIELD && !e.getModifiers().contains(Modifier.STATIC))
                    .filter(e -> {
                        // 过滤 exist = false 的字段
                        TableField tableField = e.getAnnotation(TableField.class);
                        return tableField == null || tableField.exist();
                    })
                    .map(e -> new FieldInfo(e.toString(), elementUtils.getDocComment(e))).collect(Collectors.toList()));
            currElement = (TypeElement) typeUtils.asElement(currElement.getSuperclass());
        } while (currElement != null);

        tableInfo.setFields(fieldInfos);

        StringBuilderHelper content = new StringBuilderHelper()
                .addPackage(tableInfo.getTagPackage())
                .newLine()
                .addImport(BaseColumn.class.getName())
                .addImport(Column.class.getName())
                .addImport(tableInfo.getClassName())
                .newLine()
                .addClass(tableInfo.getClassComment(), tableInfo.getTagClassName(),
                        BaseColumn.class.getSimpleName() + "<" + tableInfo.getSimpleClassName() + ">",
                        c -> c
                                .addDefaultField(tableInfo.getSimpleClassName())
                                .addConstructor(tableInfo)
                                .addFields(tableInfo)
                                .addMethod(tableInfo)
                );
        writerFile(tableInfo.getTagPackage() + "." + tableInfo.getTagClassName(), content.getContent());
        return tableInfo;
    }

    /**
     * 生成Tables
     */
    private void createTables(String tagPackage, List<TableInfo> tableInfos) {
        StringBuilderHelper content = new StringBuilderHelper();
        // package
        content.addPackage(tagPackage + ".tables");
        content.newLine();
        // import
        tableInfos.forEach(tableInfo -> content.addImport(tableInfo.getTagPackage() + "." + tableInfo.getTagClassName()));
        content.newLine();
        // class
        String tables = "Tables";
        content.addClass(" tables", tables, "", c -> c
                .newLine()
                // 私有构造
                .addPrivateConstructor(tables)
                // 添加table字段
                .addTablesFields(tableInfos));

        writerFile(tagPackage + ".tables.Tables", content.getContent());
    }

    private void writerFile(String fullClassName, String content) {
        try {
            JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(fullClassName);

            Writer writer = sourceFile.openWriter();
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            this.messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class StringBuilderHelper {
        private final StringBuilder sb = new StringBuilder();

        public StringBuilderHelper addPackage(String packageName) {
            sb.append("package ").append(packageName).append(";\n");
            return this;
        }

        public StringBuilderHelper addImport(String importName) {
            sb.append("import ").append(importName).append(";\n");
            return this;
        }

        public StringBuilderHelper addClass(String comment, String tagClassName, String impl,
                                            Consumer<StringBuilderHelper> consumer) {
            addComment("", comment);
            sb.append("public class ").append(tagClassName);
            if (StringUtil.isNotEmpty(impl)) {
                sb.append(" implements ").append(impl);
            }
            sb.append(" {\n");
            consumer.accept(this);
            sb.append("}\n");
            return this;
        }

        public StringBuilderHelper addConstructor(TableInfo tableInfo) {
            // 无参构造
            sb.append(String.format("\tpublic %s() {\n\t}\n", tableInfo.getTagClassName()));
            newLine();
            //有参构造
            sb.append("\tpublic ").append(tableInfo.getTagClassName()).append("(String alias) {\n" +
                    "\t\tthis._alias_q2Gv$ = alias;\n" +
                    "\t}\n");
            newLine();
            return this;
        }

        /**
         * 私有构造
         */
        public StringBuilderHelper addPrivateConstructor(String tagClassName) {
            sb.append(String.format("\tprivate %s() {\n\t}\n", tagClassName));
            newLine();
            return this;
        }

        public StringBuilderHelper addFields(TableInfo tableInfo) {
            tableInfo.getFields().forEach(fieldInfo -> {
                addComment("\t", fieldInfo.getComment());
                sb.append(String.format("\tpublic final Column %s = new Column(this, %s.class, \"%s\", () -> this._alias_q2Gv$);\n",
                        fieldInfo.getProperty(), tableInfo.getSimpleClassName(), fieldInfo.getProperty()));
                newLine();
            });
            return this;
        }

        public StringBuilderHelper addTablesFields(List<TableInfo> tableInfos) {
            tableInfos.forEach(tableInfo -> {
                addComment("\t", tableInfo.getClassComment());
                sb.append(String.format("\tpublic static final %s %s = new %s();\n",
                        tableInfo.getTagClassName(),
                        String.format(tableInfo.getTableAnnotation().tablesName(), tableInfo.getSimpleClassName()),
                        tableInfo.getTagClassName()));
                newLine();
            });
            return this;
        }

        public StringBuilderHelper addDefaultField(String simpleClassName) {
            newLine();
            sb.append("\tprivate static final Class<").append(simpleClassName).append("> _class_e76G$ = ").append(simpleClassName).append(".class;\n");
            newLine();
            sb.append("\tprivate String _alias_q2Gv$;\n");
            newLine();
            return this;
        }

        public StringBuilderHelper addMethod(TableInfo tableInfo) {
            sb.append("\t@Override\n" +
                            "\tpublic Class<").append(tableInfo.getSimpleClassName()).append("> getColumnClass() {\n")
                    .append("\t\treturn this._class_e76G$;\n")
                    .append("\t}\n");
            newLine();
            sb.append("\t@Override\n" +
                    "\tpublic String getAlias() {\n" +
                    "\t\treturn this._alias_q2Gv$;\n" +
                    "\t}\n");
            newLine();
            return this;
        }

        private StringBuilderHelper addComment(String prefix, String comment) {
            if (StringUtil.isNotEmpty(comment)) {
                sb.append(prefix).append("/**\n");
                sb.append(Arrays.stream(comment.split("\n")).map(f -> prefix + " *" + f).collect(Collectors.joining("\n")));
                sb.append("\n").append(prefix).append(" */");
                newLine();
            }
            return this;
        }

        public StringBuilderHelper newLine() {
            sb.append("\n");
            return this;
        }

        public String getContent() {
            return sb.toString();
        }

    }
}