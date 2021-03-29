package engine.caesar.exception.parse;

public class InvalidFlagException extends Exception {

    public InvalidFlagException( String argument ) {

        super( String.format( "Flag '%s' is invalid.", argument ) );

    }

}
