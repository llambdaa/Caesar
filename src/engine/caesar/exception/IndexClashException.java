package engine.caesar.exception;

public class IndexClashException extends Exception {

    public IndexClashException( int index ) {

        super( String.format( "Multiple fields have been assigned the index '%s'.", index ) );

    }

}
