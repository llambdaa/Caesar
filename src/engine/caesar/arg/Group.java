package engine.caesar.arg;

import engine.caesar.exception.engine.InvalidArgumentException;

import java.util.*;
import java.util.stream.Collectors;

public class Group extends AnnotatedArgument {

    private static final String ERROR_SCHEME_PREFIX     = "scheme";
    private static final String ERROR_SCHEME_UNDEFINED  = "Groups need defined schemes - scheme is null.";
    private static final String ERROR_SCHEMES_PREFIX    = "schemes";
    private static final String ERROR_SCHEMES_UNDEFINED = "Groups need defined schemes - collection either empty or null.";

    private boolean variableLength;
    private List< Scheme > schemes;
    private Format format;

    public Group( String identifier, Scheme scheme, Collection< AnnotatedArgument > alternatives, Collection< AnnotatedArgument > dependencies )
    throws InvalidArgumentException {

        this( false, identifier, Format.WHITESPACE, false, scheme, alternatives, dependencies );

    }

    public Group( boolean essential, String identifier, Scheme scheme, Collection< AnnotatedArgument > alternatives, Collection< AnnotatedArgument > dependencies )
    throws InvalidArgumentException {

        this( essential, identifier, Format.WHITESPACE, false, scheme, alternatives, dependencies );

    }

    private Group( boolean essential, String identifier, Format format, boolean variableLength, Scheme scheme, Collection< AnnotatedArgument > alternatives, Collection< AnnotatedArgument > dependencies )
    throws InvalidArgumentException {

        super( essential, identifier, alternatives, dependencies );
        this.format = format;
        this.variableLength = variableLength;
        this.schemes = new ArrayList<>();

        /* If @scheme is @null, an exception is raised
         * because this implies that the group is followed
         * by no values, which is illegal.
         */
        if ( scheme != null ) {

            this.schemes.add( scheme );

        } else throw new InvalidArgumentException( ERROR_SCHEME_PREFIX, ERROR_SCHEME_UNDEFINED );

    }

    //TODO make private
    public Group( boolean essential, String identifier, Format format, List< Scheme > schemes, Collection< AnnotatedArgument > alternatives, Collection< AnnotatedArgument > dependencies )
    throws InvalidArgumentException {

        super( essential, identifier, alternatives, dependencies );
        this.format = format;
        this.variableLength = false;
        this.schemes = new ArrayList<>();

        /* Theoretically @schemes could either itself be @null
         * or its elements are @null.
         * However, this implies that the group's flag is not
         * followed by any argument value (since no scheme for
         * any exists), which is illegal.
         *
         * Hence an exception is thrown an no replacement is set.
         * This will most likely lead to a crash of the engine,
         * because not having following values is a fatal error.
         *
         * But if @schemes not only contains @null, these @null
         * elements are removed in order to not letting the engine
         * crash although there are schemes to match following values.
         */
        if ( schemes != null ) {

            List< Scheme > nonnull = schemes.stream()
                                            .filter( Objects::nonNull )
                                            .collect( Collectors.toList() );
            this.schemes.addAll( nonnull );

        }

        if ( schemes == null || this.schemes.size() == 0 ) {

            throw new InvalidArgumentException( ERROR_SCHEMES_PREFIX, ERROR_SCHEMES_UNDEFINED );

        }

    }

    public boolean hasVariableLength() {

        return this.variableLength;

    }

    public List< Scheme > getSchemes() {

        return this.schemes;

    }

    public Format getFormat() {

        return this.format;

    }

    @Override
    public Group alias( String identifier )
    throws InvalidArgumentException {

        List< AnnotatedArgument > alternatives = this.getAlternatives();
        alternatives.add( this );

        return new Group( this.essential, identifier, this.format, this.schemes, alternatives, this.getDependencies() );

    }

}
