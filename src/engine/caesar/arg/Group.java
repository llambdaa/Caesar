package engine.caesar.arg;

import engine.caesar.exception.InvalidArgumentException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Group extends AnnotatedArgument {

    private List< Scheme > schemes;

    public Group( boolean essential, String identifier, Scheme scheme, Collection< AnnotatedArgument > alternatives, Collection< AnnotatedArgument > dependencies ) {

        this( essential, identifier, Collections.singletonList( scheme ), alternatives, dependencies );

    }

    public Group( boolean essential, String identifier, List< Scheme > schemes, Collection< AnnotatedArgument > alternatives, Collection< AnnotatedArgument > dependencies ) {

        super( essential, identifier, alternatives, dependencies );

        try {

            if ( schemes != null ) {

                this.schemes = schemes;

            } else throw new InvalidArgumentException( "There are no scheme definitions" );

        } catch ( InvalidArgumentException exception ) {

            exception.printStackTrace();

        }

    }

    public List< Scheme > getSchemes() {

        return this.schemes;

    }

}
