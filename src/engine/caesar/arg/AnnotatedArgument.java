package engine.caesar.arg;

import java.util.List;

public abstract class AnnotatedArgument extends Argument {

    private String identifier;
    private List< AnnotatedArgument > alternatives;
    private List< AnnotatedArgument > dependencies;

    public AnnotatedArgument( boolean essential, String identifier, List< AnnotatedArgument > alternatives, List< AnnotatedArgument > dependencies ) {

        super( essential, -1 );
        this.identifier = identifier;
        this.alternatives = alternatives;
        this.dependencies = dependencies;

    }

    public String getIdentifier() {

        return this.identifier;

    }

    public List< AnnotatedArgument > getAlternatives() {

        return this.alternatives;

    }

    public List< AnnotatedArgument > getDependencies() {

        return this.dependencies;

    }

}
