package engine.caesar.arg;

import java.util.List;

public class Flag extends AnnotatedArgument {

    public Flag( boolean essential, String identifier, List< AnnotatedArgument > alternatives, List< AnnotatedArgument > dependencies ) {

        super( essential, identifier, alternatives, dependencies );

    }

}
