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

        try {

            if ( !identifier.contains( "=" ) ) {

                this.identifier = identifier;

            } else throw new InvalidArgumentException( identifier, "Identifier is not allowed to contain equals sign." );

        } catch ( InvalidArgumentException exception ) {

            exception.printStackTrace();

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
