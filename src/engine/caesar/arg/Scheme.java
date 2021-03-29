package engine.caesar.arg;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Scheme {

    public static final Scheme PASS_ALL = Scheme.as( value -> true );
    public static final Scheme INTEGER  = Scheme.as( value -> value.matches( "[+-]?\\d+" ) );
    public static final Scheme URI      = Scheme.as( value -> value.matches( "[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]" ) );

    public static List< Scheme > times( Scheme scheme, int times ) {

        List< Scheme > result = new ArrayList<>();
        for ( int i = 0; i < Math.max( 1, times ); i++ ) {

            result.add( scheme );

        }

        return result;

    }

    public static Scheme as( Function< String, Boolean > function ) {

        return new Scheme( function );

    }

    public static Scheme asInt( Function< Integer, Boolean > function ) {

        return new Scheme( value -> {

            try {

                return function.apply( Integer.parseInt( value ) );

            } catch ( NumberFormatException exception ) {

                exception.printStackTrace();

            }

            return false;

        } );

    }

    public static Scheme asIntRange( int min, int max ) {

        return Scheme.asIntRange( min, max, false );

    }

    public static Scheme asIntRange( int min, int max, boolean inclusive ) {

        return Scheme.asInt( value -> value >= min && value < max + ( inclusive ? 1 : 0 ) );

    }

    public static Scheme asFloat( Function< Float, Boolean > function ) {

        return new Scheme( value -> {

            try {

                return function.apply( Float.parseFloat( value ) );

            } catch ( NumberFormatException exception ) {

                exception.printStackTrace();

            }

            return false;

        } );

    }

    public static Scheme asFloatRange( int min, int max ) {

        return Scheme.asFloatRange( min, max, false );

    }

    public static Scheme asFloatRange( float min, float max, boolean inclusive ) {

        return Scheme.asFloat( value -> value >= min && value < max + ( inclusive ? 1F : 0F ) );

    }

    ///////////////////////////////////////
    //          Class Definition         //
    ///////////////////////////////////////
    private Function< String, Boolean > validator;

    private Scheme( Function< String, Boolean > validator ) {

        this.validator = validator;

    }

    public boolean applies( String subject ) {

        return this.validator.apply( subject );

    }

}
