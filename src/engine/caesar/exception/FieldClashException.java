package engine.caesar.exception;

public class FieldClashException extends Exception {

    public FieldClashException(int index ) {

        super( String.format( "Multiple fields have been assigned the index '%s'.", index ) );

    }

}
