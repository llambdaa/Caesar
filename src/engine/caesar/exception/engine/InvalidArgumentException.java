package engine.caesar.exception.engine;

public class InvalidArgumentException extends Exception {

    public InvalidArgumentException( String argument, String message ) {

        super( String.format( "Invalid argument: %s / %s", argument, message ) );

    }

    public static void print( String argument, String message ) {

        InvalidArgumentException exception = new InvalidArgumentException( argument, message );
        exception.printStackTrace();

    }

}
