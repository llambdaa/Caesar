package engine.caesar.arg;

import engine.caesar.exception.engine.InvalidArgumentException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AnnotatedArgument extends Argument {

    private static final String ERROR_IDENTIFIER_UNDEFINED       = "Identifier is undefined.";
    private static final String ERROR_IDENTIFIER_HAS_EQUALS_SIGN = "Identifier is not allowed to contain equals sign.";
    private static final String ERROR_IDENTIFIER_HAS_COLON       = "Identifier is not allowed to contain colon.";

    private String identifier;
    private List< AnnotatedArgument > alternatives;
    private List< AnnotatedArgument > dependencies;

    public AnnotatedArgument( boolean essential, String identifier, Collection< AnnotatedArgument > alternatives, Collection< AnnotatedArgument > dependencies )
    throws InvalidArgumentException {

        super( essential );

        if ( identifier == null || identifier.isEmpty() || identifier.isBlank() ) {

            throw new InvalidArgumentException( identifier, ERROR_IDENTIFIER_UNDEFINED );

        } else if ( identifier.matches( Caesar.GROUP_EQUALS_REGEX ) ) {

            throw new InvalidArgumentException( identifier, ERROR_IDENTIFIER_HAS_EQUALS_SIGN );

        } else if ( identifier.matches( Caesar.GROUP_COLON_REGEX ) ) {

            throw new InvalidArgumentException( identifier, ERROR_IDENTIFIER_HAS_COLON );

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

    /** This function allows making aliases of this object
     *  with a new identifier.
     *  This sometimes eleviates the need to write a lot of
     *  boilerplate code in order o create similar arguments.
     */
    public abstract AnnotatedArgument alias( String identifier )
    throws InvalidArgumentException;

}
