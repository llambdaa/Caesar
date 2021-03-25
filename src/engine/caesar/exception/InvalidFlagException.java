package engine.caesar.exception;

public class InvalidFlagException extends Exception {

    public InvalidFlagException( String argument ) {

        super( String.format( "Flag '%s' is invalid.", argument ) );

    }

}
