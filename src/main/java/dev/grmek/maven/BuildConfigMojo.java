package dev.grmek.maven;

import com.squareup.javapoet.*;
import org.apache.commons.text.CaseUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;

/**
 * <code>build-config-plugin</code> is a Maven plugin for generating <code>.java</code> source files based on project properties.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class BuildConfigMojo extends AbstractMojo {

    private static final List<String> DEFAULT_INCLUDES = Arrays.asList(
            String.class.getName(),
            Integer.class.getName(),
            Long.class.getName(),
            Short.class.getName(),
            Byte.class.getName(),
            Float.class.getName(),
            Double.class.getName(),
            Boolean.class.getName(),
            Character.class.getName(),
            CharSequence.class.getName(),
            Object.class.getName()
    );

    /**
     * Gives access to the Maven project information.
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * Defines class fields.
     */
    @Parameter(required = true)
    private List<FieldConfiguration> fields = new ArrayList<>();

    /**
     * Class name.</br>
     * Default value is "BuildConfig".
     */
    @Parameter(defaultValue = "BuildConfig")
    private String className;

    /**
     * Class will be final is this parameter is set to "true"</br>
     * Default value is "true".
     */
    @Parameter(defaultValue = "true")
    private boolean finalClass;

    /**
     * Include project artifactId as last segment in package name for generated class.</br>
     * Default value is "false".
     */
    @Parameter(defaultValue = "false")
    private boolean includeArtifactIdInPackageName;

    /**
     * If you need to include some class within your project you must include it in this list.</br>
     * Following includes are available without any input.
     * <ul>
     *     <li>java.lang.String</li>
     *     <li>java.lang.Integer</li>
     *     <li>java.lang.Long</li>
     *     <li>java.lang.Short</li>
     *     <li>java.lang.Byte</li>
     *     <li>java.lang.Float</li>
     *     <li>java.lang.Double</li>
     *     <li>java.lang.Boolean</li>
     *     <li>java.lang.Character</li>
     *     <li>java.lang.CharSequence</li>
     *     <li>java.lang.Object</li>
     * </ul>
     */
    @Parameter
    private Set<String> includes = new TreeSet<>();

    /**
     * The output directory of the generated class.
     */
    @Parameter(defaultValue = "${project.build.directory}${file.separator}generated-sources")
    private File outputDirectory;

    /**
     * Generated class package name.</br>
     * Default value is project groupId.
     */
    @Parameter(defaultValue = "${project.groupId}", required = true)
    private String packageName;

    /**
     * List of enums to generate
     */
    @Parameter
    private List<EnumConfiguration> enums = new ArrayList<>();

    /**
     * @return package name for generated class
     */
    private String getPackageName() {
        return this.includeArtifactIdInPackageName ?
                String.join(".", this.packageName, getArtifactId()) :
                this.packageName;
    }

    /**
     * @return lower camel case of project artifact id
     */
    private String getArtifactId() {
        return CaseUtils.toCamelCase(this.project.getArtifactId(), false, '-', '_', '.');
    }

    /**
     * @return list of possible includes combining default includes and user specified list
     */
    private List<TypeName> getIncludeTypeNames() {
        return Stream.concat(
                DEFAULT_INCLUDES.stream(),
                includes.stream()
        ).distinct().map(ClassName::bestGuess).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Create output directory and add it to the project compile source
     * @throws MojoExecutionException when output directory is not created
     */
    private void prepareOutDir() throws MojoExecutionException {
        if (!this.outputDirectory.exists()) {
            if (!this.outputDirectory.mkdirs()) {
                if (!this.outputDirectory.exists()) {
                    throw new MojoExecutionException(String.format(Locale.US, "Error creating out folder %s", this.outputDirectory.getAbsolutePath()));
                }
            }
        }
        this.project.addCompileSourceRoot(this.outputDirectory.getAbsolutePath());
    }

    /**
     * Build class specification
     * @return JavaPoet class specification
     */
    private TypeSpec buildConfig() {
        Modifier[] modifiers = this.finalClass ?
                new Modifier[] {Modifier.PUBLIC, Modifier.FINAL} :
                new Modifier[] {Modifier.PUBLIC};

        TypeSpec.Builder builder = TypeSpec.classBuilder(this.className)
                .addModifiers(modifiers);

        final List<TypeName> localIncludes = getIncludeTypeNames();
        if (!this.enums.isEmpty()) {
            getLog().info("Building enums:");
            this.enums.forEach(it -> builder.addType(buildEnum(it, localIncludes)));
        }

        getLog().info("Building fields:");
        this.fields.forEach(it -> builder.addField(buildField(it, localIncludes)));

        return builder.build();
    }

    /**
     * Build enum specification
     * @param enumConfiguration configuration enum definition
     * @param localIncludes list of local typeName includes
     * @return JavaPoet enum specification
     */
    private TypeSpec buildEnum(EnumConfiguration enumConfiguration, List<TypeName> localIncludes) {
        getLog().info(String.format(Locale.US, "  - adding enum %s %s", enumConfiguration.getName(), enumConfiguration.getConstants()));
        validateEnumConfiguration(enumConfiguration);

        TypeSpec.Builder builder = TypeSpec.enumBuilder(enumConfiguration.getName())
                .addModifiers(enumConfiguration.getModifiers());

        enumConfiguration.getConstants().forEach(builder::addEnumConstant);

        TypeSpec result = builder.build();
        //add typename to includes
        localIncludes.add(ClassName.bestGuess(enumConfiguration.getName()));

        return result;
    }

    /**
     * Validate enum configuration
     * @param enumConfiguration enum configuration
     */
    private void validateEnumConfiguration(EnumConfiguration enumConfiguration) {
        if (enumConfiguration.getName() == null) {
            throw new RuntimeException("Parameter 'name' is required in enum definition");
        }
        else if (enumConfiguration.getConstants().isEmpty()) {
            throw new RuntimeException("Parameter 'constants' is required in enum definition");
        }
    }

    /**
     * Build field specification based on configuration field definition
     * @param fieldConfiguration configuration field definition
     * @param localIncludes list of includes
     * @return JavaPoet field specification
     */
    private FieldSpec buildField(final FieldConfiguration fieldConfiguration, final List<TypeName> localIncludes) {
        getLog().info(String.format(Locale.US, "  - adding field %s %s", fieldConfiguration.getType(), fieldConfiguration.getName()));
        validateFieldConfiguration(fieldConfiguration);
        TypeName typeName = getTypeName(fieldConfiguration.getType(), localIncludes);
        FieldSpec.Builder spec = FieldSpec.builder(typeName, fieldConfiguration.getName())
                .addModifiers(fieldConfiguration.getModifiers());

        EnumConfiguration enumType = getEnumType(typeName);
        if (enumType != null) {
            if (!enumType.getConstants().contains(fieldConfiguration.getValue())) {
                throw new RuntimeException(String.format(Locale.US, "Value '%s' of field '%s' is not one of enum type '%s'. Available values %s", fieldConfiguration.getValue(), fieldConfiguration.getName(), enumType.getName(), enumType.getConstants()));
            }
            spec.initializer("$T.$L", typeName, fieldConfiguration.getValue());
        }
        else {
            spec.initializer(fieldConfiguration.getValue());
        }

        fieldConfiguration.getComment().ifPresent(
                spec::addJavadoc
        );
        return spec.build();
    }

    /**
     * Validate field configuration
     * @param fieldConfiguration field configuration
     */
    private void validateFieldConfiguration(FieldConfiguration fieldConfiguration) {
        if (fieldConfiguration.getName() == null) {
            throw new RuntimeException("Parameter 'name' is required in field definition");
        }
        else if (fieldConfiguration.getType() == null) {
            throw new RuntimeException("Parameter 'type' is required in field definition");
        }
        else if (fieldConfiguration.getValue() == null) {
            throw new RuntimeException("Parameter 'value' is required in field definition");
        }
    }

    /**
     * Fetch enum definition for given typeName.
     * @param typeName typeName to find
     * @return Enum definition for typeName or <code>null</code> if given typeName is not enum.
     */
    private EnumConfiguration getEnumType(final TypeName typeName) {
        if (typeName instanceof ClassName) {
            final ClassName className = (ClassName) typeName;

            if (className.packageName().isEmpty()) {
                return this.enums.stream().filter(it -> it.getName().equals(className.simpleName())).findFirst().orElse(null);
            }
        }
        return null;
    }

    /**
     * Build field type based on configuration field type
     * @param type field type from configuration (without package name)
     * @param localIncludes list of includes
     * @return JavaPoet field type
     */
    private TypeName getTypeName(final String type, final List<TypeName> localIncludes) {
        return getPrimitiveTypeName(type).orElseGet(() -> localIncludes.stream()
                .filter(it -> it instanceof ClassName)
                .map(it -> (ClassName) it)
                .filter(it -> it.simpleName().equals(type))
                .findFirst().orElseThrow(
            () -> new RuntimeException(String.format(Locale.US, "Can't find class '%s'. Please include it in 'includes' section. Current available includes: %s", type, localIncludes.stream().map(it -> ((ClassName) it).canonicalName())))
        ));
    }

    /**
     * Try to get type for primitive fields
     * @param type field type from configuration (without package name)
     * @return JavaPoet field definition if given type is primitive
     */
    private Optional<TypeName> getPrimitiveTypeName(final String type) {
        switch (type) {
            case "int":
                return Optional.of(TypeName.INT);

            case "long":
                return Optional.of(TypeName.LONG);

            case "short":
                return Optional.of(TypeName.SHORT);

            case "byte":
                return Optional.of(TypeName.BYTE);

            case "float":
                return Optional.of(TypeName.FLOAT);

            case "double":
                return Optional.of(TypeName.DOUBLE);

            case "boolean":
                return Optional.of(TypeName.BOOLEAN);

            case "char":
                return Optional.of(TypeName.CHAR);

            default:
                return Optional.empty();
        }
    }

    @Override
    public void execute() throws MojoExecutionException {
        try {
            String packageName = getPackageName();
            getLog().info(String.format(Locale.US, "Creating class %s.%s", packageName, this.className));

            TypeSpec buildConfigSpec = buildConfig();

            JavaFile javaFile = JavaFile.builder(packageName, buildConfigSpec)
                    .build();

            prepareOutDir();

            getLog().info(String.format(Locale.US, "Output directory %s", this.outputDirectory.getAbsolutePath()));
            javaFile.writeTo(this.outputDirectory);
        }
        catch (RuntimeException e) {
            throw new MojoExecutionException(e.getMessage(), e.getCause());
        }
        catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}
