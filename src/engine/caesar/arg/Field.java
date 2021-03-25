package engine.caesar.arg;

public class Field extends Argument {

    private Scheme scheme;
    private int index;

    public Field( int index, Scheme scheme ) {

        super( true );
        this.index  = Math.max( 0, index );
        this.scheme = scheme;

    }

    public int getIndex() {

        return this.index;

    }

    public Scheme getScheme() {

        return this.scheme;

    }

}
