package engine.caesar.arg;

import java.util.*;

public class Group extends AnnotatedArgument {

    private List< Scheme > schemes;
    private Format format;

    public Group( String identifier, Scheme scheme, Collection< AnnotatedArgument > alternatives, Collection< AnnotatedArgument > dependencies ) {

        this( false, identifier, Format.WHITESPACE, ( scheme != null ? Arrays.asList( scheme ) : null ), alternatives, dependencies );

    }

    public Group( String identifier, List< Scheme > schemes, Collection< AnnotatedArgument > alternatives, Collection< AnnotatedArgument > dependencies ) {

        this( false, identifier, Format.WHITESPACE, schemes, alternatives, dependencies );

    }

    public Group( boolean essential, String identifier, Format format, Scheme scheme, Collection< AnnotatedArgument > alternatives, Collection< AnnotatedArgument > dependencies ) {

        this( essential, identifier, format, ( scheme != null ? Arrays.asList( scheme ) : null ), alternatives, dependencies );

    }

    public Group( boolean essential, String identifier, Format format, List< Scheme > schemes, Collection< AnnotatedArgument > alternatives, Collection< AnnotatedArgument > dependencies ) {

        super( essential, identifier, alternatives, dependencies );
        this.schemes = ( schemes != null ? schemes : new ArrayList<>() );
        this.format = format;

    }

    public List< Scheme > getSchemes() {

        return this.schemes;

    }

    public Format getFormat() {

        return this.format;

    }

    /** This method provides functionality for simple
     *  alias creation by cloning another groups configuration
     *  and assigning it an aliasing identifier.
     */
    public Group alias( String identifier ) {

        List< AnnotatedArgument > alternatives = this.getAlternatives();
        alternatives.add( this );

        return new Group( this.essential, identifier, this.format, this.schemes, alternatives, this.getDependencies() );

    }

}
