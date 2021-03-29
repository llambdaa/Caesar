package engine.caesar.arg;

import engine.caesar.exception.engine.InvalidArgumentException;

import java.util.Collection;
import java.util.List;

public class Flag extends AnnotatedArgument {

    public Flag( String identifier, Collection< AnnotatedArgument > alternatives, Collection< AnnotatedArgument > dependencies )
    throws InvalidArgumentException {

        this( false, identifier, alternatives, dependencies );

    }

    public Flag( boolean essential, String identifier, Collection< AnnotatedArgument > alternatives, Collection< AnnotatedArgument > dependencies )
    throws InvalidArgumentException {

        super( essential, identifier, alternatives, dependencies );

    }

    @Override
    public Flag alias( String identifier )
    throws InvalidArgumentException {

        List< AnnotatedArgument > alternatives = this.getAlternatives();
        alternatives.add( this );

        return new Flag( this.essential, identifier, alternatives, this.getDependencies() );

    }

}
