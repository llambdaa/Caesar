package engine.caesar.arg;

import java.util.Collection;

public class Flag extends AnnotatedArgument {

    public Flag( boolean essential, String identifier, Collection< AnnotatedArgument > alternatives, Collection< AnnotatedArgument > dependencies ) {

        super( essential, identifier, alternatives, dependencies );

    }

}
