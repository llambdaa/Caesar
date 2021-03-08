package engine.caesar.arg;

import java.util.List;

public class Group extends AnnotatedArgument {

    private Scheme scheme;

    public Group( boolean essential, String identifier, Scheme scheme, List< AnnotatedArgument > alternatives, List< AnnotatedArgument > dependencies ) {

        super( essential, identifier, alternatives, dependencies );
        this.scheme = scheme == null ? Scheme.PASS_ALL : scheme;

    }

    public Scheme getScheme() {

        return this.scheme;

    }

}
