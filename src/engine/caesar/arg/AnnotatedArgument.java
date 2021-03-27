package engine.caesar.arg;

import engine.caesar.exception.InvalidArgumentException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AnnotatedArgument extends Argument {

    private String identifier;
    private List< AnnotatedArgument > alternatives;
    private List< AnnotatedArgument > dependencies;

    public AnnotatedArgument( boolean essential, String identifier, Collection< AnnotatedArgument > alternatives, Collection< AnnotatedArgument > dependencies ) {

        super( essential );

        if ( identifier.matches( Caesar.GROUP_EQUALS_REGEX ) ) {

            InvalidArgumentException.print( identifier, "Identifier is not allowed to contain equals sign." );

        } else if ( identifier.matches( Caesar.GROUP_COLON_REGEX ) ) {

            InvalidArgumentException.print( identifier, "Identifier is not allowed to contain colon." );

        } else {

            this.identifier = identifier;

        }

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
