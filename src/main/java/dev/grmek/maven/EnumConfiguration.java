package dev.grmek.maven;

import org.apache.maven.plugins.annotations.Parameter;

import javax.lang.model.element.Modifier;
import java.util.Set;
import java.util.TreeSet;

public class EnumConfiguration {

    /**
     * Enum name
     */
    @Parameter(required = true)
    private String name;

    /**
     * Enum modifiers</br>
     * Default value: "PUBLIC"
     * @see javax.lang.model.element.Modifier
     */
    @Parameter
    private Modifier[] modifiers = new Modifier[] { Modifier.PUBLIC };

    /**
     * List of enum constants
     */
    @Parameter(required = true)
    private Set<String> constants = new TreeSet<>();


    public Modifier[] getModifiers() {
        return modifiers;
    }

    @SuppressWarnings("unused")
    public void setModifiers(Modifier[] modifiers) {
        this.modifiers = modifiers;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getConstants() {
        return constants;
    }

    @SuppressWarnings("unused")
    public void setConstants(Set<String> constants) {
        this.constants = constants;
    }
}
