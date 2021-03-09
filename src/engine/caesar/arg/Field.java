package engine.caesar.arg;

public class Field extends Argument {

    private Scheme scheme;
    private int index;

    public Field( Scheme scheme, int index ) {

        super( true );
        this.scheme = scheme;
        this.index  = Math.max( 0, index );

    }

    public Scheme getScheme() {

        return this.scheme;

    }

    public int getIndex() {

        return this.index;

    }

}
