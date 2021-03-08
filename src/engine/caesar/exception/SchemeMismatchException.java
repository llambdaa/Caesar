package engine.caesar.exception;

public class SchemeMismatchException extends Exception {

    public SchemeMismatchException( String value ) {

        super( String.format( "Value '%s' doesn't match scheme.", value ) );

    }

}
