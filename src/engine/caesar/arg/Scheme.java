package engine.caesar.arg;

import java.util.function.Function;

public class Scheme <A> {

    public static final Scheme INTEGER = Scheme.from( Integer::valueOf );
    public static final Scheme URI     = Scheme.from( value -> {

        return value.matches( "[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]" );

    } );

    public static <A> Scheme from( Function< String, A > function ) {

        return new Scheme( function );

    }

    private Function< String, A > validator;

    public Scheme( Function< String, A > validator ) {

        this.validator = validator;

    }

}
