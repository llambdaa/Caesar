package engine.caesar.arg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AnnotatedArgument extends Argument {

    private String identifier;
    private List< AnnotatedArgument > alternatives;
    private List< AnnotatedArgument > dependencies;

    public AnnotatedArgument( boolean essential, String identifier, Collection< AnnotatedArgument > alternatives, Collection< AnnotatedArgument > dependencies ) {

        super( essential );
        this.identifier = identifier;
        this.alternatives = alternatives == null ? new ArrayList<>() : new ArrayList<>( alternatives );
        this.dependencies = dependencies == null ? new ArrayList<>() : new ArrayList<>( dependencies );

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
