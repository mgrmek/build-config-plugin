package dev.grmek.maven;

import org.apache.maven.plugins.annotations.Parameter;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.Optional;

public class FieldConfiguration {
    /**
     * Field type. If type is none of primitives (and also boxed versions)
     * you need to include class in <code>includes</code> property
     */
    @Parameter(required = true)
    private String type;

    /**
     * Field name
     */
    @Parameter(required = true)
    private String name;

    /**
     * Field value
     */
    @Parameter(required = true)
    private String value;

    /**
     * Field modifier.</br>
     * Default value: "PUBLIC, STATIC, FINAL"
     * @see javax.lang.model.element.Modifier
     */
    private Modifier[] modifiers;

    /**
     * Field javadoc comment
     */
    private String comment;

    public FieldConfiguration() {
        modifiers = new Modifier[] {
                Modifier.PUBLIC,
                Modifier.STATIC,
                Modifier.FINAL
        };
    }

    public String getType() {
        return type;
    }

    @SuppressWarnings("unused")
    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    @SuppressWarnings("unused")
    public void setValue(String value) {
        this.value = value;
    }

    public Modifier[] getModifiers() {
        return modifiers;
    }

    @SuppressWarnings("unused")
    public void setModifiers(Modifier[] modifiers) {
        this.modifiers = modifiers;
    }

    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    @SuppressWarnings("unused")
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "FieldDefinition{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", modifiers=" + Arrays.toString(modifiers) +
                ", has comment='" + !comment.isEmpty() + '\'' +
                '}';
    }
}
