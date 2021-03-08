package engine.caesar.exception;

public class ArgumentIndexClashException extends Exception {

    public ArgumentIndexClashException(int index ) {

        super( String.format( "Multiple fields have been assigned the index '%s'.", index ) );

    }

}
