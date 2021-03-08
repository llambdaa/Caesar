package engine.caesar.arg;

import java.util.function.Function;

public class Scheme {

    public static final Scheme PASS_ALL = Scheme.from( value -> true );
    public static final Scheme INTEGER  = Scheme.from( value -> value.matches( "[+-]?\\d+" ) );
    public static final Scheme URI      = Scheme.from( value -> value.matches( "[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]" ) );

    public static Scheme from( Function< String, Boolean > function ) {

        return new Scheme( function );

    }

    private Function< String, Boolean > validator;

    public Scheme( Function< String, Boolean > validator ) {

        this.validator = validator;

    }

    public boolean applies( String subject ) {

        return this.validator.apply( subject );

    }

}
