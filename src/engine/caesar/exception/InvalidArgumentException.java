package engine.caesar.exception;

public class InvalidArgumentException extends Exception {

    public InvalidArgumentException( String message ) {

        super( String.format( "Invalid argument: %s.", message ) );

    }

}
