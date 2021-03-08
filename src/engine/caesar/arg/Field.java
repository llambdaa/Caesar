package engine.caesar.arg;

public class Field extends Argument {

    private Scheme scheme;

    public Field( Scheme scheme, int index ) {

        super( true, Math.max( 0, index ) );
        this.scheme = scheme;

    }

    public Scheme getScheme() {

        return this.scheme;

    }

}
